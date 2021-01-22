package nl.hiddewieringa.game.frontend.games

import kotlinx.html.js.onClickFunction
import react.RProps
import react.dom.button
import react.dom.div
import react.dom.h5
import react.dom.p
import react.functionalComponent
import kotlin.js.Json
import kotlin.js.json

external interface TaiPanProps : RProps {
    var gameState: Json?
    var dispatchAction: (event: Json) -> Unit
}

private fun typed(type: String, map: Json): Json {
    map["__type"] = type
    return map
}

private fun typed(type: String): Json =
    typed(type, json())

val TaiPanComponent = functionalComponent<TaiPanProps> { props ->
    val gameState = props.gameState
    val dispatchAction = props.dispatchAction

    console.info(gameState)

    val callTaiPan = {
        dispatchAction(typed("nl.hiddewieringa.game.taipan.CallTaiPan"))
    }

    val fold = {
        dispatchAction(typed("nl.hiddewieringa.game.taipan.Fold"))
    }

    val requestNextCards = {
        dispatchAction(typed("nl.hiddewieringa.game.taipan.RequestNextCards"))
    }

    val passDragonTrick = { dragonPass: Any ->
        dispatchAction(typed("nl.hiddewieringa.game.taipan.PassDragonTrick", json("dragonPass" to dragonPass)))
    }

    val playCards = { cards: Any, addons: Any ->
        dispatchAction(typed("nl.hiddewieringa.game.taipan.PlayCards", json("cards" to cards, "addons" to addons)))
    }

    val passCards = { left: Any, middle: Any, right: Any ->
        dispatchAction(typed("nl.hiddewieringa.game.taipan.CardPass", json("left" to left, "middle" to middle, "right" to right)))
    }

    div {
        p {
            +"TaiPan"
        }
        +(gameState?.get("@type")?.toString() ?: "no type")
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
                attrs.onClickFunction = { passDragonTrick(1) }
                +"Pass dragon trick"
            }
            button {
                attrs.onClickFunction = { playCards(1, 1) }
                +"Play cards"
            }
            button {
                attrs.onClickFunction = { passCards(1, 1, 1) }
                +"Pass cards"
            }
        }
    }
}