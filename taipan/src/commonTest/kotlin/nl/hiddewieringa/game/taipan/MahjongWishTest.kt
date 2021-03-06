package nl.hiddewieringa.game.taipan

import nl.hiddewieringa.game.taipan.card.*
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.ACE
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.JACK
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.QUEEN
import nl.hiddewieringa.game.taipan.card.Suit.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MahjongWishTest {

    @Test
    fun hasStraightOfLength() {
        assertTrue(hasStraightOfLengthAndContainsValue(2, 3, setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS)))
        assertTrue(hasStraightOfLengthAndContainsValue(2, 3, setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of HEARTS)))
        assertTrue(hasStraightOfLengthAndContainsValue(2, 5, setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of HEARTS)))
        assertTrue(hasStraightOfLengthAndContainsValue(2, 3, setOf(2 of HEARTS, Phoenix, 4 of HEARTS)))
    }

    @Test
    fun doesNotHaveStraightOfLength() {
        assertFalse(hasStraightOfLengthAndContainsValue(2, 3, setOf(3 of HEARTS, 4 of HEARTS)))
        assertFalse(hasStraightOfLengthAndContainsValue(2, 3, setOf(3 of HEARTS, 4 of HEARTS, 5 of HEARTS)))
        assertFalse(hasStraightOfLengthAndContainsValue(2, 3, setOf(2 of HEARTS, 4 of HEARTS, 5 of HEARTS)))
        assertFalse(hasStraightOfLengthAndContainsValue(2, 3, setOf(2 of HEARTS, 5 of HEARTS, 6 of HEARTS, Phoenix)))
        assertFalse(hasStraightOfLengthAndContainsValue(2, 3, setOf(Phoenix, 3 of HEARTS, 4 of HEARTS)))

        // Mahjong does not count as a numbered card in a straight, it can only be used in a starting trick
        assertFalse(hasStraightOfLengthAndContainsValue(2, 3, setOf(2 of HEARTS, Phoenix, Mahjong)))
    }

    @Test
    fun cardsContainWishHighCard() {
        assertTrue(cardsContainWish(6, HighCard(5 of HEARTS), setOf(6 of HEARTS)))
        assertTrue(cardsContainWish(6, HighCard(Phoenix, 1.5f), setOf(6 of HEARTS)))
        assertTrue(cardsContainWish(6, HighCard(Mahjong), setOf(6 of HEARTS)))

        assertFalse(cardsContainWish(6, HighCard(Phoenix, 6.5f), setOf(6 of HEARTS)))
        assertFalse(cardsContainWish(6, HighCard(5 of HEARTS), setOf(7 of HEARTS)))
        assertFalse(cardsContainWish(3, HighCard(5 of HEARTS), setOf(3 of HEARTS)))
        assertFalse(cardsContainWish(3, HighCard(Dragon), setOf(3 of HEARTS)))
    }

    @Test
    fun cardsContainWishTuple() {
        val tuple = NumberedTuple(5 of HEARTS, 5 of DIAMONDS)
        assertTrue(cardsContainWish(6, tuple, setOf(6 of HEARTS, 6 of DIAMONDS)))
        assertTrue(cardsContainWish(6, tuple, setOf(6 of HEARTS, Phoenix)))
        assertTrue(cardsContainWish(2, tuple, setOf(2 of HEARTS, 2 of DIAMONDS, 2 of SPADES, 2 of CLUBS)))
        assertTrue(cardsContainWish(2, tuple, setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of HEARTS)))

        assertFalse(cardsContainWish(6, tuple, emptySet()))
        assertFalse(cardsContainWish(6, tuple, setOf(6 of HEARTS)))
        assertFalse(cardsContainWish(6, tuple, setOf(3 of HEARTS, 3 of DIAMONDS)))
        assertFalse(cardsContainWish(6, tuple, setOf(Phoenix)))
    }

    @Test
    fun cardsContainWishTriple() {
        val triple = NumberedTriple(5 of HEARTS, 5 of DIAMONDS, 5 of SPADES)
        assertTrue(cardsContainWish(6, triple, setOf(6 of HEARTS, 6 of DIAMONDS, 6 of SPADES)))
        assertTrue(cardsContainWish(6, triple, setOf(6 of HEARTS, 6 of DIAMONDS, Phoenix)))
        assertTrue(cardsContainWish(2, triple, setOf(2 of HEARTS, 2 of DIAMONDS, 2 of SPADES, 2 of CLUBS)))
        assertTrue(cardsContainWish(2, triple, setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of HEARTS)))

        assertFalse(cardsContainWish(6, triple, setOf(6 of HEARTS, 6 of DIAMONDS)))
        assertFalse(cardsContainWish(6, triple, setOf(3 of HEARTS, 3 of DIAMONDS, 3 of SPADES)))
        assertFalse(cardsContainWish(6, triple, setOf(Phoenix, 3 of SPADES)))
    }

    @Test
    fun cardsContainWishFullHouse() {
        val fullHouse = FullHouse(NumberedTuple(2 of HEARTS, 2 of DIAMONDS), NumberedTriple(5 of HEARTS, 5 of DIAMONDS, 5 of SPADES))
        assertTrue(cardsContainWish(6, fullHouse, setOf(6 of HEARTS, 6 of DIAMONDS, 6 of SPADES, 3 of HEARTS, 3 of DIAMONDS)))
        assertTrue(cardsContainWish(6, fullHouse, setOf(6 of HEARTS, 6 of DIAMONDS, Phoenix, 3 of HEARTS, 3 of DIAMONDS)))
        assertTrue(cardsContainWish(2, fullHouse, setOf(2 of HEARTS, 2 of DIAMONDS, 2 of SPADES, 2 of CLUBS)))
        assertTrue(cardsContainWish(2, fullHouse, setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of HEARTS)))

        assertFalse(cardsContainWish(6, fullHouse, setOf(6 of HEARTS, 6 of DIAMONDS, 3 of SPADES, 3 of HEARTS, 3 of DIAMONDS)))
        assertFalse(cardsContainWish(6, fullHouse, setOf(6 of HEARTS, Phoenix, 3 of HEARTS, 3 of DIAMONDS)))
        assertFalse(cardsContainWish(6, fullHouse, setOf(6 of HEARTS, 6 of DIAMONDS, 3 of HEARTS, 3 of DIAMONDS)))
    }

    @Test
    fun cardsContainWishTupleSequence() {
        val tupleSequence = TupleSequence(
            listOf(
                NumberedTuple(2 of HEARTS, 2 of DIAMONDS),
                NumberedTuple(3 of HEARTS, 3 of DIAMONDS),
                NumberedTuple(4 of HEARTS, 4 of DIAMONDS),
            )
        )
        assertTrue(cardsContainWish(6, tupleSequence, setOf(5 of HEARTS, 5 of DIAMONDS, 6 of HEARTS, 6 of DIAMONDS, 7 of HEARTS, 7 of DIAMONDS)))
        assertTrue(cardsContainWish(6, tupleSequence, setOf(5 of HEARTS, 5 of DIAMONDS, 6 of HEARTS, Phoenix, 7 of HEARTS, 7 of DIAMONDS)))
        assertTrue(cardsContainWish(2, tupleSequence, setOf(2 of HEARTS, 2 of DIAMONDS, 2 of SPADES, 2 of CLUBS)))
        assertTrue(cardsContainWish(2, tupleSequence, setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of HEARTS)))

        assertFalse(cardsContainWish(6, tupleSequence, setOf(5 of HEARTS, 5 of DIAMONDS, 6 of HEARTS, 6 of DIAMONDS)))
        assertFalse(cardsContainWish(6, tupleSequence, setOf(5 of HEARTS, 5 of DIAMONDS, Phoenix, 7 of HEARTS, 7 of DIAMONDS)))
        assertFalse(cardsContainWish(4, tupleSequence, setOf(2 of HEARTS, 3 of DIAMONDS, 3 of HEARTS, 3 of DIAMONDS, 4 of HEARTS, 4 of DIAMONDS)))
    }

    @Test
    fun cardsContainWishStraight() {
        val straight = NumberedStraight(setOf(3 of HEARTS, 4 of HEARTS, 5 of SPADES, 6 of DIAMONDS, 7 of SPADES))
        assertTrue(cardsContainWish(6, straight, setOf(4 of HEARTS, 5 of SPADES, 6 of DIAMONDS, 7 of SPADES, 8 of SPADES)))
        assertTrue(cardsContainWish(6, straight, setOf(4 of HEARTS, 5 of SPADES, 6 of DIAMONDS, 7 of SPADES, Phoenix)))
        assertTrue(cardsContainWish(6, straight, setOf(4 of HEARTS, 5 of SPADES, 6 of DIAMONDS, Phoenix, 8 of SPADES)))
        assertTrue(cardsContainWish(6, straight, setOf(2 of DIAMONDS, 3 of DIAMONDS, 4 of HEARTS, 5 of SPADES, 6 of DIAMONDS, 7 of SPADES, 8 of SPADES)))
        assertTrue(cardsContainWish(2, straight, setOf(2 of HEARTS, 2 of DIAMONDS, 2 of SPADES, 2 of CLUBS)))
        assertTrue(cardsContainWish(2, straight, setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of HEARTS)))

        assertFalse(cardsContainWish(6, straight, setOf(3 of HEARTS, 4 of HEARTS, 5 of SPADES, 6 of DIAMONDS, 7 of SPADES)))
        assertFalse(cardsContainWish(6, straight, setOf(5 of SPADES, 6 of DIAMONDS, 7 of SPADES, 8 of SPADES)))
        assertFalse(cardsContainWish(6, straight, setOf(4 of HEARTS, 5 of SPADES, Phoenix, 7 of SPADES, 8 of SPADES)))
        assertFalse(cardsContainWish(6, straight, setOf(4 of HEARTS, 5 of SPADES, 6 of DIAMONDS, Phoenix)))
        assertFalse(cardsContainWish(6, straight, setOf(Mahjong, 2 of DIAMONDS, 3 of DIAMONDS, 4 of HEARTS, 5 of SPADES)))
        assertFalse(cardsContainWish(5, straight, setOf(Mahjong, 2 of DIAMONDS, Phoenix, 5 of SPADES, 6 of DIAMONDS)))
    }

    @Test
    fun cardsContainWishBomb() {
        val straightBomb = StraightBomb(setOf(3 of DIAMONDS, 4 of DIAMONDS, 5 of DIAMONDS, 6 of DIAMONDS, 7 of DIAMONDS, 8 of DIAMONDS))

        assertTrue(cardsContainWish(6, straightBomb, setOf(4 of HEARTS, 5 of HEARTS, 6 of HEARTS, 7 of HEARTS, 8 of HEARTS, 9 of HEARTS, 10 of HEARTS)))
        assertTrue(cardsContainWish(6, straightBomb, setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of HEARTS, 7 of HEARTS, 8 of HEARTS)))

        assertFalse(cardsContainWish(2, straightBomb, setOf(2 of DIAMONDS, 2 of HEARTS, 2 of SPADES, 2 of CLUBS)))
        assertFalse(cardsContainWish(6, straightBomb, setOf(4 of DIAMONDS, 5 of DIAMONDS, 6 of DIAMONDS, 7 of DIAMONDS, Phoenix, 9 of DIAMONDS)))
        assertFalse(cardsContainWish(6, straightBomb, setOf(9 of HEARTS, 10 of HEARTS, JACK of HEARTS, QUEEN of HEARTS, ACE of HEARTS)))
        assertFalse(cardsContainWish(6, straightBomb, setOf(3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of HEARTS, 7 of HEARTS)))
        assertFalse(cardsContainWish(6, straightBomb, setOf(5 of HEARTS, 6 of HEARTS, 7 of HEARTS, 8 of HEARTS, 9 of HEARTS)))
        assertFalse(cardsContainWish(5, straightBomb, setOf(Mahjong, 2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS)))

        val quadrupleBomb = QuadrupleBomb(setOf(3 of HEARTS, 3 of DIAMONDS, 3 of SPADES, 3 of CLUBS))

        assertTrue(cardsContainWish(4, quadrupleBomb, setOf(4 of DIAMONDS, 4 of HEARTS, 4 of SPADES, 4 of CLUBS)))
        assertTrue(cardsContainWish(6, quadrupleBomb, setOf(4 of HEARTS, 5 of HEARTS, 6 of HEARTS, 7 of HEARTS, 8 of HEARTS, 9 of HEARTS)))
        assertTrue(cardsContainWish(6, quadrupleBomb, setOf(2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of HEARTS, 7 of HEARTS, 8 of HEARTS)))
        assertTrue(cardsContainWish(6, quadrupleBomb, setOf(3 of HEARTS, 4 of HEARTS, 5 of HEARTS, 6 of HEARTS, 7 of HEARTS)))

        assertFalse(cardsContainWish(2, quadrupleBomb, setOf(2 of DIAMONDS, 2 of HEARTS, 2 of SPADES, 2 of CLUBS)))
        assertFalse(cardsContainWish(6, quadrupleBomb, setOf(4 of DIAMONDS, 5 of DIAMONDS, 6 of DIAMONDS, 7 of DIAMONDS, Phoenix, 9 of DIAMONDS)))
        assertFalse(cardsContainWish(6, quadrupleBomb, setOf(9 of HEARTS, 10 of HEARTS, JACK of HEARTS, QUEEN of HEARTS, ACE of HEARTS)))
        assertFalse(cardsContainWish(5, quadrupleBomb, setOf(Mahjong, 2 of HEARTS, 3 of HEARTS, 4 of HEARTS, 5 of HEARTS)))
    }
}
