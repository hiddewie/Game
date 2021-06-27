package nl.hiddewieringa.game.taipan.player

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import nl.hiddewieringa.game.core.Player
import nl.hiddewieringa.game.core.TwoTeamPlayerId
import nl.hiddewieringa.game.taipan.*
import nl.hiddewieringa.game.taipan.card.*
import nl.hiddewieringa.game.taipan.state.TaiPanPlayerState
import nl.hiddewieringa.game.taipan.state.TaiPanPlayerStateType

class SimpleTaiPanPlayer : Player<TaiPanGameParameters, TaiPanEvent, TaiPanPlayerActions, TwoTeamPlayerId, TaiPanPlayerState> {

    private var requestedCards = false
    private var exchanged = false

    override fun play(parameters: TaiPanGameParameters, playerId: TwoTeamPlayerId, initialState: TaiPanPlayerState, events: ReceiveChannel<Pair<TaiPanEvent, TaiPanPlayerState>>): suspend ProducerScope<TaiPanPlayerActions>.() -> Unit =
        {
            events.consumeEach { (event, state) ->
                when (state.stateType) {
                    TaiPanPlayerStateType.RECEIVE_CARDS -> {
                        if (state.playersToPlay.contains(playerId) && !requestedCards) {
                            requestedCards = true
                            send(RequestNextCards)
                        }
                    }
                    TaiPanPlayerStateType.EXCHANGE_CARDS -> {
                        if (state.playersToPlay.contains(playerId) && !exchanged) {
                            exchanged = true
                            val cards = state.cards.sorted()
                            send(CardPass(ThreeWayPass(cards[0], cards[1], cards[cards.size - 1])))
                        }
                    }
                    TaiPanPlayerStateType.PLAY -> {
                        if (state.playersToPlay.contains(playerId)) {
                            if (state.lastPlayedCards == null) {
                                send(PlayCards(setOf(state.cards.minOrNull()!!)))
                            } else if (state.lastPlayedCards.second is HighCard) {
                                val cards = state.cards.filter { it is Phoenix || it is Dragon || it is NumberedCard && it.value > (state.lastPlayedCards.second as HighCard).value }
                                if (cards.isNotEmpty()) {
                                    send(PlayCards(setOf(cards.first())))
                                } else {
                                    send(Fold)
                                }
                            } else {
                                send(Fold)
                            }
                        }
                    }
                    TaiPanPlayerStateType.PASS_DRAGON -> {
                        if (state.playersToPlay.contains(playerId)) {
                            send(PassDragonTrick(DragonPass.LEFT))
                        }
                    }
                    TaiPanPlayerStateType.GAME_FINISHED -> {
                    }
                }
            }
        }
}
