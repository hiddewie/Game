package nl.hiddewieringa.game.taipan.card

import nl.hiddewieringa.game.taipan.card.Suit.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FullHouseTest {
    @Test
    fun validFullHouse() {
        assertNotNull(
            FullHouse(
                NumberedTuple(8 of DIAMONDS, 8 of HEARTS),
                NumberedTriple(10 of DIAMONDS, 10 of HEARTS, 10 of CLUBS),
            )
        )
    }

    @Test
    fun invalidFullHouse() {
        assertThrows<IllegalArgumentException> {
            FullHouse(
                NumberedTuple(10 of DIAMONDS, 10 of HEARTS),
                NumberedTriple(10 of DIAMONDS, 10 of HEARTS, 10 of CLUBS),
            )
        }
    }

    @Test
    fun validPhoenixFullHouse() {
        assertNotNull(
            FullHouse(
                PhoenixTuple(8 of DIAMONDS, Phoenix),
                NumberedTriple(10 of DIAMONDS, 10 of HEARTS, 10 of CLUBS),
            )
        )
        assertNotNull(
            FullHouse(
                NumberedTuple(8 of DIAMONDS, 8 of HEARTS),
                PhoenixTriple(10 of DIAMONDS, 10 of HEARTS, Phoenix),
            )
        )
    }

    @Test
    fun invalidPhoenixFullHouse() {
        assertThrows<IllegalArgumentException> {
            FullHouse(
                PhoenixTuple(8 of DIAMONDS, Phoenix),
                PhoenixTriple(10 of DIAMONDS, 10 of HEARTS, Phoenix),
            )
        }
    }
}