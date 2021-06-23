package nl.hiddewieringa.game.frontend.games.taipan

import kotlinx.css.*
import kotlinx.css.properties.deg
import kotlinx.css.properties.rotate
import kotlinx.css.properties.transform
import kotlinx.html.classes
import kotlinx.html.js.onClickFunction
import nl.hiddewieringa.game.taipan.card.Card
import react.RProps
import react.child
import react.dom.button
import react.dom.div
import react.functionalComponent
import styled.css
import styled.styledDiv

external interface ExchangeCardsProps : RProps {
    var left: Card?
    var forward: Card?
    var right: Card?
    var exchange: () -> Unit
}

val ExchangeCardsComponent = functionalComponent<ExchangeCardsProps> { props ->
    val left = props.left
    val forward = props.forward
    val right = props.right

    div {
        +"Passed cards"
        styledDiv {
            css {
                display = Display.flex
                justifyContent = JustifyContent.spaceEvenly
                padding(3.rem, 2.rem, 1.rem)
            }

            styledDiv {
                css {
                    transform {
                        rotate((-15).deg)
                    }
                    put("transform-origin", "bottom")
                }

                if (left != null) {
                    child(CardComponent) {
                        attrs.card = left
                        attrs.cardSelected = {}
                        attrs.cardDeselected = {}
                        attrs.canSelect = false
                    }
                } else {
                    child(EmptyCardComponent)
                }
            }
            styledDiv {
                css {
                    marginTop = (-3).rem
                }

                if (forward != null) {
                    child(CardComponent) {
                        attrs.card = forward
                        attrs.cardSelected = {}
                        attrs.cardDeselected = {}
                        attrs.canSelect = false
                    }
                } else {
                    child(EmptyCardComponent)
                }
            }
            styledDiv {
                css {
                    transform {
                        rotate((15).deg)
                    }
                    put("transform-origin", "bottom")
                }

                if (right != null) {
                    child(CardComponent) {
                        attrs.card = right
                        attrs.cardSelected = {}
                        attrs.cardDeselected = {}
                        attrs.canSelect = false
                    }
                } else {
                    child(EmptyCardComponent)
                }
            }
        }

        styledDiv {
            css {
                display = Display.flex
                justifyContent = JustifyContent.center
            }

            button {
                attrs.classes = setOf("uk-button", "uk-button-primary")
                attrs.disabled = left == null || forward == null || right == null
                attrs.onClickFunction = { props.exchange() }

                +"Exchange cards"
            }
        }
    }
}
