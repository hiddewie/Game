package nl.hiddewieringa.game.taipan.card

import nl.hiddewieringa.game.taipan.PhoenixValue
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.ACE
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.JACK
import nl.hiddewieringa.game.taipan.card.Suit.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CardCombinationTest {

    @Test
    fun specialHighCards() {
        assertEquals(HighCard(Dragon), findCardCombination(null, setOf(Dragon), emptySet()))
        assertEquals(HighCard(Phoenix, 1.5f), findCardCombination(null, setOf(Phoenix), emptySet()))
        assertEquals(HighCard(Phoenix, 10.5f), findCardCombination(HighCard(10 of SPADES), setOf(Phoenix), emptySet()))
        assertEquals(HighCard(Dog), findCardCombination(null, setOf(Dog), emptySet()))
        assertEquals(HighCard(Mahjong), findCardCombination(null, setOf(Mahjong), emptySet()))
    }

    @Test
    fun numberedHighCards() {
        (2..ACE).forEach { value ->
            assertEquals(HighCard(value of HEARTS), findCardCombination(null, setOf(value of HEARTS), emptySet()))
        }
    }

    @Test
    fun tuples() {
        assertEquals(
            NumberedTuple(10 of HEARTS, 10 of DIAMONDS),
            findCardCombination(null, setOf(10 of HEARTS, 10 of DIAMONDS), emptySet())
        )
        assertEquals(
            PhoenixTuple(10 of HEARTS, Phoenix),
            findCardCombination(null, setOf(10 of HEARTS, Phoenix), emptySet())
        )
    }

    @Test
    fun triples() {
        assertEquals(
            NumberedTriple(10 of HEARTS, 10 of DIAMONDS, 10 of SPADES),
            findCardCombination(null, setOf(10 of HEARTS, 10 of DIAMONDS, 10 of SPADES), emptySet())
        )
        assertEquals(
            PhoenixTriple(10 of HEARTS, 10 of DIAMONDS, Phoenix),
            findCardCombination(null, setOf(10 of HEARTS, 10 of DIAMONDS, Phoenix), emptySet())
        )
    }

    @Test
    fun tupleSequences() {
        assertEquals(
            TupleSequence(listOf(NumberedTuple(9 of HEARTS, 9 of DIAMONDS), NumberedTuple(10 of HEARTS, 10 of DIAMONDS))),
            findCardCombination(null, setOf(9 of HEARTS, 9 of DIAMONDS, 10 of HEARTS, 10 of DIAMONDS), emptySet())
        )
        assertEquals(
            TupleSequence(
                listOf(
                    NumberedTuple(9 of HEARTS, 9 of DIAMONDS),
                    NumberedTuple(10 of HEARTS, 10 of DIAMONDS),
                    NumberedTuple(JACK of HEARTS, JACK of DIAMONDS),
                )
            ),
            findCardCombination(null, setOf(9 of HEARTS, 9 of DIAMONDS, 10 of HEARTS, 10 of DIAMONDS, JACK of HEARTS, JACK of DIAMONDS), emptySet())
        )
        assertEquals(
            TupleSequence(listOf(NumberedTuple(9 of HEARTS, 9 of DIAMONDS), PhoenixTuple(10 of HEARTS, Phoenix))),
            findCardCombination(null, setOf(9 of HEARTS, 9 of DIAMONDS, 10 of HEARTS, Phoenix), emptySet())
        )
    }

    @Test
    fun bombs() {
        assertEquals(
            QuadrupleBomb(setOf(9 of HEARTS, 9 of DIAMONDS, 9 of SPADES, 9 of CLUBS)),
            findCardCombination(null, setOf(9 of HEARTS, 9 of DIAMONDS, 9 of SPADES, 9 of CLUBS), emptySet())
        )
        assertEquals(
            StraightBomb(setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of HEARTS)),
            findCardCombination(null, setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of HEARTS), emptySet())
        )
    }

    @Test
    fun fullHouses() {
        assertEquals(
            FullHouse(NumberedTuple(9 of HEARTS, 9 of DIAMONDS), NumberedTriple(8 of HEARTS, 8 of DIAMONDS, 8 of CLUBS)),
            findCardCombination(null, setOf(9 of HEARTS, 9 of DIAMONDS, 8 of HEARTS, 8 of DIAMONDS, 8 of CLUBS), emptySet())
        )
        assertEquals(
            FullHouse(PhoenixTuple(9 of HEARTS, Phoenix), NumberedTriple(8 of HEARTS, 8 of DIAMONDS, 8 of CLUBS)),
            findCardCombination(null, setOf(9 of HEARTS, 8 of HEARTS, 8 of DIAMONDS, 8 of CLUBS, Phoenix), emptySet())
        )
        assertEquals(
            FullHouse(NumberedTuple(9 of HEARTS, 9 of DIAMONDS), PhoenixTriple(8 of HEARTS, 8 of DIAMONDS, Phoenix)),
            findCardCombination(null, setOf(9 of HEARTS, 9 of DIAMONDS, 8 of HEARTS, 8 of DIAMONDS, Phoenix), setOf(PhoenixValue(8)))
        )
        assertEquals(
            FullHouse(NumberedTuple(8 of HEARTS, 8 of DIAMONDS), PhoenixTriple(9 of HEARTS, 9 of DIAMONDS, Phoenix)),
            findCardCombination(null, setOf(9 of HEARTS, 9 of DIAMONDS, 8 of HEARTS, 8 of DIAMONDS, Phoenix), setOf(PhoenixValue(9)))
        )
    }

    @Test
    fun straights() {
        assertEquals(
            NumberedStraight(setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of DIAMONDS)),
            findCardCombination(null, setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of DIAMONDS), emptySet())
        )
        assertEquals(
            MahjongStraight(setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS), Mahjong),
            findCardCombination(null, setOf(Mahjong, 2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS), emptySet())
        )
        assertEquals(
            PhoenixStraight(setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS), Phoenix, PhoenixValue(6)),
            findCardCombination(null, setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS, Phoenix), setOf(PhoenixValue(6)))
        )
        assertEquals(
            PhoenixStraight(setOf(3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of HEARTS), Phoenix, PhoenixValue(2)),
            findCardCombination(null, setOf(3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of HEARTS, Phoenix), setOf(PhoenixValue(2)))
        )
    }

    @Test
    fun noCombination() {
        // No Phoenix value
        assertNull(findCardCombination(null, setOf(9 of HEARTS, 9 of DIAMONDS, 8 of HEARTS, 8 of DIAMONDS, Phoenix), emptySet()))
        assertNull(findCardCombination(null, setOf(3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of HEARTS, Phoenix), emptySet()))

        // Rubbish
        assertNull(findCardCombination(null, setOf(8 of HEARTS, 9 of HEARTS), emptySet()))
        assertNull(findCardCombination(null, setOf(8 of HEARTS, 8 of DIAMONDS, 9 of HEARTS), emptySet()))
        assertNull(findCardCombination(null, setOf(8 of HEARTS, 9 of HEARTS, 10 of DIAMONDS), emptySet()))
        assertNull(findCardCombination(null, setOf(8 of HEARTS, 9 of HEARTS, 10 of HEARTS, JACK of HEARTS), emptySet()))
        assertNull(findCardCombination(null, setOf(6 of HEARTS, 8 of HEARTS, 9 of HEARTS, 10 of HEARTS), emptySet()))
        assertNull(findCardCombination(null, setOf(6 of HEARTS, Dog), emptySet()))
        assertNull(findCardCombination(null, setOf(6 of HEARTS, Dragon), emptySet()))
    }
}
