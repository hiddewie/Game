package nl.hiddewieringa.game.frontend.games.taipan

import kotlinx.css.*
import kotlinx.css.properties.LineHeight
import kotlinx.css.properties.boxShadow
import kotlinx.css.properties.lh
import kotlinx.html.classes
import kotlinx.html.js.onClickFunction
import nl.hiddewieringa.game.taipan.card.*
import react.RBuilder
import react.RProps
import react.functionalComponent
import styled.css
import styled.styledDiv

val cardCssRuleSet: (Boolean, Boolean, Boolean) -> RuleSet = { small, selected, showHover ->
    {
        height = if (small) 60.px else 120.px
        width = if (small) 40.px else 80.px
        minWidth = if (small) 10.px else 35.px
        flex(0.0, 1.0, width)
        lineHeight = LineHeight(height.toString())
        verticalAlign = VerticalAlign.top
        textAlign = TextAlign.center
        backgroundPosition = "0 0"
        display = Display.inlineBlock
        border = "1px solid #666"
        borderRadius = 3.px
        marginTop = if (selected) (-2).rem else 0.rem
        boxShadow(rgba(0, 0, 0, 0.1), (-4).px, 0.px, 4.px, 0.px)

        if (showHover) {
            hover {
                boxShadow(rgba(0, 0, 0, 0.25), (-4).px, 0.px, 4.px, 0.px)
                borderTopLeftRadius = 0.px
                borderTopRightRadius = 0.px
            }
        }

        not(":last-child") {
            marginRight = if (small) (-30).px else (-45).px
        }
    }
}

external interface PartialCardProps : RProps {
    var small: Boolean
    var content: String?
    var hoverControls: ((RBuilder) -> Unit)?
}

external interface CardProps : PartialCardProps {
    var card: Card
    var canSelect: Boolean
    var selected: Boolean
    var cardSelected: () -> Unit
    var cardDeselected: () -> Unit
}

val CardComponent = functionalComponent<CardProps> { props ->
    // TODO replace with 'normal' class names
    val cardClass = when (val card = props.card) {
        is NumberedCard -> {
            val suitValue = when (card.suit) {
                Suit.HEARTS -> 0
                Suit.DIAMONDS -> 1
                Suit.SPADES -> 2
                Suit.CLUBS -> 3
            }
            "c${suitValue}-${card.value}"
        }
        Dragon -> "c6-15"
        Phoenix -> "c7-0"
        Dog -> "c5-0"
        Mahjong -> "c4-1"
    }

    styledDiv {
        css {
            position = Position.relative

            hover {
                child(".controls") {
                    display = Display.block
                }
            }
        }
        css(cardCssRuleSet(props.small, props.selected, props.hoverControls != null))

        attrs.classes = setOf(cardClass)
        if (props.canSelect) {
            attrs.onClickFunction = {
                if (props.selected) {
                    props.cardDeselected()
                } else {
                    props.cardSelected()
                }
            }
        }

        val hoverControls = props.hoverControls
        if (hoverControls != null) {
            styledDiv {
                attrs.classes = setOf("controls")
                css {
                    display = Display.none

                    position = Position.absolute
                    top = (-2).rem
                    height = 2.rem
                    left = (-1).px
                    right = (-1).px
                    lineHeight = 2.rem.lh
//                    marginLeft = if (props.small) (-30).px else (-45).px

                    background = "white"
                    border = "1px solid #666"
                    borderRadius = 3.px
                    borderBottomRightRadius = 0.px
                    borderBottomLeftRadius = 0.px
                    borderBottom = "none"
                    boxShadow(rgba(0, 0, 0, 0.1), (-4).px, (-4).px, 4.px, 0.px)

                    hover {
                        display = Display.block
                    }
                }

                hoverControls(this)
            }
        }
        if (props.content != null) {
            +props.content.toString()
        }
    }
}

val EmptyCardComponent = functionalComponent<PartialCardProps> { props ->
    styledDiv {
        attrs.classes = setOf("blankcard")
        css(cardCssRuleSet(props.small, false, props.hoverControls != null))
        if (props.content != null) {
            +props.content.toString()
        }
    }
}
