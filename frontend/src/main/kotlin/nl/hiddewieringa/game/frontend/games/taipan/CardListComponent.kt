package nl.hiddewieringa.game.frontend.games.taipan

import kotlinx.css.*
import nl.hiddewieringa.game.taipan.card.Card
import nl.hiddewieringa.game.taipan.card.CardSet
import react.RBuilder
import react.RProps
import react.child
import react.dom.div
import react.functionalComponent
import styled.css
import styled.styledDiv
import kotlin.Float

external interface CardListProps : RProps {
    var cards: Iterable<Card>
    var selectedCards: CardSet
    var hoverControls: ((Card) -> (RBuilder) -> Unit)?
    var small: Boolean
    var cardSelected: (Card) -> Unit
    var cardDeselected: (Card) -> Unit
}

val cardListRuleSet: (Float, Boolean) -> RuleSet = { index, large ->
    {
//        transform {
//            val rotation = -index * 3
//            val x = index * 35
//            translateX(x.px)
//            translateY((-0.7 * tan(rotation / 180 * PI) * x).px)
//            rotate(rotation.deg)
//        }
//        display = Display.inlineBlock
//        flex(1.0, if (large) 1.0 else 0.0)
    }
}
val CardListComponent = functionalComponent<CardListProps> { props ->
    // TODO store custom card order for manually sorted hand
    val cards = props.cards.sorted()
//    val count = cards.size // <= 14
//    val middleIndex: Float = (count - 1).toFloat() / 2 // <= 6.5

    styledDiv {
        css {
//            justify-content: space-between;

            display = Display.inlineFlex
            justifyContent = JustifyContent.center

            paddingTop = 2.rem
//            put("flex-flow", "wrap")
//            paddingRight = 45.px
        }

        cards.mapIndexed { index, card ->
//            val indexFromMiddle = middleIndex - index.toFloat()
//            styledDiv {
//                css(cardListRuleSet(indexFromMiddle, index == count - 1))
            child(CardComponent) {
                attrs.card = card
                attrs.small = props.small
                attrs.selected = props.selectedCards.contains(card)
                attrs.content = null
                attrs.hoverControls = props.hoverControls?.let { controls -> controls(card) }
                attrs.cardSelected = { props.cardSelected(card) }
                attrs.cardDeselected = { props.cardDeselected(card) }
            }
//            }
        }
    }

    div {
        +"${cards.size} cards, ${cards.sumBy { it.points }} points"
    }
}

external interface EmptyCardListProps : RProps {
    var count: Int
}

val EmptyCardListComponent = functionalComponent<EmptyCardListProps> { props ->
    val count = props.count
    val middleIndex: Float = (count - 1).toFloat() / 2 // <= 6.5

    styledDiv {
        css {
            display = Display.inlineFlex
//            justifyContent = JustifyContent.spaceBetween
            put("flex-flow", "wrap")
//            paddingRight = 30.px
        }

        repeat(count) { index ->
            val indexFromMiddle = middleIndex - index.toFloat()
//            styledDiv {
//                css(cardListRuleSet(indexFromMiddle, index == count - 1))
            child(EmptyCardComponent) {
                attrs.small = true
                attrs.hoverControls = attrs.hoverControls

                if (index == count - 1) {
                    attrs.content = count.toString()
                }
            }
//            }
        }
    }
}
