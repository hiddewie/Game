package nl.hiddewieringa.game.core

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel

interface Player<
    M : GameParameters,
    E : Event,
    S : GameState,
    A : PlayerActions,
    R : GameResult,
    > {

    /**
     * Initialize resources
     *
     * TODO rename to play
     */
    fun initialize(parameters: M, initialState: S, eventBus: ReceiveChannel<Pair<E, S>>): suspend ProducerScope<A>.() -> Unit

    /**
     * Clean up resources
     *
     * TODO make game event
     */
    fun gameEnded(result: R)
}

interface PlayerSpecific {
    val player: TwoTeamPlayerId
}
