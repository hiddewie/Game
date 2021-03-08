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
data class CardsHaveBeenDealt(override val player: TwoTeamPlayerId, val cards: CardSet) : TaiPanEvent(), PlayerSpecific {
    override fun toString(): String = when (cards.size) {
        8 -> "Player $player received the first eight cards."
        6 -> "Player $player received the next six cards."
        else -> "Player $player received ${cards.size} cards."
    }
}

@Serializable
data class CardsHaveBeenExchanged(override val player: TwoTeamPlayerId, val pass: ThreeWayPass) : TaiPanEvent(), PlayerSpecific {
    override fun toString(): String =
        "Player $player exchanged cards."
}

@Serializable
object AllPlayersHaveReceivedCards : TaiPanEvent() {
    override fun toString(): String =
        "All players have received their cards."
}

@Serializable
object AllPlayersHaveExchangedCards : TaiPanEvent() {
    override fun toString(): String =
        "All players have exchanged their cards."
}

@Serializable
data class PlayerPlayedCards(val player: TwoTeamPlayerId, val cards: CardCombination, val mahjongRequest: MahjongRequest? = null) : TaiPanEvent() {
    override fun toString(): String =
        "Player $player has played cards $cards ${mahjongRequest?.toString() ?: "without a Mahjong wish"}."
}

@Serializable
data class PlayerFolds(val player: TwoTeamPlayerId) : TaiPanEvent() {
    override fun toString(): String =
        "Player $player folds."
}

@Serializable
data class DragonTrickWon(val player: TwoTeamPlayerId) : TaiPanEvent() {
    override fun toString(): String =
        "Player $player won the trick."
}

@Serializable
data class TrickWon(val playerToReceiveTrickCards: TwoTeamPlayerId, val nextPlayer: TwoTeamPlayerId) : TaiPanEvent() {
    override fun toString(): String =
        "Player $playerToReceiveTrickCards won the trick.${if (playerToReceiveTrickCards != nextPlayer) " Next player: $nextPlayer." else ""}"
}

@Serializable
data class RoundEnded(val roundIndex: Int, val roundScore: Map<TwoTeamTeamId, Int>) : TaiPanEvent() {
    override fun toString(): String =
        "Round $roundIndex ended. Score: team 1: ${roundScore.getValue(TwoTeamTeamId.TEAM1)}, team 2: ${roundScore.getValue(TwoTeamTeamId.TEAM2)}."
}

data class MahjongWishRequested(val value: Int) : TaiPanEvent() {
    override fun toString(): String =
        "Mahjong wish $value."
}

@Serializable
object MahjongWishFulfilled : TaiPanEvent() {
    override fun toString(): String =
        "Mahjong wish has been fulfilled."
}

@Serializable
data class PlayerIsOutOfCards(val player: TwoTeamPlayerId) : TaiPanEvent() {
    override fun toString(): String =
        "Player $player is out of cards."
}

@Serializable
data class IllegalAction(val message: String, val player: TwoTeamPlayerId, val action: TaiPanPlayerActions) : TaiPanEvent() {
    override fun toString(): String =
        "Player $player tried to perform an illegal action, '$message'."
}

@Serializable
data class PlayerTaiPanned(val player: TwoTeamPlayerId, val status: TaiPanStatus) : TaiPanEvent() {
    override fun toString(): String =
        "Player $player Tai Panned: ${status.text()} Tai Pan!"

    private fun TaiPanStatus.text() = when (this) {
        TaiPanStatus.GREAT -> "great"
        TaiPanStatus.NORMAL -> "normal"
    }
}

@Serializable
data class PlayerPassedDragon(val player: TwoTeamPlayerId, val dragonPass: DragonPass) : TaiPanEvent() {
    override fun toString(): String =
        "Player $player passed the dragon to the ${dragonPass.text()}."

    private fun DragonPass.text() = when (this) {
        DragonPass.LEFT -> "left"
        DragonPass.RIGHT -> "right"
    }
}

@Serializable
data class GameEnded(val winningTeam: TwoTeamTeamId) : TaiPanEvent() {
    override fun toString(): String =
        "Game has ended, team $winningTeam won!"
}

