package nl.hiddewieringa.game.taipan.player

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import nl.hiddewieringa.game.taipan.TaiPanGameParameters
import nl.hiddewieringa.game.taipan.TaiPanGameResult
import nl.hiddewieringa.game.core.Player
import nl.hiddewieringa.game.taipan.*
import nl.hiddewieringa.game.taipan.card.*

typealias TaiPanPlayer = Player<TaiPanGameParameters, TaiPanEvent, TaiPanPlayerActions, TaiPanGameResult>

class SimpleTaiPanPlayer : TaiPanPlayer {

    private lateinit var hand: CardSet
    private var lastPlayedCards: CardCombination? = null

    override fun initialize(parameters: TaiPanGameParameters, eventBus: ReceiveChannel<TaiPanEvent>): suspend ProducerScope<TaiPanPlayerActions>.() -> Unit =
        {
            eventBus.consumeEach { event ->
                when (event) {
                    is CardsHaveBeenDealt -> {
                        hand = event.cards
                    }
                    is CardsHaveBeenPassed -> {
                        hand = event.cards
                    }
                    is RoundBegan -> {
                    }
                    is TrickBegan -> {
                    }
                    is PlayerPlayedCards -> {
                        lastPlayedCards = event.cards
                    }
                    is PlayerPasses -> {
                    }
                    is TrickWon -> {
                    }
                    is RoundEnded -> {
                    }
                    RequestPassCards -> send(passCards())
                    RequestPlayCards -> {
                        val action: TaiPanPlayerActions = when (val cardsToPlay = play()) {
                            null -> Pass
                            else -> PlayCards(cardsToPlay)
                        }
                        send(action)
                    }
                    RequestPassDragon -> send(PassDragonTrick(dragonPass()))
                    is IllegalAction -> {
                    }
                    is ScoreUpdated -> {
                    }
                    is PlayerTaiPanned -> {
                    }
                    is MahjongWishRequested -> {
                    }
                    MahjongWishFulfilled -> {
                    }
                }
            }
        }

    private fun passCards(): CardPass {
        val ordered = hand.toList()
        return CardPass(ordered[0], ordered[1], ordered[2])
    }

    /**
     * Passes in case of null
     */
    private fun play(): CardSet? {
        // find first valid card combination to play
        return when (val stack = lastPlayedCards) {
            is HighCard -> hand.firstOrNull {
                it is Dragon ||
                        (stack.card !is Dragon && it is Phoenix) ||
                        (it is NumberedCard && stack.card is NumberedCard && stack.card.value < it.value)
            }
                ?.let { setOf(it) }
            else -> null
        }
    }

    /**
     * Where to pass the dragon?
     */
    private fun dragonPass(): DragonPass =
        DragonPass.LEFT

    override fun gameEnded(result: TaiPanGameResult) {
    }

}
