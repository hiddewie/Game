package nl.hiddewieringa.game.taipan

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.hiddewieringa.game.core.TwoTeamPlayerId
import nl.hiddewieringa.game.taipan.card.NumberedCard
import nl.hiddewieringa.game.taipan.card.Suit
import nl.hiddewieringa.game.taipan.state.toPlayerState
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializationTest {

    @Test
    fun state() {
        val json = Json { }
        val state = TaiPan(TaiPanGameParameters(0, 0))
            .toPlayerState(TwoTeamPlayerId.PLAYER1)
        val serialized = json.encodeToString(state)

        assertEquals(
            """{"stateType":"RECEIVE_CARDS","playersToPlay":["PLAYER1","PLAYER2","PLAYER3","PLAYER4"],"cards":[],"numberOfCardsPerPlayer":{"PLAYER1":0,"PLAYER2":0,"PLAYER3":0,"PLAYER4":0},"taiPannedPlayers":{},"playedCards":[],"points":{"TEAM1":0,"TEAM2":0},"trickCards":[],"lastPlayedCards":null,"mahjongWish":null,"roundCards":{},"roundIndex":null,"trickIndex":null,"outOfCardOrder":[]}
            """.trimIndent(),
            serialized
        )
    }

    @Test
    fun card() {
        val json = Json { }
        val serialized = json.encodeToString(NumberedCard(Suit.CLUBS, 3))
        assertEquals("""{"points":0,"suit":"CLUBS","value":3}""", serialized)
    }
}
