package nl.hiddewieringa.game.frontend.games

import kotlinx.css.*
import kotlinx.css.properties.deg
import kotlinx.css.properties.rotate
import kotlinx.css.properties.transform
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import nl.hiddewieringa.game.core.TwoTeamPlayerId
import nl.hiddewieringa.game.frontend.GameUiProps
import nl.hiddewieringa.game.frontend.games.taipan.CardListComponent
import nl.hiddewieringa.game.frontend.games.taipan.ExchangeCardsComponent
import nl.hiddewieringa.game.frontend.games.taipan.HiddenPlayerComponent
import nl.hiddewieringa.game.frontend.games.taipan.PlayerComponent
import nl.hiddewieringa.game.taipan.*
import nl.hiddewieringa.game.taipan.card.Card
import nl.hiddewieringa.game.taipan.card.NumberedCard
import nl.hiddewieringa.game.taipan.card.ThreeWayPass
import nl.hiddewieringa.game.taipan.state.TaiPanPlayerState
import nl.hiddewieringa.game.taipan.state.TaiPanPlayerStateType
import org.w3c.dom.HTMLInputElement
import react.child
import react.dom.*
import react.functionalComponent
import react.useState
import styled.*

// TODO implement auto-fold with random delay

val TaiPanComponent = functionalComponent<GameUiProps<TaiPanPlayerState, TaiPanPlayerActions>> { props ->
    val gameState = props.gameState
    val dispatchAction = props.dispatchAction
    val playerId = props.playerId?.let { TwoTeamPlayerId.valueOf(it) }

    val (selectedCards, setSelectedCards) = useState(emptySet<Card>())
    val (exchangeCards, setExchangeCards) = useState<Triple<Card?, Card?, Card?>>(Triple(null, null, null))

    console.info(gameState, selectedCards.joinToString(", "))

    val callTaiPan = {
        dispatchAction.dispatch(CallTaiPan)
    }

    val fold = {
        dispatchAction.dispatch(Fold)
    }

    val requestNextCards = {
        dispatchAction.dispatch(RequestNextCards)
    }

    val passDragonTrick = { dragonPass: DragonPass ->
        dispatchAction.dispatch(PassDragonTrick(dragonPass))
    }

    val playCards = {
        if (selectedCards.isNotEmpty()) {
            // TODO gather addons
            val addons = emptySet<PlayCardsAddon>()

            dispatchAction.dispatch(PlayCards(selectedCards, addons))
        } else {
            // TODO
            console.error("Not implemented")
        }
    }

    val passCards = {
        val (left, forward, right) = exchangeCards
        if (left != null && forward != null && right != null) {
            dispatchAction.dispatch(CardPass(ThreeWayPass(left, forward, right)))
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

    if (playerId == null || gameState == null) {
        p {
            +"No player ID or game state"
        }
        return@functionalComponent
    }

    styledDiv {
        // score
        // TODO to component
        styledDiv {
            css { }

            val points = gameState.points
            styledTable {
                css {
                    margin = "0 auto"
                    put("font-variant", "small-caps")
                }

                tbody {
                    tr {
                        styledTd {
                            css {
                                textAlign = TextAlign.right
                                paddingRight = 1.rem
                            }
                            +"you"
                        }
                        td {
                            +"them"
                        }
                    }
                    styledTr {
                        css {
                            fontSize = (1.5).rem
                        }
                        styledTd {
                            css {
                                textAlign = TextAlign.right
                                paddingRight = 1.rem
                            }
                            +points[playerId.team].toString()
                        }
                        td {
                            +points[playerId.team.otherTeam()].toString()
                        }
                    }
                }
            }

            val round = gameState.roundIndex
            val trick = gameState.trickIndex
            styledDiv {
                css {
                    textAlign = TextAlign.center
                    put("font-variant", "small-caps")
                }
                if (round != null && trick != null) {
                    +"Round $round ${Typography.mdash} Trick $trick"
                }
            }
        }

        styledDiv {
            css {}
            +"Played cards: ${gameState.playedCards.joinToString(" - ")}"
        }

        // partner
        styledDiv {
            css {
                textAlign = TextAlign.center
            }

            val partnerPlayerId = nextPlayer(nextPlayer(playerId))
            child(HiddenPlayerComponent) {
                attrs.playerId = partnerPlayerId
                attrs.taiPanned = gameState.taiPannedPlayers[partnerPlayerId]
                attrs.shouldPlay = gameState.playersToPlay.contains(partnerPlayerId)
                attrs.numberOfCards = gameState.numberOfCardsPerPlayer[partnerPlayerId] ?: 0
            }
        }

        styledDiv {
            css {
                display = Display.flex
            }

            // left & right
            styledDiv {
                css {
                    flex(1.0, 1.0, 350.px)

                    borderLeft = "3px solid blue" // TODO color
                    paddingLeft = 1.rem
                }

                val leftPlayerId = nextPlayer(playerId)
                child(HiddenPlayerComponent) {
                    attrs.playerId = leftPlayerId
                    attrs.taiPanned = gameState.taiPannedPlayers[leftPlayerId]
                    attrs.shouldPlay = gameState.playersToPlay.contains(leftPlayerId)
                    attrs.numberOfCards = gameState.numberOfCardsPerPlayer[leftPlayerId] ?: 0
                }
            }

            styledDiv {
                css {
                    flex(1.0, 1.0, 50.pct)

                    display = Display.flex
                    flexDirection = FlexDirection.column
                }

                styledDiv {
                    css {
                        flex(0.0, 1.0)

                        textAlign = TextAlign.center
                    }
                    styledImg("Arrow up", "arrow.svg") {
                        css {
                            width = 2.rem
                            transform {
                                rotate(180.deg)
                            }
                        }
                    }
                }
                styledDiv {
                    css {
                        flex(1.0, 0.0)

                        display = Display.flex
                    }

                    styledDiv {
                        css {
                            flex(0.0, 0.0, 2.rem)
                            display = Display.flex
                        }
                        styledImg("Arrow left", "arrow.svg") {
                            css {
                                width = 2.rem
                                transform {
                                    rotate(90.deg)
                                }
                            }
                        }
                    }
                    styledDiv {
                        css {
                            flex(1.0, 1.0)

                            minHeight = 400.px
                            display = Display.flex
                            flexDirection = FlexDirection.column
                            justifyContent = JustifyContent.center
                        }

                        when (gameState.stateType) {
                            TaiPanPlayerStateType.RECEIVE_CARDS -> {
                                if (gameState.cards.size < 14) {
                                    +"Receive next cards or tai pan"
                                    button {
                                        attrs.onClickFunction = { requestNextCards() }
                                        +"Receive next cards"
                                    }
                                }
                            }
                            TaiPanPlayerStateType.EXCHANGE_CARDS -> {
                                if (gameState.playersToPlay.contains(playerId)) {
                                    child(ExchangeCardsComponent) {
                                        attrs.left = exchangeCards.first
                                        attrs.forward = exchangeCards.second
                                        attrs.right = exchangeCards.third
                                        attrs.exchange = { passCards() }
                                    }
                                } else {
                                    +"Wait for the other players to exchange"
                                }
                            }
                            TaiPanPlayerStateType.PASS_DRAGON -> {
                                button {
                                    attrs.onClickFunction = { passDragonTrick(DragonPass.LEFT) }
                                    +"Pass dragon left"
                                }
                                button {
                                    attrs.onClickFunction = { passDragonTrick(DragonPass.RIGHT) }
                                    +"Pass dragon right"
                                }
                            }
                            else -> {
                                child(CardListComponent) {
                                    attrs.cards = gameState.cards
                                    attrs.hoverControls = null
                                }
                            }
                        }
                    }
                    styledDiv {
                        css {
                            flex(0.0, 0.0, 2.rem)
                            display = Display.flex
                        }
                        styledImg("Arrow right", "arrow.svg") {
                            css {
                                width = 2.rem
                                transform {
                                    rotate(270.deg)
                                }
                            }
                        }
                    }
                }
                styledDiv {
                    css {
                        flex(0.0, 1.0)

                        textAlign = TextAlign.center
                    }
                    styledImg("Arrow down", "arrow.svg") {
                        css {
                            width = 2.rem
                        }
                    }
                }
            }

            styledDiv {
                css {
                    flex(1.0, 1.0, 350.px)

                    textAlign = TextAlign.right
                    borderRight = "3px solid blue" // TODO color
                    paddingRight = 1.rem
                }

                val rightPlayerId = nextPlayer(nextPlayer(nextPlayer(playerId)))
                child(HiddenPlayerComponent) {
                    attrs.playerId = rightPlayerId
                    attrs.taiPanned = gameState.taiPannedPlayers[rightPlayerId]
                    attrs.shouldPlay = gameState.playersToPlay.contains(rightPlayerId)
                    attrs.numberOfCards = gameState.numberOfCardsPerPlayer[rightPlayerId] ?: 0
                }
            }
        }

        // player
        styledDiv {
            css {
                textAlign = TextAlign.center
            }

            child(PlayerComponent) {
                attrs.playerId = playerId
                attrs.taiPanned = gameState.taiPannedPlayers[playerId]
                attrs.shouldPlay = gameState.playersToPlay.contains(playerId)
                attrs.exchangeCardLeft = { card -> setExchangeCards(Triple(card, exchangeCards.second, exchangeCards.third)) }
                attrs.exchangeCardForward = { card -> setExchangeCards(Triple(exchangeCards.first, card, exchangeCards.third)) }
                attrs.exchangeCardRight = { card -> setExchangeCards(Triple(exchangeCards.first, exchangeCards.second, card)) }
                attrs.cards = gameState.cards.filterNot { it == exchangeCards.first || it == exchangeCards.second || it == exchangeCards.third }.toSet()
            }
        }

        // current trick
        styledDiv {
            css {}

            +"Current trick ${gameState.trickCards} (${gameState.trickCards.size} cards, points ${gameState.trickCards.sumBy(Card::points)})"
        }

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

                    +"$card (${card.points})"

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
                attrs.onClickFunction = { playCards() }
                +"Play cards"
            }
        }
    }
}