package nl.hiddewieringa.game.taipan.card

import nl.hiddewieringa.game.taipan.card.Suit.*
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class TupleSequenceTest {
    @Test
    fun validNumberedTupleSequence() {
        TupleSequence(
            listOf(
                NumberedTuple(8 of DIAMONDS, 8 of HEARTS),
                NumberedTuple(9 of DIAMONDS, 9 of HEARTS),
                NumberedTuple(10 of DIAMONDS, 10 of HEARTS),
            )
        )
        TupleSequence(
            listOf(
                NumberedTuple(9 of DIAMONDS, 9 of HEARTS),
                NumberedTuple(10 of DIAMONDS, 10 of HEARTS),
            )
        )
    }

    @Test
    fun invalidNumberedTupleSequence() {
        assertFailsWith<IllegalArgumentException> {
            TupleSequence(
                listOf(
                    NumberedTuple(10 of DIAMONDS, 10 of HEARTS),
                    NumberedTuple(8 of DIAMONDS, 8 of HEARTS),
                    NumberedTuple(9 of DIAMONDS, 9 of HEARTS),
                )
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TupleSequence(
                listOf(
                    NumberedTuple(10 of DIAMONDS, 10 of HEARTS),
                )
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TupleSequence(
                listOf(
                    NumberedTuple(10 of DIAMONDS, 10 of SPADES),
                    NumberedTuple(10 of DIAMONDS, 10 of HEARTS),
                )
            )
        }
    }

    @Test
    fun validPhoenixTupleSequence() {
        assertNotNull(
            TupleSequence(
                listOf(
                    PhoenixTuple(9 of DIAMONDS, Phoenix),
                    NumberedTuple(10 of DIAMONDS, 10 of HEARTS),
                )
            )
        )
    }

    @Test
    fun invalidPhoenixTupleSequence() {
        assertFailsWith<IllegalArgumentException> {
            TupleSequence(
                listOf(
                    PhoenixTuple(9 of DIAMONDS, Phoenix),
                    PhoenixTuple(10 of DIAMONDS, Phoenix),
                )
            )
        }
    }
}
