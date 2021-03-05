package nl.hiddewieringa.game.taipan.state

import kotlinx.serialization.Serializable
import nl.hiddewieringa.game.core.TwoTeamPlayerId
import nl.hiddewieringa.game.core.TwoTeamTeamId
import nl.hiddewieringa.game.taipan.*
import nl.hiddewieringa.game.taipan.card.CardCombination
import nl.hiddewieringa.game.taipan.card.CardSet

enum class TaiPanPlayerStateType {
    RECEIVE_CARDS,
    EXCHANGE_CARDS,
    PLAY,
    PASS_DRAGON,
    GAME_FINISHED
}

@Serializable
data class TaiPanPlayerState(
    val stateType: TaiPanPlayerStateType,
    val playersToPlay: List<TwoTeamPlayerId>,
    val cards: CardSet,
    val numberOfCardsPerPlayer: Map<TwoTeamPlayerId, Int>,
    val taiPannedPlayers: Map<TwoTeamPlayerId, TaiPanStatus>,
    val playedCards: CardSet,
    val points: Map<TwoTeamTeamId, Int>,
    val trickCards: CardSet,
    val lastPlayedCards: Triple<TwoTeamPlayerId, CardCombination, MahjongRequest?>?,
    val mahjongWish: Int?,
    val roundCards: Map<TwoTeamPlayerId, CardSet>,
    val roundIndex: Int?,
    val trickIndex: Int?,
    val outOfCardOrder: List<TwoTeamPlayerId>,
)

fun TaiPanState.toPlayerState(playerId: TwoTeamPlayerId): TaiPanPlayerState =
    when (this) {
        is TaiPan -> TaiPanPlayerState(
            TaiPanPlayerStateType.RECEIVE_CARDS,
            TwoTeamPlayerId.values().filter { playerCards.getValue(it).size < 14 },
            playerCards.getValue(playerId),
            playerCards.map { (key, value) -> key to value.size }.toMap(),
            taiPannedPlayers,
            emptySet(),
            points,
            emptySet(),
            null,
            null,
            emptyMap(),
            null,
            null,
            emptyList(),
        )
        is TaiPanPassCards -> TaiPanPlayerState(
            TaiPanPlayerStateType.EXCHANGE_CARDS,
            TwoTeamPlayerId.values().filterNot(passedCards::containsKey),
            playerCards.getValue(playerId),
            playerCards.map { (key, value) -> key to value.size }.toMap(),
            taiPannedPlayers,
            emptySet(),
            points,
            emptySet(),
            null,
            null,
            emptyMap(),
            null,
            null,
            emptyList(),
        )
        is TaiPanPlayTrick -> TaiPanPlayerState(
            TaiPanPlayerStateType.PLAY,
            listOf(currentPlayer),
            playerCards.getValue(playerId),
            playerCards.map { (key, value) -> key to value.size }.toMap(),
            taiPannedPlayers,
            roundCards.flatMapTo(mutableSetOf()) { it.value } + trickCards,
            points,
            trickCards,
            lastPlayedCards,
            mahjongWish.wish,
            roundCards,
            roundIndex,
            trickIndex,
            outOfCardOrder,
        )
        is TaiPanDragonPass -> TaiPanPlayerState(
            TaiPanPlayerStateType.PASS_DRAGON,
            listOf(trick.currentPlayer),
            trick.playerCards.getValue(playerId),
            trick.playerCards.map { (key, value) -> key to value.size }.toMap(),
            trick.taiPannedPlayers,
            trick.roundCards.flatMapTo(mutableSetOf()) { it.value } + trick.trickCards,
            trick.points,
            trick.trickCards,
            trick.lastPlayedCards,
            trick.mahjongWish.wish,
            trick.roundCards,
            trick.roundIndex,
            trick.trickIndex,
            trick.outOfCardOrder
        )
        is TaiPanFinalScore -> TaiPanPlayerState(
            TaiPanPlayerStateType.GAME_FINISHED,
            emptyList(),
            emptySet(),
            emptyMap(),
            emptyMap(),
            emptySet(),
            points,
            emptySet(),
            null,
            null,
            emptyMap(),
            null,
            null,
            emptyList(),
        )
    }
