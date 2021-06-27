package nl.hiddewieringa.game.server.games

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import nl.hiddewieringa.game.core.*

class WebsocketPlayer<M : GameParameters, A : PlayerActions, E : Event, S : Any> : Player<M, E, A, PlayerId, S> {

    /**
     * Hot flow that can have multiple consumers. No replay: new consumers have to fetch the latest state manually, and will not receive past events.
     */
    val eventChannel = MutableSharedFlow<Pair<E, S>>(replay = 0)

    /**
     * Actions must be delivered exactly once, suspending until they are delivered.
     */
    val actionChannel = Channel<A>(capacity = 0)

    override fun play(parameters: M, playerId: PlayerId, initialState: S, events: ReceiveChannel<Pair<E, S>>): suspend ProducerScope<A>.() -> Unit =
        {
            launch {
                actionChannel.consumeEach {
                    send(it)
                }
            }
            launch {
                events.consumeEach { (event, state) ->
                    eventChannel.emit(event to state)
                }
            }
        }
}
