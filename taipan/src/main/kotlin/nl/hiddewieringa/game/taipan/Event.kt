package nl.hiddewieringa.game.taipan

import nl.hiddewieringa.game.core.*
import nl.hiddewieringa.game.taipan.card.CardCombination
import nl.hiddewieringa.game.taipan.card.CardSet
import nl.hiddewieringa.game.taipan.card.ThreeWayPass

sealed class TaiPanEvent : Event
data class CardsHaveBeenDealt(override val player: TwoTeamPlayerId, val cards: CardSet) : TaiPanEvent(), PlayerSpecific
data class CardsHaveBeenPassed(override val player: TwoTeamPlayerId, val pass: ThreeWayPass) : TaiPanEvent(), PlayerSpecific
object AllPlayersHaveReceivedCards : TaiPanEvent()
object AllPlayersHavePassedCards : TaiPanEvent()

data class RoundBegan(val roundIndex: Int) : TaiPanEvent()
data class TrickBegan(val startingPlayer: TwoTeamPlayerId) : TaiPanEvent()

data class PlayerPlayedCards(val player: TwoTeamPlayerId, val cards: CardCombination, val mahjongRequest: MahjongRequest? = null) : TaiPanEvent()
data class PlayerFolds(val player: TwoTeamPlayerId) : TaiPanEvent()
data class DragonTrickWon(val player: TwoTeamPlayerId) : TaiPanEvent()
data class TrickWon(val playerToReceiveTrickCards: TwoTeamPlayerId, val nextPlayer: TwoTeamPlayerId) : TaiPanEvent()

data class RoundEnded(val roundIndex: Int, val roundScore: Map<TwoTeamTeamId, Int>) : TaiPanEvent()
data class ScoreUpdated(val score: Map<TwoTeamTeamId, Int>) : TaiPanEvent()
data class MahjongWishRequested(val value: Int) : TaiPanEvent()
object MahjongWishFulfilled : TaiPanEvent()
data class PlayerIsOutOfCards(val player: TwoTeamPlayerId) : TaiPanEvent()

object RequestPassCards : TaiPanEvent()
object RequestPlayCards : TaiPanEvent()
object RequestPassDragon : TaiPanEvent()
data class IllegalAction(val message: String, val action: TaiPanPlayerActions) : TaiPanEvent()
data class PlayerTaiPanned(val playerId: TwoTeamPlayerId, val status: TaiPanStatus) : TaiPanEvent()
data class PlayerPassedDragon(val playerId: TwoTeamPlayerId, val dragonPass: DragonPass) : TaiPanEvent()

data class GameEnded(val winningTeam: TwoTeamTeamId) : TaiPanEvent()
