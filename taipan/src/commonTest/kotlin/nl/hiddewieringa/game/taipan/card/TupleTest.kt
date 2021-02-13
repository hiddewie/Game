package nl.hiddewieringa.game.taipan.card

import nl.hiddewieringa.game.taipan.card.Suit.DIAMONDS
import nl.hiddewieringa.game.taipan.card.Suit.HEARTS
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class TupleTest {
    @Test
    fun validNumberedTuple() {
        assertNotNull(NumberedTuple(10 of DIAMONDS, 10 of HEARTS))
    }

    @Test
    fun invalidNumberedTuple() {
        assertFailsWith<IllegalArgumentException> { NumberedTuple(10 of DIAMONDS, 8 of HEARTS) }
        assertFailsWith<IllegalArgumentException> { NumberedTuple(10 of DIAMONDS, 10 of DIAMONDS) }
    }

    @Test
    fun validPhoenixTuple() {
        assertNotNull(PhoenixTuple(10 of DIAMONDS, Phoenix))
    }
}
