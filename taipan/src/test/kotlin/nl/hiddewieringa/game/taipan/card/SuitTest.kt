package nl.hiddewieringa.game.taipan.card

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SuitTest {

    @Test
    fun fullSuit() {
        val expected = """🂲-🂳-🂴-🂵-🂶-🂷-🂸-🂹-🂺-🂻-🂽-🂾-🂱-🃂-🃃-🃄-🃅-🃆-🃇-🃈-🃉-🃊-🃋-🃍-🃎-🃁-🂢-🂣-🂤-🂥-🂦-🂧-🂨-🂩-🂪-🂫-🂭-🂮-🂡-🃒-🃓-🃔-🃕-🃖-🃗-🃘-🃙-🃚-🃛-🃝-🃞-🃑-🐉-🦤-🐕-1️⃣"""
        assertEquals(expected, fullSuit.joinToString("-"))
    }

    @Test
    fun shuffle() {
        val expected = """🃓-🃔-🐕-🂫-🐉-🂴-🃅-🃝-🂾-🂱-🃙-🃞-🃑-🃃-🂥-🂡-🂩-🃊-🃆-🂺-🃋-🦤-🃒-🂳-🂣-🃕-🂪-🃘-🂭-🂨-🂦-🃈-🂻-🂢-🂶-🃂-🂲-🂮-🃗-🃁-🂸-🃖-🃎-🃉-🂷-🃚-🃄-🂵-🃍-🂽-1️⃣-🃛-🂹-🂤-🃇-🂧"""
        val actual = Random(47).shuffle(fullSuit).joinToString("-")
        assertEquals(expected, actual)
    }
}