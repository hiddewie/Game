package nl.hiddewieringa.game.taipan.state

import kotlinx.serialization.Serializable
import nl.hiddewieringa.game.core.TwoTeamPlayerId
import nl.hiddewieringa.game.core.TwoTeamTeamId
import nl.hiddewieringa.game.taipan.*
import nl.hiddewieringa.game.taipan.card.CardSet

@Serializable
data class TaiPanPlayerState(
    val playersToPlay: List<TwoTeamPlayerId>,
    val cards: CardSet,
    val numberOfCardsPerPlayer: Map<TwoTeamPlayerId, Int>,
    val taiPannedPlayers: Map<TwoTeamPlayerId, TaiPanStatus>,
    val playedCards: CardSet,
    val points: Map<TwoTeamTeamId, Int>,
    val trickCards: CardSet,
    val roundCards: Map<TwoTeamPlayerId, CardSet>,
    val roundIndex: Int?,
    val trickIndex: Int?,
    val outOfCardOrder: List<TwoTeamPlayerId>,
)

fun TaiPanState.toPlayerState(playerId: TwoTeamPlayerId): TaiPanPlayerState =
    when (this) {
        is TaiPan -> TaiPanPlayerState(
            TwoTeamPlayerId.values().filter { playerCards.getValue(it).size < 14 },
            playerCards.getValue(playerId),
            playerCards.map { (key, value) -> key to value.size }.toMap(),
            taiPannedPlayers,
            emptySet(),
            points,
            emptySet(),
            emptyMap(),
            null,
            null,
            emptyList(),
        )
        is TaiPanPassCards -> TaiPanPlayerState(
            TwoTeamPlayerId.values().filterNot(passedCards::containsKey),
            playerCards.getValue(playerId),
            playerCards.map { (key, value) -> key to value.size }.toMap(),
            taiPannedPlayers,
            emptySet(),
            points,
            emptySet(),
            emptyMap(),
            null,
            null,
            emptyList(),
        )
        is TaiPanPlayTrick -> TaiPanPlayerState(
            listOf(currentPlayer),
            playerCards.getValue(playerId),
            playerCards.map { (key, value) -> key to value.size }.toMap(),
            taiPannedPlayers,
            roundCards.flatMapTo(mutableSetOf()) { it.value } + trickCards,
            points,
            trickCards,
            roundCards,
            roundIndex,
            trickIndex,
            outOfCardOrder,
        )
        is TaiPanDragonPass -> TaiPanPlayerState(
            listOf(trick.currentPlayer),
            trick.playerCards.getValue(playerId),
            trick.playerCards.map { (key, value) -> key to value.size }.toMap(),
            trick.taiPannedPlayers,
            trick.roundCards.flatMapTo(mutableSetOf()) { it.value } + trick.trickCards,
            trick.points,
            trick.trickCards,
            trick.roundCards,
            trick.roundIndex,
            trick.trickIndex,
            trick.outOfCardOrder
        )
        is TaiPanFinalScore -> TaiPanPlayerState(
            emptyList(),
            emptySet(),
            emptyMap(),
            emptyMap(),
            emptySet(),
            points,
            emptySet(),
            emptyMap(),
            null,
            null,
            emptyList(),
        )
    }
