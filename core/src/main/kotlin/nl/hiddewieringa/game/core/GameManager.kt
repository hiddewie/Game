package nl.hiddewieringa.game.core

import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class GameManager {

    // TODO maybe refactor factories to simple parameter passing?
    suspend fun <M : GameParameters, P : Player<M, E, S, A, R>, A : PlayerActions, E : Event, R : GameResult, PID : PlayerId, PC : PlayerConfiguration<PID, P>, S : GameState>
    play(
        gameFactory: (M) -> Game<M, P, A, E, R, PID, PC, S>,
        playerFactory: () -> PC,
        parameters: M
    ): R {
        val players = playerFactory()
        val game = gameFactory(parameters)

        return coroutineScope {
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

            players.forEach { playerId ->
                val playerProducer = players.player(playerId).initialize(parameters, game.state, playerChannels.getValue(playerId))
                val playerSendChannel = produce(coroutineContext, UNLIMITED, playerProducer)
                launch {
                    playerSendChannel.consumeEach { gameReceiveChannel.send(playerId to it) }
                }
            }

            val result = game.play(GameContext(players, { game.state }, gameSendAllChannel, gameSendChannel, gameReceiveChannel))

            // The game is done, players must exit
            players.forEach { players.player(it).gameEnded(result) }
            coroutineContext.cancelChildren()

            result
        }
    }
}
