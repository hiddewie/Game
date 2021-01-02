package nl.hiddewieringa.game.core

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.lang.IllegalStateException

/**
 * Interface for a game that can be played by certain players.
 *
 * This interface models the interaction of the players with the game state, and implements the game logic by reacting to player actions.
 * The players actions cause game events, which are processed applied to the game state to produce a new game state.
 */
// interface Game<
//        M : GameParameters,
//        P : Player<M, E, S, A, R>,
//        A : PlayerActions,
//        E : Event,
//        R : GameResult,
//        PID : PlayerId,
//        PC : PlayerConfiguration<PID, P>,
//        S : GameState<PID,A,E>> {
//
//    val initialState: GameState<PID, A, E>
//
//    suspend fun play(context: GameContext<A, E, PID, PC, S>): R
// }

class GameContext<A : PlayerActions, E : Event, PID : PlayerId, PC : PlayerConfiguration<PID, *>, S : State<S>>(
    // TODO remove
    val players: PC,
    initialState: S,
    private val sendAllChannel: SendChannel<Pair<E, S>>,
    private val playerChannel: SendChannel<Triple<PID, E, S>>,
    private val playerActions: ReceiveChannel<Pair<PID, A>>
) {

    var state = initialState

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

                        // TODO send events to players which are influenced by the new state!
                        sendAllChannel.send(decisionEvent to state)

                        println("Game loop: State update processed")
                    } else {
                        val (playerId, action) = playerActions.receive()
                        println("Game loop: Player $playerId played $action")
                        val event = (currentState as IntermediateGameState<PID, A, E, S>).processPlayerAction(playerId, action)

                        println("Game loop: Action caused $event")
                        state = (currentState as IntermediateGameState<PID, A, E, S>).applyEvent(event)

                        // TODO send events to players which are influenced by the new state!
                        sendAllChannel.send(event to state)

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

// TODO move to funcitonal tools file
sealed class Either<L, R> {
    class Left<L, R>(val left: L) : Either<L, R>()
    class Right<L, R>(val right: R) : Either<L, R>()
}

interface GameStage<PID : PlayerId, A : PlayerActions, E : Event> {

    fun processPlayerAction(playerId: PID, action: A): E
}

class GameDecision<E>(
    val condition: Boolean,
    val event: () -> E
)

interface IntermediateGameState<PID : PlayerId, A : PlayerActions, E : Event, S : State<S>> : GameStage<PID, A, E> {

    val gameDecisions: List<GameDecision<E>>

    fun applyEvent(event: E): S

    fun hasDecision(): Boolean =
        gameDecisions.any(GameDecision<E>::condition)
}

// TODO split into public state and player specific state
// typealias GameState<PID , A , E, S > = Either<
//        IntermediateGameState<PID , A , E, S >,
//        GameResult<S>
//        >

// TODO rename GameState
interface State<S : State<S>>

interface GameParameters

interface PlayerActions

// // TODO add game state and state for each player
// interface GameState<E, S: GameState<E,S>> {
//
//    fun with(event: E): S
//
// }
