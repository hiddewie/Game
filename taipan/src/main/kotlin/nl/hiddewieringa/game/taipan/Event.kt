package nl.hiddewieringa.game.taipan

import kotlinx.serialization.Serializable
import nl.hiddewieringa.game.core.Event
import nl.hiddewieringa.game.core.PlayerSpecific
import nl.hiddewieringa.game.core.TwoTeamPlayerId
import nl.hiddewieringa.game.core.TwoTeamTeamId
import nl.hiddewieringa.game.taipan.card.CardCombination
import nl.hiddewieringa.game.taipan.card.CardSet
import nl.hiddewieringa.game.taipan.card.ThreeWayPass

@Serializable
sealed class TaiPanEvent : Event

@Serializable
data class CardsHaveBeenDealt(override val player: TwoTeamPlayerId, val cards: CardSet) : TaiPanEvent(), PlayerSpecific

// TODO rename -> exchanged
@Serializable
data class CardsHaveBeenPassed(override val player: TwoTeamPlayerId, val pass: ThreeWayPass) : TaiPanEvent(), PlayerSpecific

@Serializable
object AllPlayersHaveReceivedCards : TaiPanEvent()

@Serializable
object AllPlayersHavePassedCards : TaiPanEvent()

@Serializable
data class PlayerPlayedCards(val player: TwoTeamPlayerId, val cards: CardCombination, val mahjongRequest: MahjongRequest? = null) : TaiPanEvent()

@Serializable
data class PlayerFolds(val player: TwoTeamPlayerId) : TaiPanEvent()

@Serializable
data class DragonTrickWon(val player: TwoTeamPlayerId) : TaiPanEvent()

@Serializable
data class TrickWon(val playerToReceiveTrickCards: TwoTeamPlayerId, val nextPlayer: TwoTeamPlayerId) : TaiPanEvent()

@Serializable
data class RoundEnded(val roundIndex: Int, val roundScore: Map<TwoTeamTeamId, Int>) : TaiPanEvent()
data class MahjongWishRequested(val value: Int) : TaiPanEvent()

@Serializable
object MahjongWishFulfilled : TaiPanEvent()

@Serializable
data class PlayerIsOutOfCards(val player: TwoTeamPlayerId) : TaiPanEvent()

@Serializable
data class IllegalAction(val message: String, val action: TaiPanPlayerActions) : TaiPanEvent()

@Serializable
data class PlayerTaiPanned(val playerId: TwoTeamPlayerId, val status: TaiPanStatus) : TaiPanEvent()

@Serializable
data class PlayerPassedDragon(val playerId: TwoTeamPlayerId, val dragonPass: DragonPass) : TaiPanEvent()

@Serializable
data class GameEnded(val winningTeam: TwoTeamTeamId) : TaiPanEvent()
