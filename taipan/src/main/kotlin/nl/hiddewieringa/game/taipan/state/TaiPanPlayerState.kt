package nl.hiddewieringa.game.taipan.state

import kotlinx.serialization.Serializable
import nl.hiddewieringa.game.core.TwoTeamPlayerId
import nl.hiddewieringa.game.core.TwoTeamTeamId
import nl.hiddewieringa.game.taipan.*
import nl.hiddewieringa.game.taipan.card.Card
import nl.hiddewieringa.game.taipan.card.fullDeck

@Serializable
data class TaiPanPlayerState(
    val playersToPlay: List<TwoTeamPlayerId>,
    val cards: List<Card>,
    val numberOfCardsPerPlayer: Map<TwoTeamPlayerId, Int>,
    val taiPannedPlayers: Map<TwoTeamPlayerId, TaiPanStatus>,
    val cardsInGame: List<Card>,
    val points: Map<TwoTeamTeamId, Int>,
    val roundIndex: Int?,
    val trickIndex: Int?,
)

fun TaiPanState.toPlayerState(playerId: TwoTeamPlayerId): TaiPanPlayerState =
    when (this) {
        is TaiPan -> TaiPanPlayerState(
            emptyList(),
            playerCards.getValue(playerId).toList(),
            playerCards.map { (key, value) -> key to value.size }.toMap(),
            taiPannedPlayers,
            (fullDeck.toSet() - playerCards.flatMap { it.value }.toSet()).toList(),
            points,
            null,
            null
        )
        is TaiPanPassCards -> TaiPanPlayerState(
            TwoTeamPlayerId.values().filterNot(passedCards::containsKey),
            playerCards.getValue(playerId).toList(),
            playerCards.map { (key, value) -> key to value.size }.toMap(),
            taiPannedPlayers,
            (fullDeck.toSet() - playerCards.flatMap { it.value }.toSet()).toList(),
            points,
            null,
            null
        )
        is TaiPanPlayTrick -> TaiPanPlayerState(
            listOf(currentPlayer),
            playerCards.getValue(playerId).toList(),
            playerCards.map { (key, value) -> key to value.size }.toMap(),
            taiPannedPlayers,
            (fullDeck.toSet() - playerCards.flatMap { it.value }.toSet()).toList(),
            points,
            roundIndex,
            trickIndex
        )
        is TaiPanDragonPass -> TaiPanPlayerState(
            listOf(trick.currentPlayer),
            trick.playerCards.getValue(playerId).toList(),
            trick.playerCards.map { (key, value) -> key to value.size }.toMap(),
            trick.taiPannedPlayers,
            (fullDeck.toSet() - trick.playerCards.flatMap { it.value }.toSet()).toList(),
            trick.points,
            trick.roundIndex,
            trick.trickIndex
        )
        is TaiPanFinalScore -> TaiPanPlayerState(
            emptyList(),
            emptyList(),
            emptyMap(),
            emptyMap(),
            emptyList(),
            points,
            null,
            null
        )
    }
