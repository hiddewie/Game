package nl.hiddewieringa.game.taipan.card

import nl.hiddewieringa.game.taipan.card.Suit.HEARTS
import kotlin.test.*

class HighCardTest {

    @Test
    fun testValidCards() {
        assertNotNull(HighCard(Dog))
        assertNotNull(HighCard(Mahjong))
        assertNotNull(HighCard(Dragon))
        assertNotNull(HighCard(Phoenix, 10.5f))
        assertNotNull(HighCard(10 of HEARTS))
    }
}
