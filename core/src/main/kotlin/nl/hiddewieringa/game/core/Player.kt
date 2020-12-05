package nl.hiddewieringa.game.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel


interface Player<
        M : GameParameters,
        E : Event,
        A : PlayerActions,
        R : GameResult,
        > {

    /**
     * Initialize resources
     *
     * TODO rename to play
     */
    fun initialize(parameters: M, eventBus: ReceiveChannel<E>): suspend ProducerScope<A>.() -> Unit

    // Any other player actions

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