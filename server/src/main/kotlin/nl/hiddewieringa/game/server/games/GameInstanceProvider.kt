package nl.hiddewieringa.game.server.games

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import mu.KLogging
import nl.hiddewieringa.game.core.*
import nl.hiddewieringa.game.server.data.GameInstanceRepository
import nl.hiddewieringa.game.server.data.PlayerSlot
import nl.hiddewieringa.game.server.data.PlayerSlotRepository
import nl.hiddewieringa.game.server.event.GameEvents
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors

sealed class GameStateRequest<S : GameState<S>>
data class UpdateState<S : GameState<S>>(val state: S) : GameStateRequest<S>()
data class GetState<S : GameState<S>>(val response: CompletableDeferred<S>) : GameStateRequest<S>()

data class OpenGame(val id: UUID, val playerSlotIds: List<OpenGamePlayerSlot>)
data class OpenGamePlayerSlot(val id: UUID, val name: String)

@Component
class GameInstanceProvider(
    private val gameEvents: GameEvents,
    private val gameManager: GameManager,
    private val gameProvider: GameProvider,
    private val gameInstanceRepository: GameInstanceRepository,
    private val playerSlotRepository: PlayerSlotRepository,
) {

    private val serializer = Json.Default

    private val threadPoolDispatcher = Executors.newWorkStealingPool().asCoroutineDispatcher()

    suspend fun <
            M : GameParameters,
            P : Player<M, E, A, PID, PS>,
            A : PlayerActions,
            E : Event,
            PID : PlayerId,
            PC : PlayerConfiguration<PID, P>,
            S : GameState<S>,
            PS : Any
            >
            start(gameDetails: GameDetails<M, P, A, E, PID, PC, S, PS>, parameters: M): UUID {
        val coroutineScope = CoroutineScope(threadPoolDispatcher)

        val initialGameState = gameDetails.gameFactory(parameters)

        val instanceId = UUID.randomUUID()

        // We need a reference to the players for exposing the channels
        val playerConfiguration = gameDetails.playerConfigurationFactory { WebsocketPlayer<M, A, E, PS>() as P }

        val stateUpdateChannel = Channel<S>()
        val playerStateActor = coroutineScope.actor<GameStateRequest<S>> {
            var state: S? = null
            for (msg in channel) {
                when (msg) {
                    is UpdateState ->
                        state = msg.state
                    is GetState ->
                        if (state != null) {
                            msg.response.complete(state)
                        } else {
                            msg.response.completeExceptionally(IllegalStateException("Actor state not initialized yet"))
                        }
                }
            }
        }
        val stateJob = coroutineScope.launch {
            logger.info("State update channel start consume")
            stateUpdateChannel.consumeEach { state ->
                playerStateActor.send(UpdateState(state))
            }
            logger.info("State update channel end consume")
        }

//        val playerSlots: Map<UUID, PlayerSlot<A, E, PS, PID>> = playerConfiguration
//            // TODO add boolean for interaction
//            .filter { playerId -> playerConfiguration.player(playerId) is WebsocketPlayer<*, *, *, *> }
//            .associate { playerId ->
//                val player = playerConfiguration.player(playerId) as WebsocketPlayer<M, A, E, PS>
//                UUID.randomUUID() to PlayerSlot(
//                    playerId,
//                    player.actionChannel,
//                    player.eventChannel,
//                )
//            }
        val playerSlots: Map<UUID, PID> = playerConfiguration.associateBy { UUID.randomUUID() }

        // Use the global scope to launch a
        //   WITHOUT waiting for the result of the game
        //   using the thread pool for running games.
        coroutineScope.launch {
            // TODO simplify: less factories, more concrete arguments
            val gameResult = gameManager.play(
                gameDetails.gameFactory,
                { playerConfiguration },
                parameters,
                gameDetails.playerState,
                stateUpdateChannel,
            )

            // TODO store game result
            logger.info("Game result of $instanceId: $gameResult.")
            gameInstanceRepository.deleteById(instanceId)
            playerStateActor.close()
            stateJob.cancel()
            playerConfiguration.allPlayers.forEach {
                (playerConfiguration.player(it) as WebsocketPlayer<M, A, E, PS>).actionChannel.close()
//                (playerConfiguration.player(it) as WebsocketPlayer<M, A, E, PS>).eventChannel.close()
            }
            val activeGames = gameInstanceRepository.count()
            logger.info("Removed game instance $instanceId (active games $activeGames).")
        }

        gameInstanceRepository.save(
            nl.hiddewieringa.game.server.data.GameInstance(
                instanceId,
                gameDetails.slug,
                serializeGameState(gameDetails.stateSerializer, initialGameState),
                mutableMapOf()
            ).apply {
                (this.playerSlots as MutableMap<UUID, PlayerSlot>).putAll(
                    playerSlots.mapValues { (id, playerId) ->
                        PlayerSlot(
                            id,
                            this,
                            serializer.encodeToString(gameDetails.playerIdSerializer, playerId),
                            Instant.EPOCH,
                        )
                    })
            }
            // TODO
//            { playerId: PID ->
//                val deferredResult = CompletableDeferred<S>()
//                playerStateActor.send(GetState(deferredResult))
//                val result = deferredResult.await()
//                gameDetails.playerState.invoke(result, playerId)
//            }
        )
        val activeGames = gameInstanceRepository.count()
        logger.info("Created instance $instanceId with ${playerSlots.size} playerSlots (active games $activeGames).")

        return instanceId
    }

    @Transactional
    fun gameInstance(instanceId: UUID): GameInstance<*, *>? =
        gameInstanceRepository.findById(instanceId)
            .map(::hydrateGameInstance)
            .orElse(null)

    // TODO dont hydrate state
    @Transactional
    fun openGames(gameSlug: String): List<OpenGame> =
        gameInstanceRepository.openGames(gameSlug, Instant.now())
            .map(::hydrateOpenGame)

    private fun hydrateGameInstance(gameInstance: nl.hiddewieringa.game.server.data.GameInstance): GameInstance<*, *> =
        hydrateGameInstance(gameInstance, gameProvider.bySlug(gameInstance.gameSlug))

    private fun hydrateOpenGame(gameInstance: nl.hiddewieringa.game.server.data.GameInstance): OpenGame =
        OpenGame(
            gameInstance.id,
            gameInstance.playerSlots
                .filterValues { it.lockedUntil <= Instant.now() }
                .map { (key, value) -> OpenGamePlayerSlot(key, value.toString()) }
        )

    private fun <S : GameState<S>> hydrateGameInstance(gameInstance: nl.hiddewieringa.game.server.data.GameInstance, gameDetails: GameDetails<*, *, *, *, *, *, S, *>): GameInstance<*, *> =
        GameInstance(
            gameInstance.id,
            gameDetails.slug,
            deserializeGameState(gameDetails.stateSerializer, gameInstance.serializedState),
            gameInstance.playerSlots.mapValues { serializer.decodeFromString(gameDetails.playerIdSerializer, it.value.serializedPlayerId) }
        )

    @Transactional
    fun <A : PlayerActions, E : Event, PID : PlayerId, S : GameState<S>, PS : Any> applyPlayerAction(instanceId: UUID, playerId: PID, action: A) {
        gameInstanceRepository.findById(instanceId).ifPresent { gameInstance ->
            val gameDetails = gameProvider.bySlug(gameInstance.gameSlug)
            val gameState = deserializeGameState(gameDetails.stateSerializer, gameInstance.serializedState) as S

            gameManager.applyPlayerAction(gameState, playerId, action) { event: E, state: S ->

                logger.info("Persisting game state for instance $instanceId")
                gameInstance.serializedState = serializeGameState(gameDetails.stateSerializer as KSerializer<S>, state)
                gameInstanceRepository.save(gameInstance)

                // TODO check if game is finished

                gameInstance.playerSlots.values.forEach { playerSlot ->
                    val playerStateFunction = gameDetails.playerState as (S.(PID) -> PS)
                    gameEvents.publish(gameInstance.gameSlug, instanceId, playerSlot.id, event, playerStateFunction.invoke(state, playerId))
                }
            }
        }
    }

    fun <E : Event, S : Any> gameInstanceEvents(instanceId: UUID, playerSlotId: UUID): Flow<Pair<E, S>> =
        gameEvents.receiveEvents(instanceId, playerSlotId)

    @Transactional
    fun increasePlayerSlotLock(instanceId: UUID, playerSlotId: UUID) {
        val until = Instant.now().plus(PlayerSlot.PLAYER_SLOT_LOCK_DURATION)
        playerSlotRepository.lockUntil(instanceId, playerSlotId, until)
        logger.info("Increasing player slot lock for instance $instanceId and player slot $playerSlotId until $until")
    }

    @Transactional
    fun removePlayerSlotLock(instanceId: UUID, playerSlotId: UUID) {
        playerSlotRepository.removeLock(instanceId, playerSlotId)
        logger.info("Removed player slot lock for instance $instanceId and player slot $playerSlotId")
    }

    private fun <S : GameState<S>> serializeGameState(stateSerializer: KSerializer<S>, state: S): String =
        serializer.encodeToString(stateSerializer, state)

    private fun <S : GameState<S>> deserializeGameState(stateSerializer: KSerializer<S>, serializedState: String): S =
        serializer.decodeFromString(stateSerializer, serializedState)

    companion object : KLogging()
}
