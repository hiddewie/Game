package nl.hiddewieringa.game.server.controller

import nl.hiddewieringa.game.server.games.GameInstance
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

    data class GameListItem(val slug: String, val name: String, val description: String)

    @GetMapping("games")
    fun games(): List<GameListItem> =
        gameProvider.games()
            .map { GameListItem(it.slug, it.name, it.description) }

    data class OpenGamePlayerSlot(val id: UUID, val name: String)
    data class OpenGame(val id: UUID, val playerSlotIds: List<OpenGamePlayerSlot>)

    @GetMapping("games/{gameSlug}/open")
    fun openGames(@PathVariable gameSlug: String): List<OpenGame> =
        gameInstanceProvider.openGames(gameSlug)
            .map(::generateOpenGames)

    private fun generateOpenGames(gameInstance: GameInstance<*, *, *, *>) =
        OpenGame(gameInstance.id, gameInstance.playerSlots
            .filterValues { it.referenceCount.get() == 0 }
            .map { (key, value) -> OpenGamePlayerSlot(key, value.playerId.toString()) })

    @PostMapping("games/{gameSlug}/start")
    suspend fun startGame(@PathVariable gameSlug: String): UUID =
        gameInstanceProvider.start(gameProvider.bySlug(gameSlug))
}
