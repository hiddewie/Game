package nl.hiddewieringa.game.taipan.card

import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.ACE
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.JACK
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.KING
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.QUEEN
import nl.hiddewieringa.game.taipan.card.Suit.HEARTS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PointsTest {
    @Test
    fun specialCards() {
        assertEquals(0, Dog.points)
        assertEquals(-25, Phoenix.points)
        assertEquals(25, Dragon.points)
        assertEquals(0, Mahjong.points)
    }

    @Test
    fun normalCards() {
        assertEquals(0, (2 of HEARTS).points)
        assertEquals(0, (3 of HEARTS).points)
        assertEquals(0, (4 of HEARTS).points)
        assertEquals(5, (5 of HEARTS).points)
        assertEquals(0, (6 of HEARTS).points)
        assertEquals(0, (7 of HEARTS).points)
        assertEquals(0, (8 of HEARTS).points)
        assertEquals(0, (9 of HEARTS).points)
        assertEquals(10, (10 of HEARTS).points)
        assertEquals(0, (JACK of HEARTS).points)
        assertEquals(0, (QUEEN of HEARTS).points)
        assertEquals(10, (KING of HEARTS).points)
        assertEquals(0, (ACE of HEARTS).points)
    }
}
