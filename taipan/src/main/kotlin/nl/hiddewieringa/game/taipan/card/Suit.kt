package nl.hiddewieringa.game.taipan.card

import kotlin.random.Random

typealias CardSet = Set<Card>

val numberedCards: CardSet = Suit.values().flatMap { suit -> (2..14).map { value -> NumberedCard(suit, value) } }.toSet()
val specialCards: CardSet = setOf(Dragon, Phoenix, Dog, Mahjong)
val fullSuit: CardSet = numberedCards + specialCards

fun <T> Random.shuffle(items: Iterable<T>): List<T> =
    items.map { nextInt() to it }
        .sortedBy { it.first }
        .map { it.second }
