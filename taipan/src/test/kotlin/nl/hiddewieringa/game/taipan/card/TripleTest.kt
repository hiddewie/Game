package nl.hiddewieringa.game.taipan.card

import nl.hiddewieringa.game.taipan.card.Suit.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TripleTest {
    @Test
    fun validNumberedTriple() {
        assertNotNull(NumberedTriple(10 of DIAMONDS, 10 of HEARTS, 10 of CLUBS))
    }

    @Test
    fun invalidNumberedTriple() {
        assertThrows<IllegalArgumentException> { NumberedTriple(10 of DIAMONDS, 8 of HEARTS, 10 of HEARTS) }
        assertThrows<IllegalArgumentException> { NumberedTriple(10 of DIAMONDS, 10 of DIAMONDS, 10 of HEARTS) }
    }

    @Test
    fun validPhoenixTriple() {
        assertNotNull(PhoenixTriple(10 of DIAMONDS, 10 of HEARTS, Phoenix))
    }

    @Test
    fun invalidPhoenixTriple() {
        assertThrows<IllegalArgumentException> { NumberedTriple(10 of DIAMONDS, 10 of DIAMONDS, 10 of HEARTS) }
    }
}