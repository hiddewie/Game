package nl.hiddewieringa.game.taipan

import nl.hiddewieringa.game.core.Event
import nl.hiddewieringa.game.core.PlayerSpecific
import nl.hiddewieringa.game.core.TwoTeamPlayerId
import nl.hiddewieringa.game.core.TwoTeamTeamId
import nl.hiddewieringa.game.taipan.card.CardCombination
import nl.hiddewieringa.game.taipan.card.CardSet

sealed class TaiPanEvent : Event
data class CardsHaveBeenDealt(override val player: TwoTeamPlayerId, val cards: CardSet) : TaiPanEvent(), PlayerSpecific
data class CardsHaveBeenPassed(override val player: TwoTeamPlayerId, val cards: CardSet) : TaiPanEvent(), PlayerSpecific

data class RoundBegan(val roundIndex: Int) : TaiPanEvent()
data class TrickBegan(val startingPlayer: TwoTeamPlayerId) : TaiPanEvent()

data class PlayerPlayedCards(val player: TwoTeamPlayerId, val cards: CardCombination) : TaiPanEvent()
data class PlayerPasses(val player: TwoTeamPlayerId) : TaiPanEvent()
data class TrickWon(val player: TwoTeamPlayerId) : TaiPanEvent()

data class RoundEnded(val roundIndex: Int, val roundScore: Map<TwoTeamTeamId, Int>) : TaiPanEvent()
data class ScoreUpdated(val score: Map<TwoTeamTeamId, Int>) : TaiPanEvent()
data class MahjongWishRequested(val value: Int) : TaiPanEvent()
object MahjongWishFulfilled : TaiPanEvent()

object RequestPassCards : TaiPanEvent()
object RequestPlayCards : TaiPanEvent()
object RequestPassDragon : TaiPanEvent()
data class IllegalAction(val message: String, val action: TaiPanPlayerActions) : TaiPanEvent()
data class PlayerTaiPanned(val playerId: TwoTeamPlayerId, val status: TaiPanStatus) : TaiPanEvent()
