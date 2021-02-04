package nl.hiddewieringa.game.server.games

import nl.hiddewieringa.game.core.*
import nl.hiddewieringa.game.taipan.*
import nl.hiddewieringa.game.taipan.state.TaiPanPlayerState
import nl.hiddewieringa.game.taipan.state.toPlayerState
import nl.hiddewieringa.game.tictactoe.*
import nl.hiddewieringa.game.tictactoe.state.TicTacToePlayerState
import nl.hiddewieringa.game.tictactoe.state.toPlayerState
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
    val description: String,
    val gameFactory: (M) -> S,
    val playerConfigurationFactory: (player: () -> P) -> PC,
    val defaultParameters: M,
    val playerState: S.(PID) -> PS,
)

@Component
class GameProvider {

    // TODO how will we handle seed?
    private val games = listOf<GameDetails<*, *, *, *, *, *, *, *>>(
        GameDetails("TicTacToe", "tic-tac-toe", "A game for two players that place alternating crosses and circles.", { TicTacToePlay() }, { player: () -> Player<TicTacToeGameParameters, TicTacToeEvent, TicTacToePlayerActions, TwoPlayerId, TicTacToePlayerState> -> TwoPlayers(player(), player()) }, TicTacToeGameParameters, TicTacToeState::toPlayerState),
        GameDetails("TaiPan", "tai-pan", "A two two-player team trick taking tactical card game.", { parameters -> TaiPan(parameters) }, { player: () -> Player<TaiPanGameParameters, TaiPanEvent, TaiPanPlayerActions, TwoTeamPlayerId, TaiPanPlayerState> -> TwoTeams(TwoPlayers(player(), player()), TwoPlayers(player(), player())) }, TaiPanGameParameters(1000, 0), TaiPanState::toPlayerState),
    )

    fun games(): List<GameDetails<*, *, *, *, *, *, *, *>> =
        games

    fun bySlug(gameSlug: String): GameDetails<*, *, *, *, *, *, *, *> =
        games.first { it.slug == gameSlug }
}
