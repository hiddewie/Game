package nl.hiddewieringa.game.taipan.state

import nl.hiddewieringa.game.core.TwoTeamPlayerId
import nl.hiddewieringa.game.taipan.*
import nl.hiddewieringa.game.taipan.card.Card

data class TaiPanPlayerState(
    val playerId: TwoTeamPlayerId,
    val cards: List<Card>,
)

fun TaiPanState.toPlayerState(playerId: TwoTeamPlayerId): TaiPanPlayerState =
    when (this) {
        is TaiPan -> TaiPanPlayerState(
            playerId,
            playerCards.getValue(playerId).toList(),
        )
        is TaiPanPassCards -> TaiPanPlayerState(
            playerId,
            playerCards.getValue(playerId).toList(),
        )
        is TaiPanPlayTrick -> TaiPanPlayerState(
            playerId,
            playerCards.getValue(playerId).toList(),
        )
        is TaiPanDragonPass -> TaiPanPlayerState(
            playerId,
            trick.playerCards.getValue(playerId).toList(),
        )
        is TaiPanFinalScore -> TaiPanPlayerState(
            playerId,
            emptyList(),
        )
    }