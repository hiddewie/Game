package nl.hiddewieringa.game.server.games

import nl.hiddewieringa.game.core.*
import nl.hiddewieringa.game.taipan.TaiPan
import nl.hiddewieringa.game.taipan.TaiPanGameParameters
import nl.hiddewieringa.game.tictactoe.TicTacToe
import nl.hiddewieringa.game.tictactoe.TicTacToeGameParameters
import org.springframework.stereotype.Component
import java.util.*

data class GameDetails<
    M : GameParameters,
    P : Player<M, E, S, A, R>,
    A : PlayerActions,
    E : Event,
    R : GameResult,
    PID : PlayerId,
    PC : PlayerConfiguration<PID, P>,
    S : GameState
    >(
    val id: UUID,
    val name: String,
    val slug: String,
    val gameFactory: (M) -> Game<M, P, A, E, R, PID, PC, S>,
    val playerConfigurationFactory: (player: () -> P) -> PC,
    val defaultParameters: M,
    // The action base class is required for deserializing action messages for a specific game instance.
//    val actionClass: Class<A>,
)

@Component
class GameProvider {

    // TODO how will we handle seed?
    private val games = listOf<GameDetails<*, *, *, *, *, *, *, *>>(
        GameDetails(UUID.randomUUID(), "TicTacToe", "tic-tac-toe", ::TicTacToe, { player -> TwoPlayers(player(), player()) }, TicTacToeGameParameters), // , TicTacToePlayerActions::class.java),
        GameDetails(UUID.randomUUID(), "TaiPan", "tai-pan", ::TaiPan, { player -> TwoTeams(TwoPlayers(player(), player()), TwoPlayers(player(), player())) }, TaiPanGameParameters(1000, 0)), // , TaiPanPlayerActions::class.java),
    )

    fun games(): List<GameDetails<*, *, *, *, *, *, *, *>> =
        games

    fun byId(gameId: UUID): GameDetails<*, *, *, *, *, *, *, *> =
        games.first { it.id == gameId }
}
