package nl.hiddewieringa.game.tictactoe

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nl.hiddewieringa.game.core.*

@Serializable
sealed class TicTacToeState : GameState<TicTacToeState>

@Serializable
class TicTacToePlay(
    val board: List<List<GameMark?>>,
    val playerToPlay: TwoPlayerId,
) : TicTacToeState(), IntermediateGameState<TwoPlayerId, TicTacToePlayerActions, TicTacToeEvent, TicTacToeState> {

    constructor() : this(
        listOf(
            listOf(null, null, null),
            listOf(null, null, null),
            listOf(null, null, null)
        ),
        TwoPlayerId.PLAYER1
    )

    override fun applyEvent(event: TicTacToeEvent): TicTacToeState =
        when (event) {
            is IllegalMove -> when (event.player) {
                TwoPlayerId.PLAYER1 -> PlayerWon(board, TwoPlayerId.PLAYER2)
                TwoPlayerId.PLAYER2 -> PlayerWon(board, TwoPlayerId.PLAYER1)
            }

            is PlayerPlacedMark -> {
                val newBoard = board.mapIndexed { i, gameMarks ->
                    if (i == event.location.x) {
                        gameMarks.mapIndexed { j, original ->
                            if (j == event.location.y) event.mark else original
                        }
                    } else {
                        gameMarks
                    }
                }

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
                    IllegalMove(playerId)
                } else {
                    val mark = markForPlayer(playerId)
                    PlayerPlacedMark(playerId, mark, played)
                }
            }

            else ->
                IllegalMove(playerId)
        }

    @Transient
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

    private fun noPlayerWon(board: List<List<GameMark?>>): Boolean =
        board.all { it.all { value -> value != null } }

    private fun playerWon(board: List<List<GameMark?>>): GameMark? =
        when {
            playerWon(board, Circle) -> Circle
            playerWon(board, Cross) -> Cross
            else -> null
        }

    private fun playerWon(board: List<List<GameMark?>>, mark: GameMark): Boolean =
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

@Serializable
sealed class GameMark

@Serializable
object Cross : GameMark()

@Serializable
object Circle : GameMark()

@Serializable
data class Location(val x: Int, val y: Int)

@Serializable
sealed class TicTacToeEvent : Event

@Serializable
data class PlayerPlacedMark(val player: TwoPlayerId, val mark: GameMark, val location: Location) : TicTacToeEvent()

@Serializable
data class IllegalMove(val player: TwoPlayerId) : TicTacToeEvent()

@Serializable
data class GameEnded(val playerWon: TwoPlayerId?) : TicTacToeEvent()

@Serializable
object TicTacToeGameParameters : GameParameters

@Serializable
sealed class TicTacToePlayerActions : PlayerActions

@Serializable
data class PlaceMarkLocation(val location: Location) : TicTacToePlayerActions()

@Serializable
sealed class TicTacToeGameResult : TicTacToeState()

@Serializable
class PlayerWon(val board: List<List<GameMark?>>, val player: TwoPlayerId) : TicTacToeGameResult()

@Serializable
class NoPlayerWon(val board: List<List<GameMark?>>) : TicTacToeGameResult()

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

    private fun play(board: List<List<GameMark?>>): Location {
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
