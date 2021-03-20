package nl.hiddewieringa.game.server.games

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
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
    val receiveChannel: ReceiveChannel<Pair<E, S>>,
) {

    var referenceCount = AtomicInteger()

    fun increaseReference() {
        referenceCount.incrementAndGet()
        logger.info("player slot ${referenceCount.get()}")
    }

    fun decreaseReference() {
        referenceCount.decrementAndGet()
        logger.info("player slot ${referenceCount.get()}")
    }
}

// TODO store start timestamp
class GameInstance<A : PlayerActions, E : Event, S : Any, PID : PlayerId>(
    val id: UUID,
    val gameSlug: String,
    val playerSlots: Map<UUID, PlayerSlot<A, E, S, PID>>,
    val stateProvider: (PID) -> S,
    val actionSerializer: KSerializer<A>,
    val eventSerializer: KSerializer<E>,
    val stateSerializer: KSerializer<S>,
    val playerIdSerializer: KSerializer<PID>,
) {
    val open: Boolean
        get() = playerSlots.values.any { it.referenceCount.get() == 0 }

    fun playerState(playerSlotId: UUID): S =
        stateProvider(playerSlots.getValue(playerSlotId).playerId)
}

class WebsocketPlayer<M : GameParameters, A : PlayerActions, E : Event, S : Any> : Player<M, E, A, PlayerId, S> {

    val eventChannel = Channel<Pair<E, S>>(capacity = 0)
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
                    eventChannel.send(event to state)
                }
            }
        }
}

@Component
class GameInstanceProvider(
    private val gameManager: GameManager,
    private val coroutineScope: CoroutineScope,
) {

    private val instances: MutableMap<UUID, GameInstance<*, *, *, *>> = ConcurrentHashMap()
    private val threadPoolDispatcher = Executors.newWorkStealingPool().asCoroutineDispatcher()

    suspend fun <M : GameParameters, P : Player<M, E, A, PID, PS>, A : PlayerActions, E : Event, PID : PlayerId, PC : PlayerConfiguration<PID, P>, S : GameState<S>, PS : Any>
    start(gameDetails: GameDetails<M, P, A, E, PID, PC, S, PS>): UUID {

        val instanceId = UUID.randomUUID()
        val parameters = gameDetails.defaultParameters

        // We need a reference to the players for exposing the channels
        val playerConfiguration = gameDetails.playerConfigurationFactory({ WebsocketPlayer<M, A, E, PS>() as P })

        // TODO simplify: less factories, more concrete arguments
        val gameJob = gameManager.play(
            coroutineScope,
            gameDetails.gameFactory,
            { playerConfiguration },
            parameters,
            gameDetails.playerState,
        )

        // Use the global scope to launch a
        //   WITHOUT waiting for the result of the game
        //   using the thread pool for running games.
        coroutineScope.async(threadPoolDispatcher) {
            val gameResult = gameJob.await()

            // TODO store game result
            println("Game result of $instanceId: $gameResult.")
            instances.remove(instanceId)
            logger.info("Removed game instance $instanceId (active games ${instances.size}).")
        }

        val playerSlots = playerConfiguration.associate { playerId ->
            val player = playerConfiguration.player(playerId) as WebsocketPlayer<M, A, E, PS>
            UUID.randomUUID() to PlayerSlot(
                playerId,
                player.actionChannel,
                player.eventChannel,
            )
        }

        instances[instanceId] = GameInstance(
            instanceId,
            gameDetails.slug,
            playerSlots,
            { playerId: PID -> gameDetails.playerState.invoke(gameJob.stateSupplier(), playerId) },
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
