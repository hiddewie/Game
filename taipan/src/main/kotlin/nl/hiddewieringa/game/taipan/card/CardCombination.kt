package nl.hiddewieringa.game.taipan.card

import nl.hiddewieringa.game.taipan.PhoenixValue
import nl.hiddewieringa.game.taipan.PlayCardsAddon

sealed class CardCombination(
    val cards: CardSet
) {
    fun contains(card: Card) =
        cards.any { it == card }
}

data class HighCard(val card: Card) : CardCombination(setOf(card))

sealed class Tuple(cards: CardSet, val value: Int) : CardCombination(cards)
data class NumberedTuple(val card1: NumberedCard, val card2: NumberedCard) : Tuple(setOf(card1, card2), card1.value) {
    init {
        require(card1.value == card2.value) { "The cards should be equal" }
        require(cards.size == 2) { "There should be two cards" }
    }
}

data class PhoenixTuple(val card1: NumberedCard, val card2: Phoenix) : Tuple(setOf(card1, card2), card1.value)

sealed class Triple(cards: Set<Card>, val value: Int) : CardCombination(cards.toSet()) {
    init {
        require(cards.size == 3) { "There should be three cards" }
    }
}

data class NumberedTriple(val card1: NumberedCard, val card2: NumberedCard, val card3: NumberedCard) : Triple(setOf(card1, card2, card3), card1.value) {
    init {
        val allSequential = setOf(card1, card2, card3)
            .zipWithNext { a, b -> a.value == b.value }
            .all { it }
        require(allSequential) { "The cards should be sequential" }
    }
}

data class PhoenixTriple(val card1: NumberedCard, val card2: NumberedCard, val card3: Phoenix) : Triple(setOf(card1, card2, card3), card1.value) {
    init {
        require(card1.value == card2.value) { "The card values should be equal" }
    }
}

data class FullHouse(val tuple: Tuple, val triple: Triple) : CardCombination(tuple.cards + triple.cards) {
    val value: Int

    init {
        require(cards.size == 5) { "There should be five cards" }
        require(!(tuple.contains(Phoenix) && triple.contains(Phoenix))) { "Only one of the tuple and the triple may contain a Phoenix" }
        value = triple.value
    }
}

data class TupleSequence(val tuples: List<Tuple>) : CardCombination(tuples.flatMapTo(mutableSetOf(), { it.cards })) {
    val minValue: Int
    val length = tuples.size

    init {
        require(tuples.size >= 2) { "There must be at least two tuples" }
        val allSequential = tuples
            .zipWithNext { a, b -> a.value + 1 == b.value }
            .all { it }
        require(allSequential) { "The tuples should be sequential " }
        require(tuples.count { it.contains(Phoenix) } <= 1) { "At most one of the tuples may contain a Phoenix" }

        minValue = tuples.minOf { it.value }
    }
}

sealed class Straight(cards: CardSet, val minValue: Int) : CardCombination(cards) {
    val length = cards.size
}

data class NumberedStraight(val straightCards: Set<NumberedCard>) : Straight(
    straightCards,
    straightCards.minOf { it.value }
) {
    init {
        require(straightCards.size >= 5) { "There must be at least five cards" }
        val allSequential = straightCards.sortedWith(Comparator.naturalOrder())
            .zipWithNext { a, b -> a.value + 1 == b.value }
            .all { it }
        require(allSequential) { "The cards should be sequential" }
    }
}

data class PhoenixStraight(val straightCards: Set<NumberedCard>, val phoenix: Phoenix, val phoenixValue: PhoenixValue) : Straight(
    straightCards + setOf(phoenix),
    Integer.min(straightCards.minOf { it.value }, phoenixValue.value)
) {
    init {
        require(straightCards.size >= 4) { "There should be at least four cards and the Phoenix" }
        val allSequential = straightCards.sortedWith(Comparator.naturalOrder())
            .zipWithNext { a, b -> a.value + 1 == b.value || (a.value + 1 == phoenixValue.value && phoenixValue.value + 1 == b.value) }
            .all { it }
        require(allSequential) { "The cards must be sequential, with at most one gap (Phoenix)" }
    }
}

data class MahjongStraight(val straightCards: Set<NumberedCard>, val mahjong: Mahjong) : Straight(
    straightCards + setOf(mahjong),
    1
) {
    init {
        val sortedCards = straightCards.sortedWith(Comparator.naturalOrder())
        require(straightCards.size >= 4) { "There should be at least four cards and the Mahjong" }
        require(sortedCards.first().value == 2) { "The first card must be a two" }
        val allSequential = sortedCards
            .zipWithNext { a, b -> a.value + 1 == b.value }
            .all { it }
        require(allSequential) { "The cards must be sequential" }
    }
}

data class MahjongPhoenixStraight(val straightCards: Set<NumberedCard>, val mahjong: Mahjong, val phoenix: Phoenix, val phoenixValue: PhoenixValue) : Straight(
    straightCards + setOf(mahjong),
    1,
) {
    init {
        val sortedCards = straightCards.sortedWith(Comparator.naturalOrder())
        require(straightCards.size >= 3) { "There should be at least four cards and the Mahjong" }
        require(sortedCards.first().value == 2) { "The first card must be a two" }
        val allSequential = sortedCards
            .zipWithNext { a, b -> a.value + 1 == b.value || (a.value + 1 == phoenixValue.value && phoenixValue.value + 1 == b.value) }
            .all { it }
        require(allSequential) { "The cards must be sequential, with at most one gap (Phoenix)" }
    }
}

sealed class Bomb(cards: CardSet) : CardCombination(cards)
data class QuadrupleBomb(val bombCards: Set<NumberedCard>) : Bomb(bombCards) {
    val value = bombCards.minOf { it.value }

    init {
        require(bombCards.toSet().size == 4) { "There must be four cards" }
        val sameValue = bombCards
            .zipWithNext { a, b -> a.value == b.value }
            .all { it }
        require(sameValue) { "The cards must all be of the same value" }
    }
}

data class StraightBomb(val bombCards: Set<NumberedCard>) : Bomb(bombCards) {
    val value = bombCards.minOf { it.value }
    val length = bombCards.size

    init {
        val sortedCards = bombCards.sortedWith(Comparator.naturalOrder())
        require(sortedCards.size >= 5) { "There must be at least five cards" }
        val allSameSuitAndSequential = sortedCards
            .zipWithNext { a, b -> a.suit == b.suit && a.value + 1 == b.value }
            .all { it }
        require(allSameSuitAndSequential) { "The cards must of the same suit and sequential" }
    }
}

fun findCardCombination(cards: CardSet, addons: Set<PlayCardsAddon>): CardCombination? {
    val sortedCards = cards.sortedWith(Comparator.naturalOrder())

    val phoenix: Phoenix? = sortedCards.filterIsInstance<Phoenix>().firstOrNull()
    val mahjong: Mahjong? = sortedCards.filterIsInstance<Mahjong>().firstOrNull()
    val dog: Dog? = sortedCards.filterIsInstance<Dog>().firstOrNull()
    val dragon: Dragon? = sortedCards.filterIsInstance<Dragon>().firstOrNull()
    val phoenixValue: PhoenixValue? = addons.filterIsInstance<PhoenixValue>().firstOrNull()

    val numberedCards: List<NumberedCard> = sortedCards.filterIsInstance<NumberedCard>()
    val numberedCardCounts: Map<Int, Int> = numberedCards.groupBy { it.value }.mapValues { (_, items) -> items.size }

    return try {
        when {
            // A dog can only be played on its own
            dog != null && sortedCards.size == 1 -> HighCard(dog)
            dog != null -> null

            // A dragon can only be played on its own
            dragon != null && sortedCards.size == 1 -> HighCard(dragon)
            dragon != null -> null

            mahjong != null && sortedCards.size == 1 -> HighCard(mahjong)
            mahjong != null && phoenix != null && phoenixValue != null && sortedCards.size >= 5 -> MahjongPhoenixStraight(numberedCards.toSet(), mahjong, phoenix, phoenixValue)
            mahjong != null && numberedCards.size == sortedCards.size - 1 -> MahjongStraight(numberedCards.toSet(), mahjong)
            mahjong != null -> null

            phoenix != null && sortedCards.size == 1 -> HighCard(phoenix)
            phoenix != null && sortedCards.size == 2 -> PhoenixTuple(numberedCards[0], phoenix)
            phoenix != null && sortedCards.size == 3 -> PhoenixTriple(numberedCards[0], numberedCards[1], phoenix)
            phoenix != null && numberedCards.size >= 4 && phoenixValue != null && numberedCardCounts.all { (_, count) -> count == 1 } -> PhoenixStraight(
                numberedCards.toSet(),
                phoenix,
                phoenixValue
            )
            phoenix != null && sortedCards.size % 2 == 0 && numberedCardCounts.count { (_, count) -> count == 2 } == numberedCardCounts.size - 1 && numberedCardCounts.count { (_, count) -> count == 1 } == 1 -> TupleSequence(
                numberedCardCounts.map { (value, count) ->
                    when (count) {
                        1 -> PhoenixTuple(numberedCards.first { it.value == value }, phoenix)
                        2 -> numberedCards.filter { it.value == value }.let { cards -> NumberedTuple(cards[0], cards[1]) }
                        else -> throw IllegalStateException("Other counts than 1 and 2 should not be possible, found $count.")
                    }
                }
            )
            phoenix != null && numberedCards.size == 4 && numberedCardCounts.size == 2 -> {
                when (numberedCardCounts.minOf { (_, count) -> count }) {
                    // 1 + 3 + Phoenix combination
                    1 -> {
                        val single = numberedCardCounts.filterValues { it == 1 }.keys.first()
                        val triple = numberedCardCounts.filterValues { it == 3 }.keys.first()
                        FullHouse(
                            numberedCards.filter { it.value == single }.let { cards -> PhoenixTuple(cards[0], phoenix) },
                            numberedCards.filter { it.value == triple }.let { cards -> NumberedTriple(cards[0], cards[1], cards[2]) }
                        )
                    }
                    // 2 + 2 + Phoenix combination
                    2 -> {
                        val low = numberedCardCounts.keys.minOrNull()
                        val high = numberedCardCounts.keys.maxOrNull()
                        when (phoenixValue?.value) {
                            null -> null
                            low -> FullHouse(
                                numberedCards.filter { it.value == high }.let { cards -> NumberedTuple(cards[0], cards[1]) },
                                numberedCards.filter { it.value == low }.let { cards -> PhoenixTriple(cards[0], cards[1], phoenix) }
                            )
                            high -> FullHouse(
                                numberedCards.filter { it.value == low }.let { cards -> NumberedTuple(cards[0], cards[1]) },
                                numberedCards.filter { it.value == high }.let { cards -> PhoenixTriple(cards[0], cards[1], phoenix) }
                            )
                            else -> null
                        }
                    }
                    else -> null
                }
            }
            phoenix != null -> null

            cards.size == 1 -> HighCard(numberedCards[0])
            cards.size == 2 -> NumberedTuple(numberedCards[0], numberedCards[1])
            cards.size == 3 -> NumberedTriple(numberedCards[0], numberedCards[1], numberedCards[2])
            cards.size == 4 && numberedCardCounts.size == 1 -> QuadrupleBomb(numberedCards.toSet())
            cards.size == 5 && numberedCardCounts.size == 2 ->
                Pair(numberedCardCounts.filter { (_, count) -> count == 2 }.keys.first(), numberedCardCounts.filter { (_, count) -> count == 3 }.keys.first())
                    .let { (two, three) ->
                        FullHouse(
                            numberedCards.filter { it.value == two }.let { cards -> NumberedTuple(cards[0], cards[1]) },
                            numberedCards.filter { it.value == three }.let { cards -> NumberedTriple(cards[0], cards[1], cards[2]) }
                        )
                    }
            cards.size >= 4 && cards.size % 2 == 0 && numberedCardCounts.all { (_, count) -> count == 2 } ->
                TupleSequence(numberedCards.groupBy { it.value }.map { (_, cards) -> NumberedTuple(cards[0], cards[1]) })
            cards.size >= 5 && numberedCardCounts.all { (_, count) -> count == 1 } && numberedCards.groupBy { it.suit }.size == 1 -> StraightBomb(numberedCards.toSet())
            cards.size >= 5 && numberedCardCounts.all { (_, count) -> count == 1 } -> NumberedStraight(numberedCards.toSet())

            else -> null
        }
    } catch (illegalArguments: IllegalArgumentException) {
        null
    }
}
