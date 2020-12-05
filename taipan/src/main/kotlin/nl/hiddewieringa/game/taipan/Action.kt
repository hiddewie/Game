package nl.hiddewieringa.game.taipan

import nl.hiddewieringa.game.core.PlayerActions
import nl.hiddewieringa.game.taipan.card.Card
import nl.hiddewieringa.game.taipan.card.CardSet

sealed class TaiPanPlayerActions : PlayerActions
data class CardPass(val left: Card, val forward: Card, val right: Card) : TaiPanPlayerActions() {
    init {
        require(left != forward) { "Left should not be the same card as forward" }
        require(forward != right) { "Right should not be the same card as forward" }
        require(left != right) { "Left should not be the same card as right" }
    }
}

data class PlayCards(val cards: CardSet, val addons: Set<PlayCardsAddon>) : TaiPanPlayerActions() {
    constructor(cards: CardSet) : this(cards, emptySet())
}

data class PassDragonTrick(val dragonPass: DragonPass) : TaiPanPlayerActions()
object Fold : TaiPanPlayerActions()
object RequestNextCards : TaiPanPlayerActions()

enum class DragonPass {
    LEFT,
    RIGHT
}

object CallTaiPan : TaiPanPlayerActions()
