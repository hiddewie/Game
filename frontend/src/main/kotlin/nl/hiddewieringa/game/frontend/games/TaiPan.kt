package nl.hiddewieringa.game.frontend.games

import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import nl.hiddewieringa.game.frontend.serializer
import nl.hiddewieringa.game.taipan.*
import nl.hiddewieringa.game.taipan.card.Card
import nl.hiddewieringa.game.taipan.card.NumberedCard
import nl.hiddewieringa.game.taipan.card.ThreeWayPass
import nl.hiddewieringa.game.taipan.state.TaiPanPlayerState
import org.w3c.dom.HTMLInputElement
import react.RProps
import react.dom.*
import react.functionalComponent
import react.useState

external interface TaiPanProps : RProps {
    var gameState: String? // Json string
    var dispatchAction: (event: String) -> Unit // Json encoded action
    var playerId: String?
}

val TaiPanComponent = functionalComponent<TaiPanProps> { props ->
    val gameState = props.gameState?.let { serializer.decodeFromString(TaiPanPlayerState.serializer(), it) }
    val dispatchAction = props.dispatchAction

    val (selectedCards, setSelectedCards) = useState(emptySet<Card>())
    val (dragonPass, setDragonPass) = useState<DragonPass?>(null)
    val (exchangeCards, setExchangeCards) = useState<Triple<Card?, Card?, Card?>>(Triple(null, null, null))

    console.info(gameState, selectedCards.joinToString(", "))

    val callTaiPan = {
        dispatchAction(serializer.encodeToString(CallTaiPan.serializer(), CallTaiPan))
    }

    val fold = {
        dispatchAction(serializer.encodeToString(Fold.serializer(), Fold))
    }

    val requestNextCards = {
        dispatchAction(serializer.encodeToString(RequestNextCards.serializer(), RequestNextCards))
    }

    val passDragonTrick = {
        if (dragonPass != null) {
            dispatchAction(serializer.encodeToString(PassDragonTrick.serializer(), PassDragonTrick(dragonPass)))
        } else {
            // TODO
            console.error("Not implemented")
        }
    }

    val playCards = {
        if (selectedCards.isNotEmpty()) {
            // TODO gather addons
            val addons = emptySet<PlayCardsAddon>()

            dispatchAction(serializer.encodeToString(PlayCards.serializer(), PlayCards(selectedCards, addons)))
        } else {
            // TODO
            console.error("Not implemented")
        }
    }

    val passCards = {
        val (left, forward, right) = exchangeCards
        if (left != null && forward != null && right != null) {
            dispatchAction(serializer.encodeToString(CardPass.serializer(), CardPass(ThreeWayPass(left, forward, right))))
        } else {
            // TODO
            console.error("Not implemented")
        }
    }

    val selectedCard = { card: Card ->
        setSelectedCards(selectedCards.plusElement(card))
    }
    val deselectedCard = { card: Card ->
        setSelectedCards(selectedCards.filter { it != card }.toSet())
    }

    val playerId = gameState?.playersToPlay ?: emptyList()

    div {
        +"Players to play ${playerId.joinToString(", ")}"

        ul {
            val cards = gameState?.cards ?: emptyList()
            cards.map { card ->
                li {
                    input {
                        attrs.type = InputType.checkBox
                        attrs.onChangeFunction = { event ->
                            if ((event.target as HTMLInputElement).checked) {
                                selectedCard(card)
                            } else {
                                deselectedCard(card)
                            }
                        }
                    }

                    if (card is NumberedCard) {
                        +card.toString()
                        +"$card: ${card.suit} ${card.value}"
                    }
                    +" ${card.points}"

                    val uniqueString = "${card::class.simpleName}${if (card is NumberedCard) card.suit else null}${if (card is NumberedCard) card.value else null}"

                    label {
                        attrs.htmlFor = "exchangeLeft$uniqueString"
                        input {
                            attrs.id = "exchangeLeft$uniqueString"
                            attrs.type = InputType.radio
                            attrs.name = "exchangeLeft"
                            attrs.onChangeFunction = { setExchangeCards(Triple(card, exchangeCards.second, exchangeCards.third)) }
                        }
                        +"exchange left"
                    }
                    label {
                        attrs.htmlFor = "exchangeForward$uniqueString"
                        input {
                            attrs.id = "exchangeForward$uniqueString"
                            attrs.type = InputType.radio
                            attrs.name = "exchangeForward"
                            attrs.onChangeFunction = { setExchangeCards(Triple(exchangeCards.first, card, exchangeCards.third)) }
                        }
                        +"exchange forward"
                    }
                    label {
                        attrs.htmlFor = "exchangeRight$uniqueString"
                        input {
                            attrs.id = "exchangeRight$uniqueString"
                            attrs.type = InputType.radio
                            attrs.name = "exchangeRight"
                            attrs.onChangeFunction = { setExchangeCards(Triple(exchangeCards.first, exchangeCards.second, card)) }
                        }
                        +"exchange right"
                    }
                }
            }
        }

        p {
            +"Dragon pass"

            DragonPass.values().map { dragonPassValue ->
                label {
                    attrs.htmlFor = dragonPassValue.toString()
                    input {
                        attrs.id = dragonPassValue.toString()
                        attrs.type = InputType.radio
                        attrs.name = "dragonPassValue"
                        attrs.onChangeFunction = {
                            setDragonPass(dragonPassValue)
                        }
                    }
                    +dragonPassValue.toString()
                }
            }
        }

        div {
            h5 {
                +"Actions"
            }

            button {
                attrs.onClickFunction = { callTaiPan() }
                +"Call tai pan"
            }
            button {
                attrs.onClickFunction = { fold() }
                +"Fold"
            }
            button {
                attrs.onClickFunction = { requestNextCards() }
                +"Request next cards"
            }
            button {
                attrs.onClickFunction = { passDragonTrick() }
                +"Pass dragon trick"
            }
            button {
                attrs.onClickFunction = { playCards() }
                +"Play cards"
            }
            button {
                attrs.onClickFunction = { passCards() }
                +"Pass cards"
            }
        }
    }
}