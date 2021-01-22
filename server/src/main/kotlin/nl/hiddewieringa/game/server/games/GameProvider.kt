package nl.hiddewieringa.game.server.games

import nl.hiddewieringa.game.core.*
import nl.hiddewieringa.game.taipan.*
import nl.hiddewieringa.game.taipan.state.TaiPanPlayerState
import nl.hiddewieringa.game.taipan.state.toPlayerState
import nl.hiddewieringa.game.tictactoe.*
import org.springframework.stereotype.Component

data class GameDetails<
        M : GameParameters,
        P : Player<M, E, A, PID, PS>,
        A : PlayerActions,
        E : Event,
        PID : PlayerId,
        PC : PlayerConfiguration<PID, P>,
        S : GameState<S>,
        PS : Any,
        >(
    val name: String,
    val slug: String,
    val gameFactory: (M) -> S,
    val playerConfigurationFactory: (player: () -> P) -> PC,
    val defaultParameters: M,
    val playerState: S.(PID) -> PS,
)

@Component
class GameProvider {

    // TODO how will we handle seed?
    private val games = listOf<GameDetails<*, *, *, *, *, *, *, *>>(
        GameDetails("TicTacToe", "tic-tac-toe", { TicTacToePlay() }, { player: () -> Player<TicTacToeGameParameters, TicTacToeEvent, TicTacToePlayerActions, TwoPlayerId, TicTacToeState> -> TwoPlayers(player(), player()) }, TicTacToeGameParameters, { this }),
        GameDetails("TaiPan", "tai-pan", { parameters -> TaiPan(parameters) }, { player: () -> Player<TaiPanGameParameters, TaiPanEvent, TaiPanPlayerActions, TwoTeamPlayerId, TaiPanPlayerState> -> TwoTeams(TwoPlayers(player(), player()), TwoPlayers(player(), player())) }, TaiPanGameParameters(1000, 0), TaiPanState::toPlayerState),
    )

    fun games(): List<GameDetails<*, *, *, *, *, *, *, *>> =
        games

    fun bySlug(gameSlug: String): GameDetails<*, *, *, *, *, *, *, *> =
        games.first { it.slug == gameSlug }
}
