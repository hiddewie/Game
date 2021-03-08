package nl.hiddewieringa.game.taipan.player

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import nl.hiddewieringa.game.core.Player
import nl.hiddewieringa.game.core.TwoTeamPlayerId
import nl.hiddewieringa.game.taipan.*
import nl.hiddewieringa.game.taipan.TaiPanGameParameters
import nl.hiddewieringa.game.taipan.card.*

class SimpleTaiPanPlayer : Player<TaiPanGameParameters, TaiPanEvent, TaiPanPlayerActions, TwoTeamPlayerId, TaiPanState> {

    private lateinit var hand: CardSet
    private var lastPlayedCards: CardCombination? = null

    override fun play(parameters: TaiPanGameParameters, playerId: TwoTeamPlayerId, initialState: TaiPanState, events: ReceiveChannel<Pair<TaiPanEvent, TaiPanState>>): suspend ProducerScope<TaiPanPlayerActions>.() -> Unit =
        {
            events.consumeEach { (event, _) ->
                when (event) {
                    is CardsHaveBeenDealt -> {
                        hand = event.cards
                    }
                    is CardsHaveBeenExchanged -> {
                    }
                    is PlayerPlayedCards -> {
                        lastPlayedCards = event.cards
                    }
                    is PlayerFolds -> {
                    }
                    is TrickWon -> {
                    }
                    is RoundEnded -> {
                    }
                    is PlayerTaiPanned -> {
                    }
                    is MahjongWishRequested -> {
                    }
                    MahjongWishFulfilled -> {
                    }
                    is PlayerPassedDragon -> {
                    }
                    is GameEnded -> {
                    }
                    AllPlayersHaveReceivedCards -> {}
                    AllPlayersHaveExchangedCards -> {}
                    is DragonTrickWon -> {}
                    is PlayerIsOutOfCards -> {}
                    is IllegalAction -> {}
                }
            }
        }

    private fun passCards(): CardPass {
        val ordered = hand.toList()
        return CardPass(ThreeWayPass(ordered[0], ordered[1], ordered[2]))
    }

    /**
     * Folds in case of null
     */
    private fun play(): CardSet? {
        // find first valid card combination to play
        return when (val stack = lastPlayedCards) {
            is HighCard ->
                hand.firstOrNull {
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
}
