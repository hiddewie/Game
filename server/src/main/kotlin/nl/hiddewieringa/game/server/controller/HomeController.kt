package nl.hiddewieringa.game.server.controller

import nl.hiddewieringa.game.server.games.GameInstanceProvider
import nl.hiddewieringa.game.server.games.GameProvider
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class HomeController(
    val gameProvider: GameProvider,
    val gameInstanceProvider: GameInstanceProvider,
) {

    data class GameListItem(val slug: String, val name: String)

    @GetMapping("games")
    fun games(): List<GameListItem> =
        gameProvider.games()
            .map { GameListItem(it.slug, it.name) }

    data class OpenGame(val id: UUID, val playerSlotIds: Set<UUID>)

    @GetMapping("games/{gameSlug}/open")
    fun openGames(@PathVariable gameSlug: String): List<OpenGame> =
        gameInstanceProvider.openGames(gameSlug)
            .map { OpenGame(it.id, it.playerSlots.keys) }

    @PostMapping("games/{gameSlug}/start")
    suspend fun startGame(@PathVariable gameSlug: String): UUID =
        gameInstanceProvider.start(gameProvider.bySlug(gameSlug))
}
