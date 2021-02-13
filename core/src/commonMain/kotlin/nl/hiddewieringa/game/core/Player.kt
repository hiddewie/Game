package nl.hiddewieringa.game.core

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Represents a player in a game.
 * Defines the behaviour of the player by reacting to game events.
 */
interface Player<
    M : GameParameters,
    E : Event,
    A : PlayerActions,
    PID : PlayerId,
    S : Any
    > {

    /**
     * Generate the player actions based on the game events and state channel.
     * This function will be called once by the game.
     */
    fun play(parameters: M, playerId: PID, initialState: S, events: ReceiveChannel<Pair<E, S>>): suspend ProducerScope<A>.() -> Unit
}

interface PlayerSpecific {
    val player: TwoTeamPlayerId
}
