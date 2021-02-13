package nl.hiddewieringa.game.frontend.games

import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import nl.hiddewieringa.game.frontend.serializer
import nl.hiddewieringa.game.tictactoe.Cross
import nl.hiddewieringa.game.tictactoe.GameMark
import nl.hiddewieringa.game.tictactoe.Location
import nl.hiddewieringa.game.tictactoe.PlaceMarkLocation
import nl.hiddewieringa.game.tictactoe.state.TicTacToePlayerState
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

external interface TicTacToeProps : RProps {
    var gameState: String? // Json string
    var dispatchAction: (event: String) -> Unit // Json encoded action
    var playerId: String?
}

val emptyBoard = arrayOf(arrayOf<GameMark?>(null, null, null), arrayOf<GameMark?>(null, null, null), arrayOf<GameMark?>(null, null, null))

val TicTacToeComponent = functionalComponent<TicTacToeProps> { props ->
    val gameState = props.gameState?.let { serializer.decodeFromString(TicTacToePlayerState.serializer(), it) }

    val playerToPlay = gameState?.playerToPlay
    val board = gameState?.board ?: emptyBoard
    val playerWon = gameState?.playerWon
    val gameFinished = gameState?.gameFinished ?: false

    val dispatchAction = props.dispatchAction
    val playerId = props.playerId

    val play = { x: Int, y: Int ->
        console.info("play", x, y)
        val action = PlaceMarkLocation(Location(x, y))
        dispatchAction(serializer.encodeToString(PlaceMarkLocation.serializer(), action))
    }

    div {
        styledP {
            css {
                textAlign = TextAlign.center
            }

            if (playerWon != null) {
                if (playerWon.toString() == playerId) {
                    +"You won!"
                } else {
                    +"$playerWon won!"
                }
            } else if (playerToPlay != null) {
                if (playerToPlay.toString() == playerId) {
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
                                        board[x][y] is Cross -> "×"
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