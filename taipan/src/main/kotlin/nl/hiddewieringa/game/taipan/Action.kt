package nl.hiddewieringa.game.taipan

import nl.hiddewieringa.game.core.PlayerActions
import nl.hiddewieringa.game.taipan.card.CardSet
import nl.hiddewieringa.game.taipan.card.ThreeWayPass

sealed class TaiPanPlayerActions : PlayerActions
data class CardPass(val cardPass: ThreeWayPass) : TaiPanPlayerActions()

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
