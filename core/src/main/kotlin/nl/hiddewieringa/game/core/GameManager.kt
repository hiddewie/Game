package nl.hiddewieringa.game.core

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce

// interface SS <PID : PlayerId, A : PlayerActions, E : Event> :  State, GameState<PID, A, E>

class StatefulJob<S : State<S>>(
    private val job: Deferred<S>,
    val stateSupplier: () -> S
) : Deferred<S> by job

class GameManager {

    // TODO maybe refactor factories to simple parameter passing?
    suspend fun <M : GameParameters, P : Player<M, E, A, PID, S>, A : PlayerActions, E : Event, PID : PlayerId, PC : PlayerConfiguration<PID, P>, S : State<S>>
    play(
//        gameFactory: (M) -> Game<M, P, A, E, R, PID, PC, S>,
        gameStateFactory: (M) -> S,
        playerFactory: () -> PC,
        parameters: M
    ): StatefulJob<S> {
        val players = playerFactory()
//        val game = gameFactory(parameters)

        return coroutineScope {
            // TODO why unlimited capacity? Would be better to meet-and-greet for delivering events and actions (?)
            val gameReceiveChannel = Channel<Pair<PID, A>>(capacity = UNLIMITED)
            val gameSendChannel = Channel<Triple<PID, E, S>>(capacity = UNLIMITED)
            val gameSendAllChannel = Channel<Pair<E, S>>(capacity = UNLIMITED)

            val playerChannels = players.allPlayers
                .map { playerId -> playerId to Channel<Pair<E, S>>(capacity = UNLIMITED) }
                .toMap()

            launch {
                gameSendAllChannel.consumeEach { event ->
                    players.forEach { playerId ->
                        playerChannels.getValue(playerId).send(event)
                    }
                }
            }

            launch {
                gameSendChannel.consumeEach {
                    playerChannels.getValue(it.first).send(it.second to it.third)
                }
            }

            // Create the game context, the stateful wrapper that encapsulates all game interaction and state
            val context = GameContext(players, gameStateFactory(parameters), gameSendAllChannel, gameSendChannel, gameReceiveChannel)

            players.forEach { playerId ->
                val playerProducer = players.player(playerId).initialize(parameters, playerId, context.state, playerChannels.getValue(playerId))
                val playerSendChannel = produce(coroutineContext, UNLIMITED, playerProducer)
                launch {
                    playerSendChannel.consumeEach { gameReceiveChannel.send(playerId to it) }
                }
            }

            // Play the game, wait for the result
            val job = async {
                val result = context.playGame()
                coroutineContext.cancelChildren()
                result
            }

            StatefulJob(job, context::state)
        }
    }
}
