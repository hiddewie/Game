package nl.hiddewieringa.game.frontend.games

import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLInputElement
import react.RProps
import react.dom.*
import react.functionalComponent
import react.useState
import kotlin.js.Json
import kotlin.js.json

external interface Card {
    val __type: String
    val value: Int?
    val suit: String?
    val points: Int
}

external interface TaiPanProps : RProps {
    var gameState: Json?
    var dispatchAction: (event: Json) -> Unit
    var playerId: String?
}

class ExchangeCards (
    val left: Card?,
    val forward: Card?,
    val right: Card?,
)

private fun typed(type: String, map: Json): Json {
    map["__type"] = type
    return map
}

private fun typed(type: String): Json =
    typed(type, json())

val TaiPanComponent = functionalComponent<TaiPanProps> { props ->
    val gameState = props.gameState
    val dispatchAction = props.dispatchAction

    val (selectedCards, setSelectedCards) = useState(emptySet<Card>())
    val (dragonPass, setDragonPass) = useState("LEFT")
    val (exchangeCards, setExchangeCards) = useState(ExchangeCards(null, null, null))

    console.info(gameState, selectedCards.joinToString(", "))

    val callTaiPan = {
        dispatchAction(typed("nl.hiddewieringa.game.taipan.CallTaiPan"))
    }

    val fold = {
        dispatchAction(typed("nl.hiddewieringa.game.taipan.Fold"))
    }

    val requestNextCards = {
        dispatchAction(typed("nl.hiddewieringa.game.taipan.RequestNextCards"))
    }

    val passDragonTrick = {
        dispatchAction(typed("nl.hiddewieringa.game.taipan.PassDragonTrick", json("dragonPass" to dragonPass)))
    }

    val playCards = {
        // TODO check if any cards selected
        // TODO gather addons
        val addons = emptyArray<Any?>()
        dispatchAction(typed("nl.hiddewieringa.game.taipan.PlayCards", json("cards" to arrayOf("java.util.Set", selectedCards.toTypedArray()), "addons" to arrayOf("java.util.Set", addons))))
    }

    val passCards = {
        // TODO check if exchange not empty
        dispatchAction(typed("nl.hiddewieringa.game.taipan.CardPass", json("cardPass" to json("left" to exchangeCards.left, "forward" to exchangeCards.forward, "right" to exchangeCards.right))))
    }

    val selectedCard = { card: Card ->
        setSelectedCards(selectedCards.plusElement(card))
    }
    val deselectedCard = { card: Card ->
        setSelectedCards(selectedCards.filterNot { it.__type == card.__type && it.value == card.value && it.suit == card.suit }.toSet())
    }

    if (gameState == null) {
        p {
            +"TaiPan"
            +"No state"
        }
        return@functionalComponent
    }

    val playerId = gameState["playerId"]

    div {
        p {
            +"TaiPan"
        }
        +"Type ${gameState["__type"]?.toString() ?: "no type"}"
        +"Player Id $playerId"

        ul {
            val cards = gameState["cards"] as? Array<Array<Card>>
            cards?.get(1)?.map { card ->
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

                    +card.__type
                    +(card.value?.toString() ?: "no value")
                    +(card.suit ?: "no suit")
                    +card.points.toString()

                    label {
                        attrs.htmlFor = "exchangeLeft${card.__type}${card.suit}${card.value}"
                        input {
                            attrs.id = "exchangeLeft${card.__type}${card.suit}${card.value}"
                            attrs.type = InputType.radio
                            attrs.name = "exchangeLeft"
                            attrs.onChangeFunction = { setExchangeCards(ExchangeCards(card, exchangeCards.forward, exchangeCards.right)) }
                        }
                        +"exchange left"
                    }
                    label {
                        attrs.htmlFor = "exchangeForward${card.__type}${card.suit}${card.value}"
                        input {
                            attrs.id = "exchangeForward${card.__type}${card.suit}${card.value}"
                            attrs.type = InputType.radio
                            attrs.name = "exchangeForward"
                            attrs.onChangeFunction = { setExchangeCards(ExchangeCards(exchangeCards.left, card, exchangeCards.right)) }
                        }
                        +"exchange forward"
                    }
                    label {
                        attrs.htmlFor = "exchangeRight${card.__type}${card.suit}${card.value}"
                        input {
                            attrs.id = "exchangeRight${card.__type}${card.suit}${card.value}"
                            attrs.type = InputType.radio
                            attrs.name = "exchangeRight"
                            attrs.onChangeFunction = { setExchangeCards(ExchangeCards(exchangeCards.left, exchangeCards.forward, card)) }
                        }
                        +"exchange right"
                    }
                }
            }
        }

        p {
            +"Dragon pass"

            arrayOf(
                "LEFT",
                "RIGHT"
            ).map { dragonPassValue ->
                label {
                    attrs.htmlFor = dragonPassValue
                    input {
                        attrs.id = dragonPassValue
                        attrs.type = InputType.radio
                        attrs.name = "dragonPassValue"
                        attrs.onChangeFunction = {
                            setDragonPass(dragonPassValue)
                        }
                    }
                    +dragonPassValue
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