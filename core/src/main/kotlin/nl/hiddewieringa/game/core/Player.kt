package nl.hiddewieringa.game.core

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel

interface Player<
    M : GameParameters,
    E : Event,
    A : PlayerActions,
    PID : PlayerId,
    S
    > {

    /**
     * Initialize resources
     *
     * TODO rename to play
     */
    fun initialize(parameters: M, playerId: PID, initialState: S, eventBus: ReceiveChannel<Pair<E, S>>): suspend ProducerScope<A>.() -> Unit
}

interface PlayerSpecific {
    val player: TwoTeamPlayerId
}
