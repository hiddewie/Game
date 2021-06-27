package nl.hiddewieringa.game.server

import kotlinx.serialization.json.Json
import nl.hiddewieringa.game.server.controller.WrappedAction
import nl.hiddewieringa.game.tictactoe.Location
import nl.hiddewieringa.game.tictactoe.PlaceMarkLocation
import nl.hiddewieringa.game.tictactoe.TicTacToePlayerActions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SerializationTest {

    @Test
    fun wrappedAction() {
        val json = Json { }
        val deserialized = json.decodeFromString(WrappedAction.serializer(TicTacToePlayerActions.serializer()), """{"action":{"type":"nl.hiddewieringa.game.tictactoe.PlaceMarkLocation","location":{"x":0,"y":0}}}""")
        assertTrue(deserialized.action is PlaceMarkLocation)
        assertEquals(Location(0, 0), (deserialized.action as PlaceMarkLocation).location)
    }
}
