package nl.hiddewieringa.game.server.games

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.launch
import nl.hiddewieringa.game.core.GameManager
import nl.hiddewieringa.game.core.TwoPlayers
import nl.hiddewieringa.game.tictactoe.*
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.Executors

class PlayerSlots(
    val sendChannel: SendChannel<TicTacToePlayerActions>,
    val receiveChannel: ReceiveChannel<TicTacToeEvent>,
)

class GameInstance(
    val id: UUID,
    val gameId: UUID,
    val open: Boolean,
//   TODO  val game: Game
    val playerSlots: List<PlayerSlots>,
)

class WebsocketPlayer : TicTacToePlayer {

    val eventChannel = Channel<TicTacToeEvent>(capacity = 0)
    val actionChannel = Channel<TicTacToePlayerActions>(capacity = 0)

    override fun initialize(parameters: TicTacToeGameParameters, eventBus: ReceiveChannel<TicTacToeEvent>): suspend ProducerScope<TicTacToePlayerActions>.() -> Unit =
        {
            launch {
                actionChannel.consumeEach {
                    send(it)
                }
            }
            launch {
                eventBus.consumeEach { event ->
                    eventChannel.send(event)
                }
            }
        }

    override fun gameEnded(result: TicTacToeGameResult) {
    }
}

@Component
class GameInstanceProvider(
// TODO    private val gameProvider: GameProvider
) {

    private val instances: MutableMap<UUID, GameInstance> = mutableMapOf()
    private val threadPoolDispatcher = Executors.newWorkStealingPool().asCoroutineDispatcher()

    suspend fun startGame(gameId: UUID): UUID {
        val instanceId = UUID.randomUUID()

        val players: TwoPlayers<TicTacToePlayer> = TwoPlayers(
            WebsocketPlayer(),
            WebsocketPlayer()
        )

        // Use the global scope to launch a
        //   WITHOUT waiting for the result of the game
        //   using the thread pool for running games.
        val job = GlobalScope.async(threadPoolDispatcher) {
            // TODO replace with actual game

            GameManager().play(
                { TicTacToe() },
                { players },
                TicTacToeGameParameters
            )
        }

        // TODO store job, and for interaction with game by connecting later
        //   and blocking events!
        instances[instanceId] = GameInstance(
            instanceId,
            gameId,
            true,
            // TODO make generic
            listOf(
                PlayerSlots(
                    (players.player1 as WebsocketPlayer).actionChannel,
                    (players.player1 as WebsocketPlayer).eventChannel,
                ),
                PlayerSlots(
                    (players.player2 as WebsocketPlayer).actionChannel,
                    (players.player2 as WebsocketPlayer).eventChannel,
                ),
            )
        )

        return instanceId
    }

    fun gameInstance(instanceId: UUID): GameInstance? =
        instances[instanceId]

    fun openGames(gameId: UUID): List<GameInstance> =
        instances.values
            .filter { it.gameId == gameId }
            .filter(GameInstance::open)
            .toList()
}
