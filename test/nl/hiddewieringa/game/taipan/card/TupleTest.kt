package nl.hiddewieringa.game.taipan.card

import nl.hiddewieringa.game.taipan.card.Suit.DIAMONDS
import nl.hiddewieringa.game.taipan.card.Suit.HEARTS
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TupleTest {
    @Test
    fun validNumberedTuple() {
        assertNotNull(NumberedTuple(10 of DIAMONDS, 10 of HEARTS))
    }

    @Test
    fun invalidNumberedTuple() {
        assertThrows<IllegalArgumentException> { NumberedTuple(10 of DIAMONDS, 8 of HEARTS) }
        assertThrows<IllegalArgumentException> { NumberedTuple(10 of DIAMONDS, 10 of DIAMONDS) }
    }

    @Test
    fun validPhoenixTuple() {
        assertNotNull(PhoenixTuple(10 of DIAMONDS, Phoenix))
    }
}