package nl.hiddewieringa.game.taipan.player

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import nl.hiddewieringa.game.core.Player
import nl.hiddewieringa.game.core.TwoTeamPlayerId
import nl.hiddewieringa.game.taipan.*
import nl.hiddewieringa.game.taipan.card.*
import nl.hiddewieringa.game.taipan.state.TaiPanPlayerState
import nl.hiddewieringa.game.taipan.state.TaiPanPlayerStateType
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@OptIn(ExperimentalTime::class)
@FlowPreview
class SimpleTaiPanPlayer : Player<TaiPanGameParameters, TaiPanEvent, TaiPanPlayerActions, TwoTeamPlayerId, TaiPanPlayerState> {

    private var exchanged = false

    override fun play(parameters: TaiPanGameParameters, playerId: TwoTeamPlayerId, initialState: TaiPanPlayerState, events: ReceiveChannel<Pair<TaiPanEvent, TaiPanPlayerState>>): suspend ProducerScope<TaiPanPlayerActions>.() -> Unit =
        {
            events.consumeAsFlow()
                .debounce(Duration.milliseconds(500))
                .collect { (_, state) -> handleEvent(playerId, state) }
        }

    private suspend fun ProducerScope<TaiPanPlayerActions>.handleEvent(playerId: TwoTeamPlayerId, state: TaiPanPlayerState) {
        when (state.stateType) {
            TaiPanPlayerStateType.RECEIVE_CARDS -> {
                if (state.playersToPlay.contains(playerId) && state.cards.size < 14) {
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
                    } else if (state.lastPlayedCards.second is HighCard && !state.lastPlayedCards.second.contains(Dragon)) {
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
