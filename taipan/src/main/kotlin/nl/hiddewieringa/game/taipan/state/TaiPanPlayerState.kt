package nl.hiddewieringa.game.taipan.state

import nl.hiddewieringa.game.core.TwoTeamPlayerId
import nl.hiddewieringa.game.taipan.TaiPanState

data class TaiPanPlayerState(
    val playerId: TwoTeamPlayerId,
)

fun TaiPanState.toPlayerState(playerId: TwoTeamPlayerId): TaiPanPlayerState {
    return TaiPanPlayerState(
        playerId
    )
}