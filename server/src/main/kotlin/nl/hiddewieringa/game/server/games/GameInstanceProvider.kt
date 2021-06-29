package nl.hiddewieringa.game.server.games

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import mu.KLogging
import nl.hiddewieringa.game.core.*
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

sealed class GameStateRequest<S : GameState<S>>
data class UpdateState<S : GameState<S>>(val state: S) : GameStateRequest<S>()
data class GetState<S : GameState<S>>(val response: CompletableDeferred<S>) : GameStateRequest<S>()

@Component
class GameInstanceProvider(
    private val gameManager: GameManager,
) {

    private val instances: MutableMap<UUID, GameInstance<*, *, *, *>> = ConcurrentHashMap()
    private val threadPoolDispatcher = Executors.newWorkStealingPool().asCoroutineDispatcher()

    suspend fun <
        M : GameParameters,
        P : Player<M, E, A, PID, PS>,
        A : PlayerActions,
        E : Event,
        PID : PlayerId,
        PC : PlayerConfiguration<PID, P>,
        S : GameState<S>, PS : Any
        >
    start(gameDetails: GameDetails<M, P, A, E, PID, PC, S, PS>, parameters: M): UUID {
        val coroutineScope = CoroutineScope(threadPoolDispatcher)

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

        val playerSlots: Map<UUID, PlayerSlot<A, E, PS, PID>> = playerConfiguration
            .filter { playerId -> playerConfiguration.player(playerId) is WebsocketPlayer<*, *, *, *> }
            .associate { playerId ->
                val player = playerConfiguration.player(playerId) as WebsocketPlayer<M, A, E, PS>
                UUID.randomUUID() to PlayerSlot(
                    playerId,
                    player.actionChannel,
                    player.eventChannel,
                )
            }

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
            instances.remove(instanceId)
            playerStateActor.close()
            stateJob.cancel()
            playerConfiguration.allPlayers.forEach {
                (playerConfiguration.player(it) as WebsocketPlayer<M, A, E, PS>).actionChannel.close()
//                (playerConfiguration.player(it) as WebsocketPlayer<M, A, E, PS>).eventChannel.close()
            }
            logger.info("Removed game instance $instanceId (active games ${instances.size}).")
        }

        instances[instanceId] = GameInstance(
            instanceId,
            gameDetails.slug,
            playerSlots,
            { playerId: PID ->
                val deferredResult = CompletableDeferred<S>()
                playerStateActor.send(GetState(deferredResult))
                val result = deferredResult.await()
                gameDetails.playerState.invoke(result, playerId)
            },
            gameDetails.actionSerializer,
            gameDetails.eventSerializer,
            gameDetails.stateSerializer,
            gameDetails.playerIdSerializer,
        )
        logger.info("Created instance $instanceId with ${playerSlots.size} playerSlots (active games ${instances.size}).")

        return instanceId
    }

    fun gameInstance(instanceId: UUID): GameInstance<*, *, *, *>? =
        instances[instanceId]

    fun openGames(gameSlug: String): List<GameInstance<*, *, *, *>> =
        instances.values
            .filter { it.gameSlug == gameSlug }
            .filter(GameInstance<*, *, *, *>::open)
            .toList()

    companion object : KLogging()
}
