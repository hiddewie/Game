package nl.hiddewieringa.game.core

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.lang.IllegalStateException

/**
 * A stateful wrapper around game state, combined with the player interaction.
 * When the `playGame` method is called, the state will be updated in a loop, until a terminal state is reached.
 * Every event is published to all players, along with the projected game state.
 */
class GameContext<A : PlayerActions, E : Event, PID : PlayerId, S : GameState<S>, PS>(
    private val playerIds: Set<PID>,
    initialState: S,
    private val playerChannel: SendChannel<Triple<PID, E, PS>>,
    private val playerActions: ReceiveChannel<Pair<PID, A>>,
    private val playerState: (S) -> PS,
) {

    var state: S = initialState

    suspend fun playGame(): S {
        println("Starting game loop")
        var loop = 0
        while (loop < 1000) {
            println("Game loop: state $state")
            when (val currentState = state) {

                // Intermediate branch
                is IntermediateGameState<*, *, *, *> ->
                    if (currentState.hasDecision()) {
                        val decision = currentState.gameDecisions.first(GameDecision<out Event>::condition) as GameDecision<E>
                        println("Game loop: Decision $decision found")
                        val decisionEvent = decision.event()
                        println("Game loop: Decision generated event $decisionEvent")

                        state = (currentState as IntermediateGameState<PID, A, E, S>).applyEvent(decisionEvent)

                        playerIds.forEach { playerId ->
                            playerChannel.send(Triple(playerId, decisionEvent, playerState(state)))
                        }

                        println("Game loop: State update processed")
                    } else {
                        println("Game loop: Receiving player action")
                        // TODO add timing checks (withTimeout)
                        val (playerId, action) = playerActions.receive()
                        println("Game loop: Player $playerId played $action")
                        val event = (currentState as IntermediateGameState<PID, A, E, S>).processPlayerAction(playerId, action)

                        println("Game loop: Action caused $event")
                        state = (currentState as IntermediateGameState<PID, A, E, S>).applyEvent(event)

                        playerIds.forEach { playerId ->
                            // TODO filter sensitive event contents
                            playerChannel.send(Triple(playerId, event, playerState(state)))
                        }

                        println("Game loop: State update processed")
                    }

                // Terminating branch
                else -> {
                    println("Game loop: Terminating state $currentState")
                    return currentState
                }
            }
            loop ++
        }

        throw IllegalStateException("The loop count exceeded the maximum value $loop")
    }
}

interface GameStage<PID : PlayerId, A : PlayerActions, E : Event> {

    fun processPlayerAction(playerId: PID, action: A): E
}

class GameDecision<E>(
    val condition: Boolean,
    val event: () -> E
)

interface IntermediateGameState<PID : PlayerId, A : PlayerActions, E : Event, S : GameState<S>> : GameStage<PID, A, E> {

    val gameDecisions: List<GameDecision<E>>

    fun applyEvent(event: E): S

    fun hasDecision(): Boolean =
        gameDecisions.any(GameDecision<E>::condition)
}

interface GameState<S : GameState<S>>

interface GameParameters

interface PlayerActions
