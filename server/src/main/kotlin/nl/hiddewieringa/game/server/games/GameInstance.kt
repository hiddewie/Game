package nl.hiddewieringa.game.server.games

import kotlinx.serialization.KSerializer
import nl.hiddewieringa.game.core.Event
import nl.hiddewieringa.game.core.PlayerActions
import nl.hiddewieringa.game.core.PlayerId
import java.util.*

// TODO store start timestamp
class GameInstance<A : PlayerActions, E : Event, S : Any, PID : PlayerId>(
    val id: UUID,
    val gameSlug: String,
    val playerSlots: Map<UUID, PlayerSlot<A, E, S, PID>>,
    val stateProvider: suspend (PID) -> S,
    val actionSerializer: KSerializer<A>,
    val eventSerializer: KSerializer<E>,
    val stateSerializer: KSerializer<S>,
    val playerIdSerializer: KSerializer<PID>,
) {
    val open: Boolean
        get() = playerSlots.values.any { it.referenceCount.get() == 0 }

    suspend fun playerState(playerSlotId: UUID): S =
        stateProvider(playerSlots.getValue(playerSlotId).playerId)
}