package nl.hiddewieringa.game.tictactoe

import kotlinx.coroutines.channels.*
import nl.hiddewieringa.game.core.*

typealias TicTacToeGameContext = GameContext<TicTacToePlayerActions, TicTacToeEvent, TwoPlayerId, TwoPlayers<TicTacToePlayer>>

class TicTacToe : Game<
        TicTacToeGameParameters,
        TicTacToePlayer,
        TicTacToePlayerActions,
        TicTacToeEvent,
        TicTacToeGameResult,
        TwoPlayerId,
        TwoPlayers<TicTacToePlayer>
        > {

    private val board: Array<Array<GameMark?>> = arrayOf(
        arrayOf(null, null, null),
        arrayOf(null, null, null),
        arrayOf(null, null, null)
    )

    private fun noPlayerWon(): Boolean =
        board.all { it.all { value -> value != null } }

    private fun playerWon(location: Location, mark: GameMark): Boolean =
        (board[location.x][0] == mark &&
                board[location.x][1] == mark &&
                board[location.x][2] == mark) ||
                (board[0][location.y] == mark &&
                        board[1][location.y] == mark &&
                        board[2][location.y] == mark
                        )

    override suspend fun play(context: TicTacToeGameContext): TicTacToeGameResult {

        context.sendToAllPlayers(GameStateChanged(board.map { it.copyOf() }.toTypedArray()))

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
                if (board[played.x][played.y] != null) {
                    return if (playerId == TwoPlayerId.PLAYER1) {
                        Player2Won
                    } else {
                        Player1Won
                    }
                }

                val mark = markForPlayer.getValue(playerId)
                context.sendToAllPlayers(PlayerPlacedMark(playerId, mark, played))
                board[played.x][played.y] = mark
                context.sendToAllPlayers(GameStateChanged(board.map { it.copyOf() }.toTypedArray()))

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
class GameStateChanged(val board: Array<Array<GameMark?>>) : TicTacToeEvent()

object TicTacToeGameParameters : GameParameters

sealed class TicTacToePlayerActions : PlayerActions
data class PlaceMarkLocation(val location: Location) : TicTacToePlayerActions()

sealed class TicTacToeGameResult : GameResult
object Player1Won : TicTacToeGameResult()
object Player2Won : TicTacToeGameResult()
object NoPlayerWon : TicTacToeGameResult()

interface TicTacToePlayer : Player<TicTacToeGameParameters, TicTacToeEvent, TicTacToePlayerActions, TicTacToeGameResult>

class FreeSpaceTicTacToePlayer : TicTacToePlayer {

    private lateinit var gameState: Array<Array<GameMark?>>

    override fun initialize(parameters: TicTacToeGameParameters, eventBus: ReceiveChannel<TicTacToeEvent>): suspend ProducerScope<TicTacToePlayerActions>.() -> Unit =
        {
            eventBus.consumeEach { event ->
                when (event) {
                    is GameStateChanged -> {
                        gameState = event.board
                    }
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