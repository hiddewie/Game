package nl.hiddewieringa.game.core

import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Executor for a game with a set of players.
 * Creates a game context that holds state.
 * Returns the (stateful) running game job that can be cancelled.
 */
class GameManager {

    suspend fun <
        M : GameParameters,
        P : Player<M, E, A, PID, PS>,
        A : PlayerActions,
        E : Event,
        PID : PlayerId,
        PC : PlayerConfiguration<PID, P>,
        S : GameState<S>,
        PS
        > play(
        gameStateFactory: (M) -> S,
        playerFactory: () -> PC,
        parameters: M,
        playerState: S.(PID) -> PS,
        gameState: SendChannel<S>
    ): S =
        coroutineScope {
            val players = playerFactory()

            // Use unlimited capacity such that there is no blocking when there are no consumers
            val gameReceiveChannel = Channel<Pair<PID, A>>(capacity = UNLIMITED)
            val gameSendChannel = Channel<Triple<PID, E, PS>>(capacity = UNLIMITED)
            val playerChannels = players.allPlayers.associateWith { Channel<Pair<E, PS>>(capacity = UNLIMITED) }

            launch {
                gameSendChannel.consumeEach {
                    playerChannels.getValue(it.first).send(it.second to it.third)
                }
            }

            // Create the game context, the stateful wrapper that encapsulates all game interaction and state
            val context = GameContext(players, gameStateFactory(parameters), gameSendChannel, gameState, gameReceiveChannel, playerState)

            players.forEach { playerId ->
                val playerProducer = players.player(playerId).play(parameters, playerId, context.state.playerState(playerId), playerChannels.getValue(playerId))
                val playerSendChannel = produce(coroutineContext, UNLIMITED, playerProducer)
                launch {
                    playerSendChannel.consumeEach {
                        gameReceiveChannel.send(playerId to it)
                    }
                }
            }

            // Play the game, wait for the result
            println("Playing game")
            val result = context.playGame()
            println("Game has result $result")

            println("Closing game coroutines and channels")
            coroutineContext.cancelChildren()
            gameReceiveChannel.close()
            gameSendChannel.close()
            playerChannels.forEach { it.value.close() }
            println("Closed game coroutines and channels")

            result
        }
}
