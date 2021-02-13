package nl.hiddewieringa.game.taipan

import kotlinx.serialization.Serializable

@Serializable
enum class TaiPanStatus(
    val score: Int
) {
    NORMAL(100),
    GREAT(200)
}
