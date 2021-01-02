package nl.hiddewieringa.game.server.games

import nl.hiddewieringa.game.core.*
import nl.hiddewieringa.game.taipan.*
import nl.hiddewieringa.game.tictactoe.*
import org.springframework.stereotype.Component

data class GameDetails<
    M : GameParameters,
    P : Player<M, E, A, PID, S>,
    A : PlayerActions,
    E : Event,
    PID : PlayerId,
    PC : PlayerConfiguration<PID, P>,
    S : State<S>
    >(
    val name: String,
    val slug: String,
    val gameFactory: (M) -> S,
    val playerConfigurationFactory: (player: () -> P) -> PC,
    val defaultParameters: M,
    // The action base class is required for deserializing action messages for a specific game instance.
//    val actionClass: Class<A>,
)

@Component
class GameProvider {

    // TODO how will we handle seed?
    private val games = listOf<GameDetails<*, *, *, *, *, *, *>>(
        GameDetails("TicTacToe", "tic-tac-toe", { TicTacToePlay() }, { player: () -> Player<TicTacToeGameParameters, TicTacToeEvent, TicTacToePlayerActions, TwoPlayerId, TicTacToeState> -> TwoPlayers(player(), player()) }, TicTacToeGameParameters), // , TicTacToePlayerActions::class.java),
        GameDetails("TaiPan", "tai-pan", { parameters -> TaiPan(parameters) }, { player: () -> Player<TaiPanGameParameters, TaiPanEvent, TaiPanPlayerActions, TwoTeamPlayerId, TaiPanState> -> TwoTeams(TwoPlayers(player(), player()), TwoPlayers(player(), player())) }, TaiPanGameParameters(1000, 0)), // , TaiPanPlayerActions::class.java),
    )

    fun games(): List<GameDetails<*, *, *, *, *, *, *>> =
        games

    fun bySlug(gameSlug: String): GameDetails<*, *, *, *, *, *, *> =
        games.first { it.slug == gameSlug }
}
