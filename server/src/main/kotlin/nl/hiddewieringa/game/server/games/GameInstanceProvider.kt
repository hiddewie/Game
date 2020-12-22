package nl.hiddewieringa.game.server.games

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.launch
import nl.hiddewieringa.game.core.*
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.Executors

class PlayerSlots<A : PlayerActions, E : Event, S : GameState>(
    val sendChannel: SendChannel<A>,
    val receiveChannel: ReceiveChannel<Pair<E, S>>,
)

class GameInstance<A : PlayerActions, E : Event, S : GameState>(
    val id: UUID,
    val gameId: UUID,
    val open: Boolean,
    val playerSlots: Map<UUID, PlayerSlots<A, E, S>>,
    val stateProvider: () -> S,
//    val actionClass: Class<A>,
)

class WebsocketPlayer<M : GameParameters, A : PlayerActions, E : Event, R : GameResult, S : GameState> : Player<M, E, S, A, R> {

    val eventChannel = Channel<Pair<E, S>>(capacity = 0)
    val actionChannel = Channel<A>(capacity = 0)

    override fun initialize(parameters: M, initialState: S, eventBus: ReceiveChannel<Pair<E, S>>): suspend ProducerScope<A>.() -> Unit =
        {
            launch {
                actionChannel.consumeEach {
                    send(it)
                }
            }
            launch {
                eventBus.consumeEach { (event, state) ->
                    eventChannel.send(event to state)
                }
            }
        }

    override fun gameEnded(result: R) {
        // TODO close channels!
//        actionChannel.close()
//        eventChannel.close()
    }
}

@Component
class GameInstanceProvider(
    private val gameManager: GameManager,
) {

    private val instances: MutableMap<UUID, GameInstance<*, *, *>> = mutableMapOf()
    private val threadPoolDispatcher = Executors.newWorkStealingPool().asCoroutineDispatcher()

    fun <M : GameParameters, P : Player<M, E, S, A, R>, A : PlayerActions, E : Event, R : GameResult, PID : PlayerId, PC : PlayerConfiguration<PID, P>, S : GameState>
    start(gameDetails: GameDetails<M, P, A, E, R, PID, PC, S>): UUID {

        val instanceId = UUID.randomUUID()
        val parameters = gameDetails.defaultParameters

        // We need a reference to the players for exposing the channels
        val playerConfiguration = gameDetails.playerConfigurationFactory({ WebsocketPlayer<M, A, E, R, S>() as P })
        // We need a reference to the game to expose the latest game state
        val game = gameDetails.gameFactory(parameters)

        // Use the global scope to launch a
        //   WITHOUT waiting for the result of the game
        //   using the thread pool for running games.
        GlobalScope.launch(threadPoolDispatcher) {
            // TODO replace with actual gamemo

            val gameResult = gameManager.play(
                { game },
                { playerConfiguration },
                parameters,
            )

            // TODO store game result
            println("Game result of $instanceId: $gameResult")
        }

        val playerSlots = playerConfiguration
            .map {
                UUID.randomUUID() to PlayerSlots(
                    (it as WebsocketPlayer<M, A, E, R, S>).actionChannel,
                    (it as WebsocketPlayer<M, A, E, R, S>).eventChannel,
                )
            }
            .toMap()

        instances[instanceId] = GameInstance(
            instanceId,
            gameDetails.id,
            true,
            playerSlots,
            game::state,
//            gameDetails.actionClass
        )

        return instanceId
    }

    fun gameInstance(instanceId: UUID): GameInstance<*, *, *>? =
        instances[instanceId]

    fun openGames(gameId: UUID): List<GameInstance<*, *, *>> =
        instances.values
            .filter { it.gameId == gameId }
            .filter(GameInstance<*, *, *>::open)
            .toList()
}
