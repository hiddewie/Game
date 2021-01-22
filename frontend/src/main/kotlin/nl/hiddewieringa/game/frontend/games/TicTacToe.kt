package nl.hiddewieringa.game.frontend.games

import kotlinx.html.js.onClickFunction
import react.RProps
import react.dom.*
import react.functionalComponent
import kotlin.js.Json
import kotlin.js.json

external interface TicTacToeProps : RProps {
    var gameState: Json?
    var dispatchAction: (event: Json) -> Unit
}

val emptyBoard = arrayOf(arrayOf<Json?>(null, null, null), arrayOf<Json?>(null, null, null), arrayOf<Json?>(null, null, null))

val TicTacToeComponent = functionalComponent<TicTacToeProps> { props ->

    val gameState = props.gameState?.get("board") as Array<Array<Json?>>? ?: emptyBoard
    val dispatchAction = props.dispatchAction
    console.info(gameState)

    val play = { x: Int, y: Int ->
        console.info("play", x, y)
        dispatchAction(json(
            "__type" to "nl.hiddewieringa.game.tictactoe.PlaceMarkLocation",
            "location" to json(
                "x" to x,
                "y" to y
            )
        ))
    }

    div {
        p {
            +"TaiPan"
        }
        table {
            tbody {
                (0 until 3).map { y ->
                    tr {
                        attrs.key = y.toString()
                        (0 until 3).map { x ->
                            td {
                                attrs.key = x.toString()
                                attrs.onClickFunction = { play(x, y) }

                                +when {
                                    gameState[x][y] == null -> "."
                                    gameState[x][y]!!["__type"] == "nl.hiddewieringa.game.tictactoe.Cross" -> "X"
                                    else -> "O"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}