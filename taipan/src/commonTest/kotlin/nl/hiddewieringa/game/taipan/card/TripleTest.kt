package nl.hiddewieringa.game.taipan.card

import nl.hiddewieringa.game.taipan.card.Suit.*
import kotlin.test.*

class TripleTest {
    @Test
    fun validNumberedTriple() {
        assertNotNull(NumberedTriple(10 of DIAMONDS, 10 of HEARTS, 10 of CLUBS))
    }

    @Test
    fun invalidNumberedTriple() {
        assertFailsWith<IllegalArgumentException> { NumberedTriple(10 of DIAMONDS, 8 of HEARTS, 10 of HEARTS) }
        assertFailsWith<IllegalArgumentException> { NumberedTriple(10 of DIAMONDS, 10 of DIAMONDS, 10 of HEARTS) }
    }

    @Test
    fun validPhoenixTriple() {
        assertNotNull(PhoenixTriple(10 of DIAMONDS, 10 of HEARTS, Phoenix))
    }

    @Test
    fun invalidPhoenixTriple() {
        assertFailsWith<IllegalArgumentException> { NumberedTriple(10 of DIAMONDS, 10 of DIAMONDS, 10 of HEARTS) }
    }
}
