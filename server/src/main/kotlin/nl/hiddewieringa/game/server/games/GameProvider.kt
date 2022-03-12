package nl.hiddewieringa.game.server.games

import kotlinx.serialization.KSerializer
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
    val playerState: S.(PID) -> PS,
    val parameterSerializer: KSerializer<M>,
    val actionSerializer: KSerializer<A>,
    val eventSerializer: KSerializer<E>,
    val stateSerializer: KSerializer<S>,
    val playerStateSerializer: KSerializer<PS>,
    val playerIdSerializer: KSerializer<PID>,
)

@Component
class GameProvider {

    private val ticTacToeDetails = GameDetails(
        "TicTacToe",
        "tic-tac-toe",
        "A game for two players that place alternating crosses and circles.",
        { TicTacToePlay() },
        { player: () -> Player<TicTacToeGameParameters, TicTacToeEvent, TicTacToePlayerActions, TwoPlayerId, TicTacToePlayerState> ->
            TwoPlayers(player(), player())
        },
        TicTacToeState::toPlayerState,
        TicTacToeGameParameters.serializer(),
        TicTacToePlayerActions.serializer(),
        TicTacToeEvent.serializer(),
        TicTacToeState.serializer(),
        TicTacToePlayerState.serializer(),
        TwoPlayerId.serializer()
    )

    private val taiPanDetails = GameDetails(
        "TaiPan",
        "tai-pan",
        "A two two-player team trick taking tactical card game.",
        { parameters -> TaiPan(parameters) },
        { player: () -> Player<TaiPanGameParameters, TaiPanEvent, TaiPanPlayerActions, TwoTeamPlayerId, TaiPanPlayerState> ->
            TwoTeams(TwoPlayers(player(), player()), TwoPlayers(player(), player()))
        },
        TaiPanState::toPlayerState,
        TaiPanGameParameters.serializer(),
        TaiPanPlayerActions.serializer(),
        TaiPanEvent.serializer(),
        TaiPanState.serializer(),
        TaiPanPlayerState.serializer(),
        TwoTeamPlayerId.serializer()
    )

    private val games = listOf<GameDetails<*, *, *, *, *, *, *, *>>(
        ticTacToeDetails,
        taiPanDetails,
    ).associateBy { it.slug }
    val sortedGames = games.values.sortedBy { it.slug }

    fun games(): List<GameDetails<*, *, *, *, *, *, *, *>> =
        sortedGames

    fun bySlug(gameSlug: String): GameDetails<*, *, *, *, *, *, *, *> =
        games.getValue(gameSlug)
}
