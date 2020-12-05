package nl.hiddewieringa.game.core

import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class GameManager {

    suspend fun <M : GameParameters, P : Player<M, E, A, R>, A : PlayerActions, E : Event, R : GameResult, PID : PlayerId, PC : PlayerConfiguration<PID, P>>
            play(
        gameFactory: (M) -> Game<M, P, A, E, R, PID, PC>,
        playerFactory: () -> PC,
        parameters: M
    ): R {
        val players = playerFactory()
        val game = gameFactory(parameters)

        return coroutineScope {
            val gameReceiveChannel = Channel<Pair<PID, A>>(capacity = UNLIMITED)
            val gameSendChannel = Channel<Pair<PID, E>>(capacity = UNLIMITED)
            val gameSendAllChannel = Channel<E>(capacity = UNLIMITED)

            val playerChannels = players.allPlayers
                .map { playerId -> playerId to Channel<E>(capacity = UNLIMITED) }
                .toMap()

            launch {
                gameSendAllChannel.consumeEach { event ->
                    players.allPlayers.forEach { playerId ->
                        playerChannels.getValue(playerId).send(event)
                    }
                }
            }

            launch {
                gameSendChannel.consumeEach {
                    playerChannels.getValue(it.first).send(it.second)
                }
            }

            players.allPlayers.forEach { playerId ->
                val playerProducer = players.player(playerId).initialize(parameters, playerChannels.getValue(playerId))
                val playerSendChannel = produce(coroutineContext, UNLIMITED, playerProducer)
                launch {
                    playerSendChannel.consumeEach { gameReceiveChannel.send(playerId to it) }
                }
            }

            val result = game.play(GameContext(players, gameSendAllChannel, gameSendChannel, gameReceiveChannel))

            // The game is done, players must exit
            players.allPlayers.forEach { players.player(it).gameEnded(result) }
            coroutineContext.cancelChildren()

            result
        }
    }

}