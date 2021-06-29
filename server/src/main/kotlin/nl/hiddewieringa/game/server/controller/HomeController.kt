package nl.hiddewieringa.game.server.controller

import kotlinx.serialization.json.Json
import nl.hiddewieringa.game.core.GameParameters
import nl.hiddewieringa.game.server.games.GameDetails
import nl.hiddewieringa.game.server.games.GameInstance
import nl.hiddewieringa.game.server.games.GameInstanceProvider
import nl.hiddewieringa.game.server.games.GameProvider
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class HomeController(
    val gameProvider: GameProvider,
    val gameInstanceProvider: GameInstanceProvider,
) {
    val serializer = Json {}

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
        OpenGame(
            gameInstance.id,
            gameInstance.playerSlots
                .filterValues { it.referenceCount.get() == 0 }
                .map { (key, value) -> OpenGamePlayerSlot(key, value.playerId.toString()) }
        )

    @PostMapping("games/{gameSlug}/start")
    suspend fun startGame(@PathVariable gameSlug: String, @RequestBody parameters: String): UUID =
        startDeserialized(gameProvider.bySlug(gameSlug), parameters)

    private suspend fun <M : GameParameters> startDeserialized(game: GameDetails<M, *, *, *, *, *, *, *>, parameters: String): UUID =
        gameInstanceProvider.start(game, serializer.decodeFromString(game.parameterSerializer, parameters))

}
