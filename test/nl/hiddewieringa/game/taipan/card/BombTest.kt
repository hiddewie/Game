package nl.hiddewieringa.game.taipan.card

import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.ACE
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.JACK
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.KING
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.QUEEN
import nl.hiddewieringa.game.taipan.card.Suit.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BombTest {
    @Test
    fun validQuadrupleBomb() {
        assertNotNull(
            QuadrupleBomb(
                setOf(
                    10 of DIAMONDS,
                    10 of HEARTS,
                    10 of CLUBS,
                    10 of SPADES,
                )
            )
        )
    }

    @Test
    fun invalidQuadrupleBomb() {
        assertThrows<IllegalArgumentException> {
            QuadrupleBomb(
                setOf(
                    9 of DIAMONDS,
                    10 of HEARTS,
                    10 of CLUBS,
                    10 of SPADES,
                )
            )
        }
        assertThrows<IllegalArgumentException> {
            QuadrupleBomb(
                setOf(
                    10 of DIAMONDS,
                    10 of DIAMONDS,
                    10 of CLUBS,
                    10 of SPADES,
                )
            )
        }
    }

    @Test
    fun validStraightBomb() {
        assertNotNull(
            StraightBomb(
                setOf(
                    6 of DIAMONDS,
                    7 of DIAMONDS,
                    8 of DIAMONDS,
                    9 of DIAMONDS,
                    10 of DIAMONDS,
                )
            )
        )
        assertNotNull(
            StraightBomb(
                setOf(
                    2 of DIAMONDS,
                    3 of DIAMONDS,
                    4 of DIAMONDS,
                    5 of DIAMONDS,
                    6 of DIAMONDS,
                    7 of DIAMONDS,
                    8 of DIAMONDS,
                    9 of DIAMONDS,
                    10 of DIAMONDS,
                    JACK of DIAMONDS,
                    QUEEN of DIAMONDS,
                    KING of DIAMONDS,
                    ACE of DIAMONDS,
                )
            )
        )
    }

    @Test
    fun invalidStraightBomb() {
        assertThrows<IllegalArgumentException> {
            StraightBomb(
                setOf(
                    7 of DIAMONDS,
                    8 of DIAMONDS,
                    9 of DIAMONDS,
                    10 of DIAMONDS,
                )
            )
        }
        assertThrows<IllegalArgumentException> {
            StraightBomb(
                setOf(
                    6 of HEARTS,
                    7 of DIAMONDS,
                    8 of DIAMONDS,
                    9 of DIAMONDS,
                    10 of DIAMONDS,
                )
            )
        }
        assertThrows<IllegalArgumentException> {
            StraightBomb(
                setOf(
                    5 of DIAMONDS,
                    7 of DIAMONDS,
                    8 of DIAMONDS,
                    9 of DIAMONDS,
                    10 of DIAMONDS,
                )
            )
        }
    }

}