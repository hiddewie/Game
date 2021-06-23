package nl.hiddewieringa.game.server.games

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.KSerializer
import mu.KotlinLogging
import nl.hiddewieringa.game.core.*
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

class PlayerSlot<A : PlayerActions, E : Event, S : Any, PID : PlayerId>(
    val playerId: PID,
    val sendChannel: SendChannel<A>,
    val receiveChannel: SharedFlow<Pair<E, S>>,
) {

    var referenceCount = AtomicInteger()

    fun increaseReference() {
        referenceCount.incrementAndGet()
        logger.info("player slot $playerId ${referenceCount.get()}")
    }

    fun decreaseReference() {
        referenceCount.decrementAndGet()
        logger.info("player slot $playerId ${referenceCount.get()}")
    }
}

// TODO store start timestamp
class GameInstance<A : PlayerActions, E : Event, S : Any, PID : PlayerId>(
    val id: UUID,
    val gameSlug: String,
    val playerSlots: Map<UUID, PlayerSlot<A, E, S, PID>>,
    val stateProvider: suspend (PID) -> S,
    val actionSerializer: KSerializer<A>,
    val eventSerializer: KSerializer<E>,
    val stateSerializer: KSerializer<S>,
    val playerIdSerializer: KSerializer<PID>,
) {
    val open: Boolean
        get() = playerSlots.values.any { it.referenceCount.get() == 0 }

    suspend fun playerState(playerSlotId: UUID): S =
        stateProvider(playerSlots.getValue(playerSlotId).playerId)
}

class WebsocketPlayer<M : GameParameters, A : PlayerActions, E : Event, S : Any> : Player<M, E, A, PlayerId, S> {

    /**
     * Hot flow that can have multiple consumers. No replay: new consumers have to fetch the latest state manually, and will not receive past events.
     */
    val eventChannel = MutableSharedFlow<Pair<E, S>>(replay = 0)

    /**
     * Actions must be delivered exactly once, suspending until they are delivered.
     */
    val actionChannel = Channel<A>(capacity = 0)

    override fun play(parameters: M, playerId: PlayerId, initialState: S, events: ReceiveChannel<Pair<E, S>>): suspend ProducerScope<A>.() -> Unit =
        {
            launch {
                actionChannel.consumeEach {
                    send(it)
                }
            }
            launch {
                events.consumeEach { (event, state) ->
                    eventChannel.emit(event to state)
                }
            }
        }
}

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
    start(gameDetails: GameDetails<M, P, A, E, PID, PC, S, PS>): UUID {
        val coroutineScope = CoroutineScope(threadPoolDispatcher)

        val instanceId = UUID.randomUUID()
        val parameters = gameDetails.defaultParameters

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

        val playerSlots = playerConfiguration.associate { playerId ->
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
        val job = coroutineScope.launch {
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
}
