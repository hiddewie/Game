package nl.hiddewieringa.game.tictactoe.state

import nl.hiddewieringa.game.core.TwoPlayerId
import nl.hiddewieringa.game.tictactoe.*

data class TicTacToePlayerState(
    val playerToPlay: TwoPlayerId?,
    val board: Array<Array<GameMark?>>,
    val playerWon: TwoPlayerId?,
    val gameFinished: Boolean,
)

fun TicTacToeState.toPlayerState(playerId: TwoPlayerId): TicTacToePlayerState =
    when (this) {
        is TicTacToePlay -> TicTacToePlayerState(
            playerToPlay,
            board,
            null,
            false
        )
        is PlayerWon -> TicTacToePlayerState(
            null,
            board,
            player,
            true
        )
        is NoPlayerWon -> TicTacToePlayerState(
            null,
            board,
            null,
            true,
        )
    }
