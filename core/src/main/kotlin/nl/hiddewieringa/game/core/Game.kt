package nl.hiddewieringa.game.core

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

/**
 * Interface for a game that can be played by certain players.
 */
interface Game<
    M : GameParameters,
    P : Player<M, E, A, R>,
    A : PlayerActions,
    E : Event,
    R : GameResult,
    PID : PlayerId,
    PC : PlayerConfiguration<PID, P>> {
    suspend fun play(context: GameContext<A, E, PID, PC>): R
}

class GameContext<A : PlayerActions, E : Event, PID : PlayerId, PC : PlayerConfiguration<PID, *>>(
    val players: PC,
    private val sendAllChannel: SendChannel<E>,
    private val playerChannel: SendChannel<Pair<PID, E>>,
    private val playerActions: ReceiveChannel<Pair<PID, A>>
) {
    suspend fun sendToAllPlayers(event: E) {
        sendAllChannel.send(event)
    }

    suspend fun sendToPlayer(playerId: PID, event: E) {
        playerChannel.send(playerId to event)
    }

    suspend fun receiveFromPlayer(): Pair<PID, A> =
        playerActions.receive()
}

interface GameResult

interface GameParameters

interface PlayerActions
