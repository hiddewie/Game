package nl.hiddewieringa.game.server.games

import nl.hiddewieringa.game.core.GameState
import nl.hiddewieringa.game.core.PlayerId
import java.util.*

// TODO store start timestamp
class GameInstance<S : GameState<S>, PID : PlayerId>(
    val id: UUID,
    val gameSlug: String,
    val state: S,
    val playerSlots: Map<UUID, PID>,
//    val stateProvider: suspend (PID) -> S,
) {
//    val open: Boolean
//        get() = playerSlots.values.any { it.referenceCount.get() == 0 }
//
//    suspend fun playerState(playerSlotId: UUID): S =
//        stateProvider(playerSlots.getValue(playerSlotId).playerId)
}
