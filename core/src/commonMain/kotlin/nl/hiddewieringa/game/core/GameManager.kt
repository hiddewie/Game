package nl.hiddewieringa.game.core

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce

/**
 * A job that maintains an observable state while it is running
 */
class StatefulJob<S : GameState<S>>(
    private val job: Deferred<S>,
    val stateSupplier: () -> S,
) : Deferred<S> by job

/**
 * Executor for a game with a set of players.
 * Creates a game context that holds state.
 * Returns the (stateful) running game job that can be cancelled.
 */
class GameManager {

    suspend fun <M : GameParameters, P : Player<M, E, A, PID, PS>, A : PlayerActions, E : Event, PID : PlayerId, PC : PlayerConfiguration<PID, P>, S : GameState<S>, PS>
    play(
        scope: CoroutineScope,
        gameStateFactory: (M) -> S,
        playerFactory: () -> PC,
        parameters: M,
        playerState: S.(PID) -> PS,
    ): StatefulJob<S> {
        val players = playerFactory()

        // TODO why unlimited capacity? Would be better to meet-and-greet for delivering events and actions (?)
        val gameReceiveChannel = Channel<Pair<PID, A>>(capacity = UNLIMITED)
        val gameSendChannel = Channel<Triple<PID, E, PS>>(capacity = UNLIMITED)

        val playerChannels = players.allPlayers
            .map { playerId -> playerId to Channel<Pair<E, PS>>(capacity = UNLIMITED) }
            .toMap()

        scope.launch {
            gameSendChannel.consumeEach {
                playerChannels.getValue(it.first).send(it.second to it.third)
            }
        }

        // Create the game context, the stateful wrapper that encapsulates all game interaction and state
        val context = GameContext(players, gameStateFactory(parameters), gameSendChannel, gameReceiveChannel, playerState)

        players.forEach { playerId ->
            val playerProducer = players.player(playerId).play(parameters, playerId, context.state.playerState(playerId), playerChannels.getValue(playerId))
            val playerSendChannel = scope.produce(scope.coroutineContext, UNLIMITED, playerProducer)
            scope.launch {
                playerSendChannel.consumeEach { gameReceiveChannel.send(playerId to it) }
            }
        }

        // Play the game, wait for the result
        val job = scope.async {
            val result = context.playGame()
            println("Game has result $result")
            result
        }

        job.invokeOnCompletion {
            println("Canceled children")
            scope.coroutineContext.cancelChildren()
        }

        return StatefulJob(job, context::state)
    }
}