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

class ExchangeCards(
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

data class TaiPanGameState(
    val playersToPlay: List<String>,
    val cards: List<Card>,
    val numberOfCardsPerPlayer: Map<String, Int>,
    val taiPannedPlayers: Map<String, String>,
    val cardsInGame: List<Card>,
    val score: Map<String, Int>,
    val roundIndex: Int?,
    val trickIndex: Int?,
)

private fun parseGameState(state: Json?) =
    TaiPanGameState(
        (state?.get("playersToPlay") as? Array<String> ?: emptyArray()).toList(),
        (state?.get("cards") as? Array<Array<Card>> ?: emptyArray()).map { it[1] },
        state?.get("numberOfCardsPerPlayer") as? Map<String, Int> ?: emptyMap(),
        state?.get("taiPannedPlayers") as? Map<String, String> ?: emptyMap(),
        (state?.get("cardsInGame") as? Array<Array<Card>> ?: emptyArray()).map { it[1] },
        state?.get("score") as? Map<String, Int> ?: emptyMap(),
        state?.get("roundIndex") as? Int?,
        state?.get("trickIndex") as? Int?,
    )


val TaiPanComponent = functionalComponent<TaiPanProps> { props ->
    val gameState = parseGameState(props.gameState)
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

    val playerId = gameState.playersToPlay

    div {
        +"Players to play ${playerId.joinToString(", ")}"

        ul {
            val cards = gameState.cards
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