package nl.hiddewieringa.game.taipan.card

typealias CardSet = Set<Card>

val numberedCards: CardSet = Suit.values().flatMap { suit -> (2..14).map { value -> NumberedCard(suit, value) } }.toSet()
val specialCards: CardSet = setOf(Dragon, Phoenix, Dog, Mahjong)
val fullDeck: CardSet = numberedCards + specialCards
