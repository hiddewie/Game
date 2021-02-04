package nl.hiddewieringa.game.frontend.games

import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import react.RProps
import react.dom.div
import react.dom.key
import react.dom.tbody
import react.dom.tr
import react.functionalComponent
import styled.css
import styled.styledP
import styled.styledTable
import styled.styledTd
import kotlin.js.Json
import kotlin.js.json

external interface TicTacToeProps : RProps {
    var gameState: Json?
    var dispatchAction: (event: Json) -> Unit
    var playerId: String?
}

val emptyBoard = arrayOf(arrayOf<Json?>(null, null, null), arrayOf<Json?>(null, null, null), arrayOf<Json?>(null, null, null))

val TicTacToeComponent = functionalComponent<TicTacToeProps> { props ->

    val playerToPlay: String? = props.gameState?.get("playerToPlay") as String?
    val board: Array<Array<Json?>> = props.gameState?.get("board") as Array<Array<Json?>>? ?: emptyBoard
    val playerWon: String? = props.gameState?.get("playerWon") as String?
    val gameFinished: Boolean = props.gameState?.get("gameFinished") as Boolean? ?: false

    val dispatchAction = props.dispatchAction
    val playerId = props.playerId

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
        styledP {
            css {
                textAlign = TextAlign.center
            }

            if (playerWon != null) {
                if (playerWon == playerId) {
                    +"You won!"
                } else {
                    +"$playerWon won!"
                }
            } else if (playerToPlay != null) {
                if (playerToPlay == playerId) {
                    +"You play!"
                } else {
                    +"Current player: $playerToPlay"
                }
            }
        }

        if (!gameFinished) {
            styledTable {
                css {
                    width = 300.px
                    margin = "1rem auto"
                    border = "3px solid #333"
                    borderRadius = 3.px
                    borderCollapse = BorderCollapse.collapse
                }
                tbody {
                    (0 until 3).map { y ->
                        tr {
                            attrs.key = y.toString()
                            (0 until 3).map { x ->
                                styledTd {
                                    css {
                                        height = 100.px
                                        width = 100.px
                                        border = "3px solid #333"
                                        textAlign = TextAlign.center
                                        fontFamily = "sans-serif"
                                        fontSize = 48.px
                                    }
                                    attrs.key = x.toString()
                                    attrs.onClickFunction = { play(x, y) }

                                    +when {
                                        board[x][y] == null -> ""
                                        board[x][y]!!["__type"] == "nl.hiddewieringa.game.tictactoe.Cross" -> "×"
                                        else -> "⊙"
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