package nl.hiddewieringa.game.taipan

import kotlinx.serialization.Serializable
import nl.hiddewieringa.game.core.PlayerActions
import nl.hiddewieringa.game.taipan.card.CardSet
import nl.hiddewieringa.game.taipan.card.ThreeWayPass

@Serializable
sealed class TaiPanPlayerActions : PlayerActions

@Serializable
data class CardPass(val cardPass: ThreeWayPass) : TaiPanPlayerActions()

@Serializable
data class PlayCards(val cards: CardSet, val addons: Set<PlayCardsAddon>) : TaiPanPlayerActions() {
    constructor(cards: CardSet) : this(cards, emptySet())
}

@Serializable
data class PassDragonTrick(val dragonPass: DragonPass) : TaiPanPlayerActions()

@Serializable
object Fold : TaiPanPlayerActions()

@Serializable
object RequestNextCards : TaiPanPlayerActions()

@Serializable
enum class DragonPass {
    LEFT,
    RIGHT
}

@Serializable
object CallTaiPan : TaiPanPlayerActions()
