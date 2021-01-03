package nl.hiddewieringa.game.server.games

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.hiddewieringa.game.core.*
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

class PlayerSlots<A : PlayerActions, E : Event, S>(
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

class GameInstance<A : PlayerActions, E : Event, S : Any>(
    val id: UUID,
    val gameSlug: String,
    val playerSlots: Map<UUID, PlayerSlots<A, E, S>>,
    val stateProvider: () -> S,
//    val actionClass: Class<A>,
) {
    val open: Boolean
        get() = playerSlots.values.any { it.referenceCount.get() == 0 }
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
) {

    private val instances: MutableMap<UUID, GameInstance<*, *, *>> = mutableMapOf()
    private val threadPoolDispatcher = Executors.newWorkStealingPool().asCoroutineDispatcher()

    fun <M : GameParameters, P : Player<M, E, A, PID, PS>, A : PlayerActions, E : Event, PID : PlayerId, PC : PlayerConfiguration<PID, P>, S : GameState<S>, PS : Any>
    start(gameDetails: GameDetails<M, P, A, E, PID, PC, S, PS>): UUID {

        val instanceId = UUID.randomUUID()
        val parameters = gameDetails.defaultParameters

        // We need a reference to the players for exposing the channels
        val playerConfiguration = gameDetails.playerConfigurationFactory({ WebsocketPlayer<M, A, E, PS>() as P })

        // Use the global scope to launch a
        //   WITHOUT waiting for the result of the game
        //   using the thread pool for running games.
        var stateSupplier: (() -> S)? = null
        GlobalScope.launch(threadPoolDispatcher) {

            logger.info("Launching game ${gameDetails.slug} instance $instanceId and parameters $parameters")

            val gameJob = gameManager.play(
                gameDetails.gameFactory,
                { playerConfiguration },
                parameters,
                gameDetails.playerState,
            )
            stateSupplier = gameJob.stateSupplier

            val gameResult = gameJob.await()

            // TODO store game result
            println("Game result of $instanceId: $gameResult")
        }

        val playerSlots = playerConfiguration
            .map {
                val player = playerConfiguration.player(it) as WebsocketPlayer<M, A, E, PS>
                UUID.randomUUID() to PlayerSlots(
                    player.actionChannel,
                    player.eventChannel,
                )
            }
            .toMap()

        instances[instanceId] = GameInstance(
            instanceId,
            gameDetails.slug,
            playerSlots,
            { gameDetails.playerState(stateSupplier!!()) },
        )

        logger.info("Created instance $instanceId with ${playerSlots.size} playerSlots")

        return instanceId
    }

    fun gameInstance(instanceId: UUID): GameInstance<*, *, *>? =
        instances[instanceId]

    fun openGames(gameSlug: String): List<GameInstance<*, *, *>> =
        instances.values
            .filter { it.gameSlug == gameSlug }
            .filter(GameInstance<*, *, *>::open)
            .toList()
}
