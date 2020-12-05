package nl.hiddewieringa.game.taipan.card

sealed class Card(
    val points: Int
) : Comparable<Card> {

    override fun compareTo(other: Card): Int =
        when {
            this == other -> 0
            other == Dog -> 1
            this == Dog -> -1
            other == Mahjong -> 1
            this == Mahjong -> -1
            other == Dragon -> -1
            this == Dragon -> 1
            other == Phoenix -> -1
            this == Phoenix -> 1
            other is NumberedCard && this is NumberedCard -> when (this.value) {
                other.value -> this.suit.compareTo(other.suit)
                else -> this.value - other.value
            }
            else -> throw RuntimeException("CompareTo should be exhaustive $this <> $other")
        }
}

enum class Suit {
    HEARTS,
    DIAMONDS,
    SPADES,
    CLUBS
}

infix fun Int.of(suit: Suit): NumberedCard =
    NumberedCard(suit, this)

data class NumberedCard(val suit: Suit, val value: Int) : Card(
    when (value) {
        5 -> 5
        10, KING -> 10
        else -> 0
    }
) {
    init {
        require(value >= 2) { "The card value should be greater or equal to two." }
        require(value <= NumberedCard.ACE) { "The card value should be less than or equal to ACE (14)." }
    }

    override fun toString() =
        when (value) {
            ACE -> when (suit) {
                Suit.HEARTS -> "\uD83C\uDCB1"
                Suit.DIAMONDS -> "\uD83C\uDCC1"
                Suit.SPADES -> "\uD83C\uDCA1"
                Suit.CLUBS -> "\uD83C\uDCD1"
            }
            KING -> when (suit) {
                Suit.HEARTS -> "\uD83C\uDCBE"
                Suit.DIAMONDS -> "\uD83C\uDCCE"
                Suit.SPADES -> "\uD83C\uDCAE"
                Suit.CLUBS -> "\uD83C\uDCDE"
            }
            QUEEN -> when (suit) {
                Suit.HEARTS -> "\uD83C\uDCBD"
                Suit.DIAMONDS -> "\uD83C\uDCCD"
                Suit.SPADES -> "\uD83C\uDCAD"
                Suit.CLUBS -> "\uD83C\uDCDD"
            }
            else -> when (suit) {
                Suit.HEARTS -> "\uD83C" + (0xDCB0 + value).toChar()
                Suit.DIAMONDS -> "\uD83C" + (0xDCC0 + value).toChar()
                Suit.SPADES -> "\uD83C" + (0xDCA0 + value).toChar()
                Suit.CLUBS -> "\uD83C" + (0xDCD0 + value).toChar()
            }
        }

    companion object {
        const val JACK = 11
        const val QUEEN = 12
        const val KING = 13
        const val ACE = 14
    }
}

object Dragon : Card(25) {
    override fun toString(): String =
        "\uD83D\uDC09"
}

object Phoenix : Card(-25) {
    override fun toString(): String =
        "\uD83E\uDDA4"
}

object Dog : Card(0) {
    override fun toString(): String =
        "\uD83D\uDC15"
}

object Mahjong : Card(0) {
    override fun toString(): String =
        "1️⃣"
}