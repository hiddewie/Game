package nl.hiddewieringa.game.tictactoe

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.runBlocking
import nl.hiddewieringa.game.core.GameManager
import nl.hiddewieringa.game.core.Player
import nl.hiddewieringa.game.core.TwoPlayerId
import nl.hiddewieringa.game.core.TwoPlayers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

class TicTacToeTest {

    @Test
    fun noPlayerWon() {
        runBlocking {
            val gameManager = GameManager()

            val gameResult = gameManager.play(
                this,
                { TicTacToePlay() },
                {
                    TwoPlayers(
                        StubbedPlayer(listOf(Location(0, 0), Location(1, 2), Location(1, 1), Location(2, 0), Location(2, 1))),
                        StubbedPlayer(listOf(Location(0, 2), Location(0, 1), Location(2, 2), Location(1, 0))),
                    )
                },
                TicTacToeGameParameters,
                { this },
            ).await()

            assertTrue(gameResult is NoPlayerWon)
        }
    }

    @Test
    fun illegalMove() {
        runBlocking {
            val gameManager = GameManager()

            val gameResult = gameManager.play(
                this,
                { TicTacToePlay() },
                {
                    TwoPlayers(
                        StubbedPlayer(listOf(Location(0, 0), Location(0, 0))),
                        FreeSpaceTicTacToePlayer(),
                    )
                },
                TicTacToeGameParameters,
                { this },
            ).await()

            assertTrue(gameResult is PlayerWon)
            assertEquals(TwoPlayerId.PLAYER2, (gameResult as PlayerWon).player)
        }
    }

    @Test
    fun player1Wins() {
        runBlocking {
            val gameManager = GameManager()

            val gameResult = gameManager.play(
                this,
                { TicTacToePlay() },
                {
                    TwoPlayers(
                        StubbedPlayer(listOf(Location(0, 0), Location(0, 1), Location(0, 2))),
                        StubbedPlayer(listOf(Location(1, 0), Location(1, 1), Location(1, 2))),
                    )
                },
                TicTacToeGameParameters,
                { this },
            ).await()

            assertTrue(gameResult is PlayerWon)
            assertEquals(TwoPlayerId.PLAYER1, (gameResult as PlayerWon).player)
        }
    }

    class StubbedPlayer(locations: List<Location>) : Player<TicTacToeGameParameters, TicTacToeEvent, TicTacToePlayerActions, TwoPlayerId, TicTacToeState> {

        private val locationsQueue = ArrayDeque(locations)

        private fun play(): Location =
            locationsQueue.pop()

        override fun play(parameters: TicTacToeGameParameters, playerId: TwoPlayerId, initialState: TicTacToeState, events: ReceiveChannel<Pair<TicTacToeEvent, TicTacToeState>>): suspend ProducerScope<TicTacToePlayerActions>.() -> Unit =
            {
                when (initialState) {
                    is TicTacToePlay -> {
                        if (initialState.playerToPlay == playerId) {
                            send(PlaceMarkLocation(play()))
                        }
                    }
                    is PlayerWon -> {
                    }
                    is NoPlayerWon -> {
                    }
                }

                events.consumeEach { (_, state) ->
                    when (state) {
                        is TicTacToePlay -> {
                            if (state.playerToPlay == playerId) {
                                send(PlaceMarkLocation(play()))
                            }
                        }
                        is PlayerWon -> {
                        }
                        is NoPlayerWon -> {
                        }
                    }
                }
            }
    }
}
