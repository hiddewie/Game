package nl.hiddewieringa.game.taipan.card

import kotlinx.serialization.Serializable
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.ACE

@Serializable
sealed class Card(
    val points: Int,
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

@Serializable
enum class Suit(val character: String) {
    HEARTS("♥"),
    DIAMONDS("♦"),
    SPADES("♠"),
    CLUBS("♣");

    override fun toString(): String =
        character
}

infix fun Int.of(suit: Suit): NumberedCard =
    NumberedCard(suit, this)

@Serializable
data class NumberedCard(val suit: Suit, val value: Int) : Card(
    when (value) {
        5 -> 5
        10, KING -> 10
        else -> 0
    }
) {
    init {
        require(value >= 2) { "The card value should be greater or equal to two." }
        require(value <= ACE) { "The card value should be less than or equal to ACE ($ACE)." }
    }

    override fun toString() =
        "$suit${stringifyValue(value)}"

    companion object {
        const val JACK = 11
        const val QUEEN = 12
        const val KING = 13
        const val ACE = 14

        val VALUES = (2..10).toList() + listOf(JACK, QUEEN, KING, ACE)

        fun stringifyValue(value: Int) =
            when (value) {
                in 2..10 -> value.toString()
                ACE -> "A"
                KING -> "K"
                QUEEN -> "Q"
                JACK -> "J"
                else -> throw IllegalArgumentException("Illegal value $value")
            }
    }
}

@Serializable
object Dragon : Card(25) {
    override fun toString(): String =
        "\uD83D\uDC09"

    const val value = ACE + 1
}

@Serializable
object Phoenix : Card(-25) {
    override fun toString(): String =
        "\uD83E\uDDA4"
}

@Serializable
object Dog : Card(0) {
    override fun toString(): String =
        "\uD83D\uDC15"

    const val value = 0
}

@Serializable
object Mahjong : Card(0) {
    override fun toString(): String =
        "1️⃣"

    const val value = 1
}

@Serializable
data class ThreeWayPass(val left: Card, val forward: Card, val right: Card) {
    init {
        require(left != forward) { "Left should not be the same card as forward" }
        require(forward != right) { "Right should not be the same card as forward" }
        require(left != right) { "Left should not be the same card as right" }
    }
}
