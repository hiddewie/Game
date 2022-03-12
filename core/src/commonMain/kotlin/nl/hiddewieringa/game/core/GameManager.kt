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
        playerFactory: () -> PC, // TODO remove function call
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

    fun <A : PlayerActions, E : Event, PID : PlayerId, S : GameState<S>> applyPlayerAction(gameState: S, playerId: PID, action: A, eventPublisher: (E, S) -> Unit): S {
        val gameStateAfterDecisions = applyDecisionActions(gameState, eventPublisher)

        if (gameStateAfterDecisions !is IntermediateGameState<*, *, *, *>) {
            println("Cannot process player action for finished game")
            return gameState
        }
        println("Game loop: Player $playerId played $action")

        val intermediateState = gameStateAfterDecisions as IntermediateGameState<PID, A, E, S>
        val event = intermediateState.processPlayerAction(playerId, action)

        println("Game loop: Action caused $event")
        val stateAfterAction = intermediateState.applyEvent(event)
        println("Game loop: Game loop: State update processed")

        eventPublisher(event, stateAfterAction)

        return applyDecisionActions(stateAfterAction, eventPublisher)
    }

    private fun <E : Event, S : GameState<S>> applyDecisionActions(gameState: S, eventPublisher: (E, S) -> Unit, depth: Int = 0): S {
        if (depth > MAX_DEPTH) {
            throw IllegalStateException("The maximum depth count $MAX_DEPTH exceeded.")
        }

        while (gameState is IntermediateGameState<*, *, *, *> && gameState.hasDecision()) {
            val decision = gameState.gameDecisions.first(GameDecision<out Event>::condition)
            println("Game loop: Decision $decision found")
            val decisionEvent = decision.event() as E
            println("Game loop: Decision generated event $decisionEvent")

            val stateAfterDecision = (gameState as IntermediateGameState<*, *, E, S>).applyEvent(decisionEvent)

            eventPublisher(decisionEvent, stateAfterDecision)

            return applyDecisionActions(stateAfterDecision, eventPublisher, depth + 1)
        }

        return gameState
    }

    companion object {
        private const val MAX_DEPTH = 100
    }
}
