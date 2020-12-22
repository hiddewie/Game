package nl.hiddewieringa.game.server.controller

import nl.hiddewieringa.game.server.games.GameDetails
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

    @GetMapping("games")
    fun games(): List<GameDetails<*, *, *, *, *, *, *, *>> =
        gameProvider.games()

    data class OpenGame(val id: UUID, val playerSlotIds: Set<UUID>)

    @GetMapping("games/{gameId}/open")
    fun openGames(@PathVariable gameId: UUID): List<OpenGame> =
        gameInstanceProvider.openGames(gameId)
            .map { OpenGame(it.id, it.playerSlots.keys) }

    @GetMapping("games/{gameId}/instance/{instanceId}")
    fun instanceDetails(@PathVariable gameId: UUID, @PathVariable instanceId: UUID): GameInstance<*, *, *>? =
        gameInstanceProvider.gameInstance(gameId)

    @PostMapping("games/{gameId}/start")
    suspend fun startGame(@PathVariable gameId: UUID): UUID =
        gameInstanceProvider.start(gameProvider.byId(gameId))
}
