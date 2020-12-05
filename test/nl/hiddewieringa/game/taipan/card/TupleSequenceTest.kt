package nl.hiddewieringa.game.taipan.card

import nl.hiddewieringa.game.taipan.card.Suit.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
        assertThrows<IllegalArgumentException> {
            TupleSequence(
                listOf(
                    NumberedTuple(10 of DIAMONDS, 10 of HEARTS),
                    NumberedTuple(8 of DIAMONDS, 8 of HEARTS),
                    NumberedTuple(9 of DIAMONDS, 9 of HEARTS),
                )
            )
        }
        assertThrows<IllegalArgumentException> {
            TupleSequence(
                listOf(
                    NumberedTuple(10 of DIAMONDS, 10 of HEARTS),
                )
            )
        }
        assertThrows<IllegalArgumentException> {
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
        assertThrows<IllegalArgumentException> {
            TupleSequence(
                listOf(
                    PhoenixTuple(9 of DIAMONDS, Phoenix),
                    PhoenixTuple(10 of DIAMONDS, Phoenix),
                )
            )
        }
    }
}