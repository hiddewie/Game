package nl.hiddewieringa.game.tictactoe

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.runBlocking
import nl.hiddewieringa.game.core.GameManager
import nl.hiddewieringa.game.core.TwoPlayers
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TicTacToeTest {

    @Test
    fun noPlayerWon() {
        runBlocking {
            val gameManager = GameManager()

            val gameResult = gameManager.play(
                { TicTacToe() },
                {
                    TwoPlayers(
                        FreeSpaceTicTacToePlayer(),
                        FreeSpaceTicTacToePlayer(),
                    )
                },
                TicTacToeGameParameters
            )

            assertEquals(NoPlayerWon, gameResult)
        }
    }

    @Test
    fun illegalMove() {
        runBlocking {
            val gameManager = GameManager()

            val gameResult = gameManager.play(
                { TicTacToe() },
                {
                    TwoPlayers(
                        StubbedPlayer(listOf(Location(0, 0), Location(0, 0))),
                        FreeSpaceTicTacToePlayer(),
                    )
                },
                TicTacToeGameParameters
            )

            assertEquals(Player2Won, gameResult)
        }
    }

    @Test
    fun player1Wins() {
        runBlocking {
            val gameManager = GameManager()

            val gameResult = gameManager.play(
                { TicTacToe() },
                {
                    TwoPlayers(
                        StubbedPlayer(listOf(Location(0, 0), Location(0, 1), Location(0, 2))),
                        StubbedPlayer(listOf(Location(1, 0), Location(1, 1), Location(1, 2))),
                    )
                },
                TicTacToeGameParameters
            )

            assertEquals(Player1Won, gameResult)
        }
    }

    class StubbedPlayer(private val locations: List<Location>) : TicTacToePlayer {

        private var index: Int = 0

        private fun play(): Location =
            locations[index++]

        override fun initialize(parameters: TicTacToeGameParameters, eventBus: ReceiveChannel<TicTacToeEvent>): suspend ProducerScope<TicTacToePlayerActions>.() -> Unit =
            {
                eventBus.consumeEach { event ->
                    when (event) {
                        is PlaceMark -> send(PlaceMarkLocation(play()))
                        else -> {
                        }
                    }
                }
            }

        override fun gameEnded(result: TicTacToeGameResult) {
        }
    }
}