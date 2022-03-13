package nl.hiddewieringa.game.server.controller

import kotlinx.serialization.json.Json
import nl.hiddewieringa.game.core.GameParameters
import nl.hiddewieringa.game.server.games.GameDetails
import nl.hiddewieringa.game.server.games.GameInstanceProvider
import nl.hiddewieringa.game.server.games.GameProvider
import nl.hiddewieringa.game.server.games.OpenGame
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class HomeController(
    val gameProvider: GameProvider,
    val gameInstanceProvider: GameInstanceProvider,
) {
    private val serializer = Json.Default

    data class GameListItem(val slug: String, val name: String, val description: String)

    @GetMapping("games")
    fun games(): List<GameListItem> =
        gameProvider.games()
            .map { GameListItem(it.slug, it.name, it.description) }

    @GetMapping("games/{gameSlug}/open")
    fun openGames(@PathVariable gameSlug: String): List<OpenGame> =
        gameInstanceProvider.openGames(gameSlug)

    @PostMapping("games/{gameSlug}/start")
    suspend fun startGame(@PathVariable gameSlug: String, @RequestBody parameters: String): UUID =
        startDeserialized(gameProvider.bySlug(gameSlug), parameters)

    private suspend fun <M : GameParameters> startDeserialized(game: GameDetails<M, *, *, *, *, *, *, *>, parameters: String): UUID =
        gameInstanceProvider.start(game, serializer.decodeFromString(game.parameterSerializer, parameters))
}
