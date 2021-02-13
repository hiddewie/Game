package nl.hiddewieringa.game.taipan

import kotlinx.serialization.Serializable
import nl.hiddewieringa.game.taipan.card.NumberedCard

@Serializable
sealed class PlayCardsAddon
@Serializable
data class PhoenixValue(val value: Int) : PlayCardsAddon() {
    init {
        require(value >= 2) { "The card value should be greater or equal to two." }
        require(value <= NumberedCard.ACE) { "The card value should be less than or equal to ACE (14)." }
    }
}

@Serializable
data class MahjongRequest(val value: Int) : PlayCardsAddon() {
    init {
        require(value >= 2) { "The card value should be greater or equal to two." }
        require(value <= NumberedCard.ACE) { "The card value should be less than or equal to ACE (14)." }
    }
}
