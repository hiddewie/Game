package nl.hiddewieringa.game.core

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

/**
 * Interface for a game that can be played by certain players.
 *
 * This interface models the interaction of the players with the game state, and implements the game logic by reacting to player actions.
 * The players actions cause game events, which are processed applied to the game state to produce a new game state.
 */
interface Game<
    M : GameParameters,
    P : Player<M, E, S, A, R>,
    A : PlayerActions,
    E : Event,
    R : GameResult,
    PID : PlayerId,
    PC : PlayerConfiguration<PID, P>,
    S : GameState> {

    val state: S

    suspend fun play(context: GameContext<A, E, PID, PC, S>): R
}

class GameContext<A : PlayerActions, E : Event, PID : PlayerId, PC : PlayerConfiguration<PID, *>, S : GameState>(
    val players: PC,
    val state: () -> S,
    private val sendAllChannel: SendChannel<Pair<E, S>>,
    private val playerChannel: SendChannel<Triple<PID, E, S>>,
    private val playerActions: ReceiveChannel<Pair<PID, A>>
) {
    suspend fun sendToAllPlayers(event: E) {
        sendAllChannel.send(event to state())
    }

    suspend fun sendToPlayer(playerId: PID, event: E) {
        // TODO make player state
        playerChannel.send(Triple(playerId, event, state()))
    }

    suspend fun receiveFromPlayer(): Pair<PID, A> =
        playerActions.receive()
}

interface GameResult

interface GameParameters

interface PlayerActions

// TODO add apply method that applies a single event
// TODO add game state and state for each player
interface GameState
