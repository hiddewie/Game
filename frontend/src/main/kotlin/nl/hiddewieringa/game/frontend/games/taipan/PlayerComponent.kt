package nl.hiddewieringa.game.frontend.games.taipan

import kotlinx.css.*
import kotlinx.css.properties.deg
import kotlinx.css.properties.rotate
import kotlinx.css.properties.transform
import kotlinx.html.FormEncType
import kotlinx.html.classes
import kotlinx.html.js.onClickFunction
import nl.hiddewieringa.game.core.TwoTeamPlayerId
import nl.hiddewieringa.game.taipan.DragonPass
import nl.hiddewieringa.game.taipan.TaiPanStatus
import nl.hiddewieringa.game.taipan.card.Card
import nl.hiddewieringa.game.taipan.card.CardSet
import react.RProps
import react.child
import react.dom.button
import react.dom.div
import react.dom.span
import react.functionalComponent
import styled.css
import styled.styledDiv
import styled.styledImg

external interface PartialPlayerProps : RProps {
    var playerId: TwoTeamPlayerId
    var taiPanned: TaiPanStatus?
    var shouldPlay: Boolean // TODO modify to exchange controls or to play controls
}

external interface HiddenPlayerProps : PartialPlayerProps {
    var numberOfCards: Int
}

external interface PlayerProps : PartialPlayerProps {
    var cards: CardSet
    var exchangeCardLeft: (Card) -> Unit
    var exchangeCardForward: (Card) -> Unit
    var exchangeCardRight: (Card) -> Unit
}

val HiddenPlayerComponent = functionalComponent<HiddenPlayerProps> { props ->
    styledDiv {
        css {
            display = Display.inlineBlock
        }

        val taiPannedStatus = when (props.taiPanned) {
            null -> ""
            TaiPanStatus.NORMAL -> "★"
            TaiPanStatus.GREAT -> "★★"
        }
        +"Player ${props.playerId}: Tai pan: $taiPannedStatus. ${if (props.shouldPlay) "Should play" else "Should not play"}"

        div {
            child(EmptyCardListComponent) {
                attrs.count = props.numberOfCards
            }
        }
    }
}

val PlayerComponent = functionalComponent<PlayerProps> { props ->
    styledDiv {
        css {
            display = Display.inlineBlock
        }

        val taiPannedStatus = when (props.taiPanned) {
            null -> ""
            TaiPanStatus.NORMAL -> "★"
            TaiPanStatus.GREAT -> "★★"
        }
        +"Tai pan: $taiPannedStatus. ${if (props.shouldPlay) "Should play" else "Should not play"}"
        div {
            child(CardListComponent) {
                attrs.cards = props.cards
                attrs.hoverControls = { card ->
                    { builder ->
                        builder.span {
                            attrs.onClickFunction = { props.exchangeCardLeft(card) }
                            styledImg("Arrow left", "arrow.svg") {
                                css {
                                    width = 1.2.rem
                                    margin(0.4.rem, 0.0.rem)
                                    cursor = Cursor.pointer
                                    transform {
                                        rotate(90.deg)
                                    }
                                }
                            }
                        }
                        builder.span {
                            attrs.onClickFunction = { props.exchangeCardForward(card) }
                            styledImg("Arrow up", "arrow.svg") {
                                css {
                                    width = 1.2.rem
                                    margin(0.2.rem, 0.4.rem, 0.6.rem)
                                    cursor = Cursor.pointer
                                    transform {
                                        rotate(180.deg)
                                    }
                                }
                            }
                        }
                        builder.span {
                            attrs.onClickFunction = { props.exchangeCardRight(card) }
                            styledImg("Arrow right", "arrow.svg") {
                                css {
                                    width = 1.2.rem
                                    margin(0.4.rem, 0.0.rem)
                                    cursor = Cursor.pointer
                                    transform {
                                        rotate(270.deg)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
