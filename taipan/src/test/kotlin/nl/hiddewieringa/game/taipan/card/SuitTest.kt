package nl.hiddewieringa.game.taipan.card

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SuitTest {

    @Test
    fun fullSuit() {
        val expected = """🂲-🂳-🂴-🂵-🂶-🂷-🂸-🂹-🂺-🂻-🂽-🂾-🂱-🃂-🃃-🃄-🃅-🃆-🃇-🃈-🃉-🃊-🃋-🃍-🃎-🃁-🂢-🂣-🂤-🂥-🂦-🂧-🂨-🂩-🂪-🂫-🂭-🂮-🂡-🃒-🃓-🃔-🃕-🃖-🃗-🃘-🃙-🃚-🃛-🃝-🃞-🃑-🐉-🦤-🐕-1️⃣"""
        assertEquals(expected, fullDeck.joinToString("-"))
    }

    @Test
    fun shuffle() {
        val expected = """🃑-🃃-🂵-🃋-🃈-🃔-🃙-🃅-🂳-🂪-🃝-🃆-🦤-🂶-🐉-🂧-🃗-🂸-🂩-🃊-🃂-🂷-🂹-🂮-🂢-🂫-🂭-🃘-🂴-🃎-🃞-🂺-🃄-🃒-🂻-🃇-1️⃣-🂡-🃍-🃓-🃛-🂾-🂲-🃕-🂥-🐕-🃚-🂽-🂦-🂱-🃁-🂣-🃉-🂨-🃖-🂤"""
        val actual = fullDeck.shuffled(Random(47)).joinToString("-")
        assertEquals(expected, actual)
    }
}
