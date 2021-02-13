package nl.hiddewieringa.game.taipan.card

import kotlin.random.Random
import kotlin.test.*

class SuitTest {

    @Test
    fun fullSuit() {
        val expected = """♥2-♥3-♥4-♥5-♥6-♥7-♥8-♥9-♥10-♥J-♥Q-♥K-♥A-♦2-♦3-♦4-♦5-♦6-♦7-♦8-♦9-♦10-♦J-♦Q-♦K-♦A-♠2-♠3-♠4-♠5-♠6-♠7-♠8-♠9-♠10-♠J-♠Q-♠K-♠A-♣2-♣3-♣4-♣5-♣6-♣7-♣8-♣9-♣10-♣J-♣Q-♣K-♣A-🐉-🦤-🐕-1️⃣"""
        assertEquals(expected, fullDeck.joinToString("-"))
    }

    @Test
    fun shuffle() {
        val expected = """♣A-♦3-♥5-♦J-♦8-♣4-♣9-♦5-♥3-♠10-♣Q-♦6-🦤-♥6-🐉-♠7-♣7-♥8-♠9-♦10-♦2-♥7-♥9-♠K-♠2-♠J-♠Q-♣8-♥4-♦K-♣K-♥10-♦4-♣2-♥J-♦7-1️⃣-♠A-♦Q-♣3-♣J-♥K-♥2-♣5-♠5-🐕-♣10-♥Q-♠6-♥A-♦A-♠3-♦9-♠8-♣6-♠4"""
        val actual = fullDeck.shuffled(Random(47)).joinToString("-")
        assertEquals(expected, actual)
    }
}
