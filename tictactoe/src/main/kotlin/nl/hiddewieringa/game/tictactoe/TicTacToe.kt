package nl.hiddewieringa.game.tictactoe

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import nl.hiddewieringa.game.core.*

sealed class TicTacToeState : GameState<TicTacToeState>

data class TicTacToePlay(
    val board: Array<Array<GameMark?>>,
    val playerToPlay: TwoPlayerId,
) : TicTacToeState(), IntermediateGameState<TwoPlayerId, TicTacToePlayerActions, TicTacToeEvent, TicTacToeState> {

    constructor() : this(
        arrayOf(
            arrayOf(null, null, null),
            arrayOf(null, null, null),
            arrayOf(null, null, null)
        ),
        TwoPlayerId.PLAYER1
    )

    override fun applyEvent(event: TicTacToeEvent): TicTacToeState =
        when (event) {
            IllegalMove -> when (playerToPlay) {
                TwoPlayerId.PLAYER1 -> PlayerWon(board, TwoPlayerId.PLAYER2)
                TwoPlayerId.PLAYER2 -> PlayerWon(board, TwoPlayerId.PLAYER1)
            }

            is PlayerPlacedMark -> {
                val newBoard = board.map { it.copyOf() }.toTypedArray()
                newBoard[event.location.x][event.location.y] = event.mark

                TicTacToePlay(
                    newBoard,
                    nextPlayer(event.player)
                )
            }

            is GameEnded ->
                if (event.playerWon == null) {
                    NoPlayerWon(board)
                } else {
                    PlayerWon(board, event.playerWon)
                }
        }

    override fun processPlayerAction(playerId: TwoPlayerId, action: TicTacToePlayerActions): TicTacToeEvent =
        when {
            playerId == playerToPlay && action is PlaceMarkLocation -> {
                val played = action.location
                println("Player $playerId played $played")
                if (board[played.x][played.y] != null) {
                    IllegalMove
                } else {
                    val mark = markForPlayer(playerId)
                    PlayerPlacedMark(playerId, mark, played)
                }
            }

            else ->
                IllegalMove
        }

    override val gameDecisions: List<GameDecision<TicTacToeEvent>> =
        listOf(
            GameDecision(noPlayerWon(board)) {
                GameEnded(null)
            },
            playerWon(board)
                .let { wonMark ->
                    GameDecision(wonMark != null) {
                        GameEnded(playerForMark(wonMark!!))
                    }
                },
        )

    private fun noPlayerWon(board: Array<Array<GameMark?>>): Boolean =
        board.all { it.all { value -> value != null } }

    private fun playerWon(board: Array<Array<GameMark?>>): GameMark? =
        when {
            playerWon(board, Circle) -> Circle
            playerWon(board, Cross) -> Cross
            else -> null
        }

    private fun playerWon(board: Array<Array<GameMark?>>, mark: GameMark): Boolean =
        (0 until 3).any { j -> (0 until 3).all { i -> board[j][i] == mark } } ||
                (0 until 3).any { j -> (0 until 3).all { i -> board[i][j] == mark } } ||
                (0 until 3).all { i -> board[i][i] == mark } ||
                (0 until 3).all { i -> board[i][2 - i] == mark }

    private fun markForPlayer(playerId: TwoPlayerId): GameMark =
        when (playerId) {
            TwoPlayerId.PLAYER1 -> Cross
            TwoPlayerId.PLAYER2 -> Circle
        }

    private fun playerForMark(mark: GameMark): TwoPlayerId =
        when (mark) {
            Cross -> TwoPlayerId.PLAYER1
            Circle -> TwoPlayerId.PLAYER2
        }

    private fun nextPlayer(playerId: TwoPlayerId) =
        when (playerId) {
            TwoPlayerId.PLAYER1 -> TwoPlayerId.PLAYER2
            TwoPlayerId.PLAYER2 -> TwoPlayerId.PLAYER1
        }
}

sealed class GameMark
object Cross : GameMark()
object Circle : GameMark()
data class Location(val x: Int, val y: Int)

sealed class TicTacToeEvent : Event
data class PlayerPlacedMark(val player: TwoPlayerId, val mark: GameMark, val location: Location) : TicTacToeEvent()
object IllegalMove : TicTacToeEvent()
data class GameEnded(val playerWon: TwoPlayerId?) : TicTacToeEvent()

object TicTacToeGameParameters : GameParameters

sealed class TicTacToePlayerActions : PlayerActions
data class PlaceMarkLocation(val location: Location) : TicTacToePlayerActions()

sealed class TicTacToeGameResult : TicTacToeState()
data class PlayerWon(val board: Array<Array<GameMark?>>, val player: TwoPlayerId) : TicTacToeGameResult()
data class NoPlayerWon(val board: Array<Array<GameMark?>>) : TicTacToeGameResult()

class FreeSpaceTicTacToePlayer : Player<TicTacToeGameParameters, TicTacToeEvent, TicTacToePlayerActions, TwoPlayerId, TicTacToeState> {

    override fun play(parameters: TicTacToeGameParameters, playerId: TwoPlayerId, initialState: TicTacToeState, events: ReceiveChannel<Pair<TicTacToeEvent, TicTacToeState>>): suspend ProducerScope<TicTacToePlayerActions>.() -> Unit =
        {
            when (initialState) {
                is TicTacToePlay -> {
                    if (initialState.playerToPlay == playerId) {
                        send(PlaceMarkLocation(play(initialState.board)))
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
                            send(PlaceMarkLocation(play(state.board)))
                        }
                    }
                    is PlayerWon -> {
                    }
                    is NoPlayerWon -> {
                    }
                }
            }
        }

    private fun play(board: Array<Array<GameMark?>>): Location {
        board.forEachIndexed { i, row ->
            row.forEachIndexed { j, item ->
                if (item === null) {
                    return Location(i, j)
                }
            }
        }
        throw IllegalStateException()
    }
}
