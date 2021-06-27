package nl.hiddewieringa.game.tictactoe

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.hiddewieringa.game.core.TwoPlayerId
import nl.hiddewieringa.game.tictactoe.state.TicTacToePlayerState
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializationTest {

    @Test
    fun state() {
        val json = Json { }
        val serialized = json.encodeToString(
            TicTacToePlayerState(
                TwoPlayerId.PLAYER1,
                arrayOf(arrayOf(Circle, Cross, null), arrayOfNulls(3), arrayOfNulls(3)),
                TwoPlayerId.PLAYER2,
                false
            )
        )
        assertEquals("""{"playerToPlay":"PLAYER1","board":[[{"type":"nl.hiddewieringa.game.tictactoe.Circle"},{"type":"nl.hiddewieringa.game.tictactoe.Cross"},null],[null,null,null],[null,null,null]],"playerWon":"PLAYER2","gameFinished":false}""", serialized)
    }
}
