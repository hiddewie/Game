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
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import mu.KLogging
import nl.hiddewieringa.game.core.*
import nl.hiddewieringa.game.server.data.GameInstanceRepository
import nl.hiddewieringa.game.server.event.GameEvents
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
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
) {

    private val serializer = Json.Default

    //    private val instances: MutableMap<UUID, GameInstance<*, *, *, *>> = ConcurrentHashMap()
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
                serializePlayerSlots(gameDetails.playerIdSerializer, playerSlots),
                serializeGameState(gameDetails.stateSerializer, initialGameState),
            )
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

    fun gameInstance(instanceId: UUID): GameInstance<*, *>? =
        gameInstanceRepository.findById(instanceId)
            .map(::hydrateGameInstance)
            .orElse(null)

    // TODO dont hydrate state
    @Transactional
    fun openGames(gameSlug: String): List<OpenGame> =
        gameInstanceRepository.openGames(gameSlug)
            .map(::hydrateOpenGame)

    private fun hydrateGameInstance(gameInstance: nl.hiddewieringa.game.server.data.GameInstance): GameInstance<*, *> =
        hydrateGameInstance(gameInstance, gameProvider.bySlug(gameInstance.gameSlug))

    private fun hydrateOpenGame(gameInstance: nl.hiddewieringa.game.server.data.GameInstance): OpenGame =
        hydrateOpenGame(gameInstance, gameProvider.bySlug(gameInstance.gameSlug))

    private fun <S : GameState<S>> hydrateGameInstance(gameInstance: nl.hiddewieringa.game.server.data.GameInstance, gameDetails: GameDetails<*, *, *, *, *, *, S, *>): GameInstance<*, *> =
         GameInstance(
            gameInstance.id,
            gameDetails.slug,
            deserializeGameState(gameDetails.stateSerializer, gameInstance.serializedState),
            deserializePlayerSlots(gameDetails.playerIdSerializer, gameInstance.serializedPlayerSlots),
        )

    private fun <S : GameState<S>> hydrateOpenGame(gameInstance: nl.hiddewieringa.game.server.data.GameInstance, gameDetails: GameDetails<*, *, *, *, *, *, S, *>): OpenGame =
        OpenGame(
            gameInstance.id,
            deserializePlayerSlots(gameDetails.playerIdSerializer, gameInstance.serializedPlayerSlots)
                // TODO only open slots
//                .filterValues { it.referenceCount.get() == 0 }
                .map { (key, value) -> OpenGamePlayerSlot(key, value.toString()) }
        )

    @Transactional
    fun <A : PlayerActions, E : Event, PID : PlayerId, S : GameState<S>, PS: Any> applyPlayerAction(instanceId: UUID, playerId: PID, action: A) {
        gameInstanceRepository.findById(instanceId).ifPresent { gameInstance ->
            val gameDetails = gameProvider.bySlug(gameInstance.gameSlug)
            val gameState = deserializeGameState(gameDetails.stateSerializer, gameInstance.serializedState) as S
            val playerSlots = deserializePlayerSlots(gameDetails.playerIdSerializer, gameInstance.serializedPlayerSlots)

            gameManager.applyPlayerAction(gameState, playerId, action) { event: E, state: S ->

                logger.info("Persisting game state for instance $instanceId")
                gameInstanceRepository.save(gameInstance.copy(serializedState = serializeGameState(gameDetails.stateSerializer as KSerializer<S>, state)))

                playerSlots.forEach { (playerSlotId, playerId) ->
                    val playerStateFunction = gameDetails.playerState as (S.(PID) -> PS)
                    gameEvents.publish(gameInstance.gameSlug, instanceId, playerSlotId, event, playerStateFunction.invoke(state, playerId as PID))
                }
            }
        }
    }

    fun <E : Event, S : Any> gameInstanceEvents(instanceId: UUID, playerSlotId: UUID): Flow<Pair<E, S>> =
        gameEvents.receiveEvents(instanceId, playerSlotId)

    // TODO
    @Transactional
    fun increasePlayerSlotReference(instanceId: UUID, playerSlotId: UUID) {
        gameInstanceRepository.findById(instanceId).ifPresent { gameInstance ->
            logger.info("Increasing player slot reference for instance $instanceId and player slot $playerSlotId")
            val gameDetails = gameProvider.bySlug(gameInstance.gameSlug)
//            val playerSlots = deserializePlayerSlots(gameDetails.playerIdSerializer, gameInstance.serializedPlayerSlots)
//            // TODO count references
//            gameInstanceRepository.save(gameInstance.copy(serializedPlayerSlots = serializePlayerSlots(gameDetails.playerIdSerializer, playerSlots)))
        }
    }

    // TODO
    @Transactional
    fun decreasePlayerSlotReference(instanceId: UUID, playerSlotId: UUID) {
        gameInstanceRepository.findById(instanceId).ifPresent { gameInstance ->
            logger.info("Decreasing player slot reference for instance $instanceId and player slot $playerSlotId")
            val gameDetails = gameProvider.bySlug(gameInstance.gameSlug)
//            val playerSlots = deserializePlayerSlots(gameDetails.playerIdSerializer, gameInstance.serializedPlayerSlots)
//            // TODO count references
//            gameInstanceRepository.save(gameInstance.copy(serializedPlayerSlots = serializePlayerSlots(gameDetails.playerIdSerializer, playerSlots)))
        }
    }

    private fun <S : GameState<S>> serializeGameState(stateSerializer: KSerializer<S>, state: S): String =
        serializer.encodeToString(stateSerializer, state)

    private fun <S : GameState<S>> deserializeGameState(stateSerializer: KSerializer<S>, serializedState: String): S =
        serializer.decodeFromString(stateSerializer, serializedState)

    private fun <PID : PlayerId> serializePlayerSlots(playerIdSerializer: KSerializer<PID>, playerSlots: Map<UUID, PID>): String =
        serializer.encodeToString(playerSlotSerializer(playerIdSerializer), playerSlots.mapKeys { it.key.toString() })

    private fun <PID : PlayerId> deserializePlayerSlots(playerIdSerializer: KSerializer<PID>, serializedState: String): Map<UUID, PID> =
        serializer.decodeFromString(playerSlotSerializer(playerIdSerializer), serializedState).mapKeys { UUID.fromString(it.key) }

    private fun <PID> playerSlotSerializer(playerIdSerializer: KSerializer<PID>): KSerializer<Map<String, PID>> =
        MapSerializer(String.serializer(), playerIdSerializer)

    companion object : KLogging()
}
