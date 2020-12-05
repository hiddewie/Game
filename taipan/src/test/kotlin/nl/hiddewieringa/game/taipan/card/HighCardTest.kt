package nl.hiddewieringa.game.taipan.card

import nl.hiddewieringa.game.taipan.card.Suit.HEARTS
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class HighCardTest {

    @Test
    fun testValidCards() {
        assertNotNull(HighCard(Dog))
        assertNotNull(HighCard(Mahjong))
        assertNotNull(HighCard(Dragon))
        assertNotNull(HighCard(Phoenix))
        assertNotNull(HighCard(10 of HEARTS))
    }
}
