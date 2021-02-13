package nl.hiddewieringa.game.taipan.card

import nl.hiddewieringa.game.taipan.PhoenixValue
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.ACE
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.JACK
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.KING
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.QUEEN
import nl.hiddewieringa.game.taipan.card.Suit.*
import kotlin.test.*

class StraightTest {
    @Test
    fun validNumberedStraight() {
        assertNotNull(
            NumberedStraight(
                setOf(
                    2 of DIAMONDS,
                    3 of DIAMONDS,
                    4 of DIAMONDS,
                    5 of DIAMONDS,
                    6 of DIAMONDS,
                )
            )
        )
        assertNotNull(
            NumberedStraight(
                setOf(
                    2 of DIAMONDS,
                    3 of HEARTS,
                    4 of CLUBS,
                    5 of DIAMONDS,
                    6 of HEARTS,
                    7 of CLUBS,
                    8 of DIAMONDS,
                    9 of HEARTS,
                    10 of DIAMONDS,
                    JACK of HEARTS,
                    QUEEN of CLUBS,
                    KING of HEARTS,
                    ACE of DIAMONDS,
                )
            )
        )
    }

    @Test
    fun invalidNumberedStraight() {
        assertFailsWith<IllegalArgumentException> {
            NumberedStraight(
                setOf(
                    3 of DIAMONDS,
                    4 of DIAMONDS,
                    5 of DIAMONDS,
                    6 of DIAMONDS,
                )
            )
        }
        assertFailsWith<IllegalArgumentException> {
            NumberedStraight(
                setOf(
                    2 of DIAMONDS,
                    3 of DIAMONDS,
                    4 of DIAMONDS,
                    6 of DIAMONDS,
                    7 of DIAMONDS,
                )
            )
        }
    }

    @Test
    fun validMahjongStraight() {
        assertNotNull(
            MahjongStraight(
                setOf(
                    2 of DIAMONDS,
                    3 of HEARTS,
                    4 of CLUBS,
                    5 of DIAMONDS,
                    6 of HEARTS,
                    7 of CLUBS,
                    8 of DIAMONDS,
                    9 of HEARTS,
                    10 of DIAMONDS,
                    JACK of HEARTS,
                    QUEEN of CLUBS,
                    KING of HEARTS,
                    ACE of DIAMONDS,
                ),
                Mahjong
            )
        )
        assertNotNull(
            MahjongStraight(
                setOf(
                    2 of DIAMONDS,
                    3 of HEARTS,
                    4 of CLUBS,
                    5 of DIAMONDS,
                ),
                Mahjong
            )
        )
    }

    @Test
    fun invalidMahjongStraight() {
        assertFailsWith<IllegalArgumentException> {
            MahjongStraight(
                setOf(
                    3 of HEARTS,
                    4 of CLUBS,
                    5 of DIAMONDS,
                    6 of HEARTS,
                    7 of CLUBS,
                    8 of DIAMONDS,
                    9 of HEARTS,
                    10 of DIAMONDS,
                    JACK of HEARTS,
                    QUEEN of CLUBS,
                    KING of HEARTS,
                    ACE of DIAMONDS,
                ),
                Mahjong
            )
        }
    }

    @Test
    fun validPhoenixStraight() {
        assertNotNull(
            PhoenixStraight(
                setOf(
                    2 of DIAMONDS,
                    3 of HEARTS,
                    4 of CLUBS,
                    5 of DIAMONDS,
                    6 of HEARTS,
                    8 of DIAMONDS,
                    9 of HEARTS,
                    10 of DIAMONDS,
                    JACK of HEARTS,
                    QUEEN of CLUBS,
                    KING of HEARTS,
                    ACE of DIAMONDS,
                ),
                Phoenix, PhoenixValue(7)
            )
        )
        assertNotNull(
            PhoenixStraight(
                setOf(
                    JACK of HEARTS,
                    QUEEN of CLUBS,
                    KING of HEARTS,
                    ACE of DIAMONDS,
                ),
                Phoenix, PhoenixValue(7)
            )
        )
    }

    @Test
    fun invalidPhoenixStraight() {
        assertFailsWith<IllegalArgumentException> {
            PhoenixStraight(
                setOf(
                    2 of DIAMONDS,
                    3 of HEARTS,
                    4 of CLUBS,
                    5 of DIAMONDS,
                    6 of HEARTS,
                    8 of DIAMONDS,
                    9 of HEARTS,
                    10 of DIAMONDS,
                    JACK of HEARTS,
                    QUEEN of CLUBS,
                    KING of HEARTS,
                    ACE of DIAMONDS,
                ),
                Phoenix, PhoenixValue(8)
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PhoenixStraight(
                setOf(
                    2 of DIAMONDS,
                    3 of HEARTS,
                    4 of CLUBS,
                    5 of DIAMONDS,
                    6 of HEARTS,
                    9 of HEARTS,
                    10 of DIAMONDS,
                    JACK of HEARTS,
                    QUEEN of CLUBS,
                    KING of HEARTS,
                    ACE of DIAMONDS,
                ),
                Phoenix, PhoenixValue(8)
            )
        }
    }

    @Test
    fun validMahjongPhoenixStraight() {
        assertNotNull(
            MahjongPhoenixStraight(
                setOf(
                    2 of DIAMONDS,
                    3 of HEARTS,
                    4 of CLUBS,
                    5 of DIAMONDS,
                    6 of HEARTS,
                    8 of DIAMONDS,
                    9 of HEARTS,
                    10 of DIAMONDS,
                    JACK of HEARTS,
                    QUEEN of CLUBS,
                    KING of HEARTS,
                    ACE of DIAMONDS,
                ),
                Mahjong, Phoenix, PhoenixValue(7)
            )
        )
        assertNotNull(
            MahjongPhoenixStraight(
                setOf(
                    2 of DIAMONDS,
                    3 of HEARTS,
                    4 of CLUBS,
                ),
                Mahjong, Phoenix, PhoenixValue(7)
            )
        )
    }

    @Test
    fun invalidMahjongPhoenixStraight() {
        assertFailsWith<IllegalArgumentException> {
            MahjongPhoenixStraight(
                setOf(
                    3 of HEARTS,
                    4 of CLUBS,
                    5 of DIAMONDS,
                    6 of HEARTS,
                    8 of DIAMONDS,
                    9 of HEARTS,
                    10 of DIAMONDS,
                    JACK of HEARTS,
                    QUEEN of CLUBS,
                    KING of HEARTS,
                    ACE of DIAMONDS,
                ),
                Mahjong, Phoenix, PhoenixValue(7)
            )
        }
        assertFailsWith<IllegalArgumentException> {
            MahjongPhoenixStraight(
                setOf(
                    2 of DIAMONDS,
                    3 of HEARTS,
                    4 of CLUBS,
                    5 of DIAMONDS,
                    6 of HEARTS,
                    8 of DIAMONDS,
                    9 of HEARTS,
                    10 of DIAMONDS,
                    JACK of HEARTS,
                    QUEEN of CLUBS,
                    KING of HEARTS,
                    ACE of DIAMONDS,
                ),
                Mahjong, Phoenix, PhoenixValue(8)
            )
        }
        assertFailsWith<IllegalArgumentException> {
            MahjongPhoenixStraight(
                setOf(
                    2 of DIAMONDS,
                    3 of HEARTS,
                    4 of CLUBS,
                    5 of DIAMONDS,
                    6 of HEARTS,
                    9 of HEARTS,
                    10 of DIAMONDS,
                    JACK of HEARTS,
                    QUEEN of CLUBS,
                    KING of HEARTS,
                    ACE of DIAMONDS,
                ),
                Mahjong, Phoenix, PhoenixValue(8)
            )
        }
    }
}
