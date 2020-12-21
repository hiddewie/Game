package nl.hiddewieringa.game.server.games

import org.springframework.stereotype.Component
import java.util.*

data class GameDetails(
    val id: UUID,
    val name: String,
    val slug: String,
)

@Component
class GameProvider {

    private val games = listOf(
        GameDetails(UUID.randomUUID(), "TicTacToe", "tic-tac-toe"),
        GameDetails(UUID.randomUUID(), "TaiPan", "tai-pan"),
    )

    fun games(): List<GameDetails> =
        games

    fun byId(gameId: UUID): GameDetails =
        games.first { it.id == gameId }
}
