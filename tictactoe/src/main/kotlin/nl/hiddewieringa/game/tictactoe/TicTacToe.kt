package nl.hiddewieringa.game.tictactoe

import kotlinx.coroutines.channels.*
import nl.hiddewieringa.game.core.*

typealias TicTacToeGameContext = GameContext<TicTacToePlayerActions, TicTacToeEvent, TwoPlayerId, TwoPlayers<TicTacToePlayer>, TicTacToeState>

class TicTacToeState(
    val board: Array<Array<GameMark?>>,
) : GameState {

    constructor() : this(
        arrayOf(
            arrayOf(null, null, null),
            arrayOf(null, null, null),
            arrayOf(null, null, null)
        )
    )

    fun with(event: TicTacToeEvent): TicTacToeState =
        when (event) {
            PlaceMark -> this
            is PlayerPlacedMark -> {
                val newBoard = board.map { it.copyOf() }.toTypedArray()
                newBoard[event.location.x][event.location.y] = event.mark
                TicTacToeState(newBoard)
            }
        }
}

class TicTacToe : Game<
    TicTacToeGameParameters,
    TicTacToePlayer,
    TicTacToePlayerActions,
    TicTacToeEvent,
    TicTacToeGameResult,
    TwoPlayerId,
    TwoPlayers<TicTacToePlayer>,
    TicTacToeState,
    > {

    override var state = TicTacToeState()
//        get() = TicTacToeState(board.map { it.copyOf() }.toTypedArray())

    private fun noPlayerWon(): Boolean =
        state.board.all { it.all { value -> value != null } }

    private fun playerWon(location: Location, mark: GameMark): Boolean =
        (
            state.board[location.x][0] == mark &&
                state.board[location.x][1] == mark &&
                state.board[location.x][2] == mark
            ) ||
            (
                state.board[0][location.y] == mark &&
                    state.board[1][location.y] == mark &&
                    state.board[2][location.y] == mark
                )

    override suspend fun play(context: TicTacToeGameContext): TicTacToeGameResult {

        val markForPlayer = mapOf(
            TwoPlayerId.PLAYER1 to Cross,
            TwoPlayerId.PLAYER2 to Circle
        )

        while (true) {
            context.players.allPlayers.forEach { playerId ->
                // TODO add timing checks (withTimeout)
                context.sendToPlayer(playerId, PlaceMark)
                val (receivedPlayerId, action) = context.receiveFromPlayer()
                val played = when {
                    receivedPlayerId == playerId && action is PlaceMarkLocation -> action.location
                    else -> TODO()
                }
                println("Player $playerId played $played")
                if (state.board[played.x][played.y] != null) {
                    return if (playerId == TwoPlayerId.PLAYER1) {
                        Player2Won
                    } else {
                        Player1Won
                    }
                }

                val mark = markForPlayer.getValue(playerId)
                val event = PlayerPlacedMark(playerId, mark, played)
                state = state.with(event)
                context.sendToAllPlayers(event)

                if (noPlayerWon()) {
                    return NoPlayerWon
                }

                if (playerWon(played, mark)) {
                    return if (playerId == TwoPlayerId.PLAYER1) {
                        Player1Won
                    } else {
                        Player2Won
                    }
                }
            }
        }
    }
}

sealed class GameMark
object Cross : GameMark()
object Circle : GameMark()
data class Location(val x: Int, val y: Int)

sealed class TicTacToeEvent : Event
object PlaceMark : TicTacToeEvent()
data class PlayerPlacedMark(val player: TwoPlayerId, val mark: GameMark, val location: Location) : TicTacToeEvent()

object TicTacToeGameParameters : GameParameters

sealed class TicTacToePlayerActions : PlayerActions
data class PlaceMarkLocation(val location: Location) : TicTacToePlayerActions()

sealed class TicTacToeGameResult : GameResult
object Player1Won : TicTacToeGameResult()
object Player2Won : TicTacToeGameResult()
object NoPlayerWon : TicTacToeGameResult()

interface TicTacToePlayer : Player<TicTacToeGameParameters, TicTacToeEvent, TicTacToeState, TicTacToePlayerActions, TicTacToeGameResult>

class FreeSpaceTicTacToePlayer : TicTacToePlayer {

    private lateinit var gameState: Array<Array<GameMark?>>

    override fun initialize(parameters: TicTacToeGameParameters, initialState: TicTacToeState, eventBus: ReceiveChannel<Pair<TicTacToeEvent, TicTacToeState>>): suspend ProducerScope<TicTacToePlayerActions>.() -> Unit =
        {
            eventBus.consumeEach { (event, state) ->
                gameState = state.board
                when (event) {
                    is PlayerPlacedMark -> {
                    }
                    is PlaceMark -> {
                        send(PlaceMarkLocation(play()))
                    }
                }
            }
        }

    private fun play(): Location {
        gameState.forEachIndexed { i, row ->
            row.forEachIndexed { j, item ->
                if (item === null) {
                    return Location(i, j)
                }
            }
        }
        throw IllegalStateException()
    }

    override fun gameEnded(result: TicTacToeGameResult) {
    }
}
