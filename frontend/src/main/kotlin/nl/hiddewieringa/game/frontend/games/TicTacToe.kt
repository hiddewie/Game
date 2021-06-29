package nl.hiddewieringa.game.frontend.games

import kotlinx.css.*
import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction
import nl.hiddewieringa.game.frontend.GameUiProps
import nl.hiddewieringa.game.frontend.ParametersProps
import nl.hiddewieringa.game.tictactoe.*
import nl.hiddewieringa.game.tictactoe.state.TicTacToePlayerState
import react.dom.*
import react.functionalComponent
import styled.css
import styled.styledP
import styled.styledTable
import styled.styledTd

val TicTacToeParametersComponent = functionalComponent<ParametersProps<TicTacToeGameParameters>> { props ->
    val startGame = props.startGame

    button(null, null, ButtonType.button, "uk-button uk-button-primary") {
        attrs.onClickFunction = { startGame(TicTacToeGameParameters.serializer(), TicTacToeGameParameters) }
        +"Start a new game"
    }
}

val emptyBoard = arrayOf(arrayOf<GameMark?>(null, null, null), arrayOf<GameMark?>(null, null, null), arrayOf<GameMark?>(null, null, null))

val TicTacToeComponent = functionalComponent<GameUiProps<TicTacToePlayerState, TicTacToePlayerActions>> { props ->
    val gameState = props.gameState

    val playerToPlay = gameState?.playerToPlay
    val board = gameState?.board ?: emptyBoard
    val playerWon = gameState?.playerWon
    val gameFinished = gameState?.gameFinished ?: false

    val dispatchAction = props.dispatchAction
    val playerId = props.playerId

    val play = { x: Int, y: Int ->
        console.info("play", x, y)
        val action = PlaceMarkLocation(Location(x, y))
        dispatchAction.dispatch(action)
    }

    div {
        styledP {
            css {
                textAlign = TextAlign.center
            }

            if (gameFinished) {
                +when {
                    playerWon == null -> "Nobody won: drow"
                    playerWon.toString() == playerId -> "You won!"
                    else -> "$playerWon won!"
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