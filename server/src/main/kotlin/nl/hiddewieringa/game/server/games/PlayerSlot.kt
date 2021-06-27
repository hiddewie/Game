package nl.hiddewieringa.game.server.games

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.SharedFlow
import mu.KLogging
import mu.KotlinLogging
import nl.hiddewieringa.game.core.Event
import nl.hiddewieringa.game.core.PlayerActions
import nl.hiddewieringa.game.core.PlayerId
import java.util.concurrent.atomic.AtomicInteger

class PlayerSlot<A : PlayerActions, E : Event, S : Any, PID : PlayerId>(
    val playerId: PID,
    val sendChannel: SendChannel<A>,
    val receiveChannel: SharedFlow<Pair<E, S>>,
) {

    var referenceCount = AtomicInteger()

    fun increaseReference() {
        referenceCount.incrementAndGet()
        logger.info("player slot $playerId ${referenceCount.get()}")
    }

    fun decreaseReference() {
        referenceCount.decrementAndGet()
        logger.info("player slot $playerId ${referenceCount.get()}")
    }

    companion object : KLogging()
}