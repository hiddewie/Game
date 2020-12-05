package nl.hiddewieringa.game

import kotlinx.coroutines.runBlocking
import nl.hiddewieringa.game.core.GameManager
import nl.hiddewieringa.game.core.TwoPlayers
import nl.hiddewieringa.game.core.TwoTeams
import nl.hiddewieringa.game.taipan.TaiPan
import nl.hiddewieringa.game.taipan.TaiPanGameParameters
import nl.hiddewieringa.game.taipan.player.SimpleTaiPanPlayer
import nl.hiddewieringa.game.tictactoe.FreeSpaceTicTacToePlayer
import nl.hiddewieringa.game.tictactoe.TicTacToe
import nl.hiddewieringa.game.tictactoe.TicTacToeGameParameters
import java.time.Instant

fun main() =
    runBlocking {
        val gameManager = GameManager()

        playTicTacToe(gameManager)
        playTaiPan(gameManager)
    }

private suspend fun playTicTacToe(gameManager: GameManager) {
    val ticTacToeResult = gameManager.play(
        { TicTacToe() },
        {
            TwoPlayers(
                FreeSpaceTicTacToePlayer(),
                FreeSpaceTicTacToePlayer()
            )
        },
        TicTacToeGameParameters
    )

    println(ticTacToeResult)
}

private suspend fun playTaiPan(gameManager: GameManager) {
    val taiPanResult = gameManager.play(
        ::TaiPan,
        {
            TwoTeams(
                TwoPlayers(SimpleTaiPanPlayer(), SimpleTaiPanPlayer()),
                TwoPlayers(SimpleTaiPanPlayer(), SimpleTaiPanPlayer()),
            )
        },
        TaiPanGameParameters(100, Instant.now().toEpochMilli())
    )

    println(taiPanResult)
}