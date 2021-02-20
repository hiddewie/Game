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
import nl.hiddewieringa.game.frontend.games.taipan.*
import nl.hiddewieringa.game.taipan.*
import nl.hiddewieringa.game.taipan.card.Card
import nl.hiddewieringa.game.taipan.card.NumberedCard
import nl.hiddewieringa.game.taipan.card.ThreeWayPass
import nl.hiddewieringa.game.taipan.state.TaiPanPlayerState
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
    val (dragonPass, setDragonPass) = useState<DragonPass?>(null)
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

    val passDragonTrick = {
        if (dragonPass != null) {
            dispatchAction.dispatch(PassDragonTrick(dragonPass))
        } else {
            // TODO
            console.error("Not implemented")
        }
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

    val playersToPlay = gameState?.playersToPlay ?: emptyList()

    if (playerId == null || gameState == null) {
        p {
            +"No player ID or game state"
        }
        return@functionalComponent
    }

    styledDiv {
//        css {
//            put("transform-style", "preserve-3d")
//            put("perspective-origin", "top")
//            position = Position.relative
//            transform {
//                perspective(2000.px)
//            }
//        }

        // score
        styledDiv {
            css { }

            val points = gameState.points
            styledTable {
                css {
                    margin = "0 auto"
                    put("font-variant", "small-caps")
                }

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
//            // TODO make component
//            val taiPanned = gameState.taiPannedPlayers[partnerPlayerId]
//            val shouldPlay = gameState.playersToPlay.contains(partnerPlayerId)
//            val taiPannedStatus = when (taiPanned) {
//                null -> ""
//                TaiPanStatus.NORMAL -> "⭐"
//                TaiPanStatus.GREAT -> "⭐"
//            }
//            +"Player ${partnerPlayerId}: Tai pan: $taiPannedStatus. ${if (shouldPlay) "Should play" else "Should not play"}"

//            styledDiv {
//                css {
//                    position = Position.absolute
//                    top = 0.px
//                    marginTop = (-500).px
//                    transform {
//                        translateZ((-300).px)
//                        translateY(500.px)
//                    }
//                }
            child(HiddenPlayerComponent) {
                attrs.playerId = partnerPlayerId
                attrs.taiPanned = gameState.taiPannedPlayers[partnerPlayerId]
                attrs.shouldPlay = gameState.playersToPlay.contains(partnerPlayerId)
                attrs.numberOfCards = gameState.numberOfCardsPerPlayer[partnerPlayerId] ?: 0
            }
//            }
        }

        styledDiv {
            css {
                display = Display.flex
            }

            // left & right
            styledDiv {
                css {
                    flex(1.0, 1.0, 30.pct)

                    borderLeft = "3px solid blue" // TODO color
                    paddingLeft = 1.rem
                }

                val leftPlayerId = nextPlayer(playerId)
//                // TODO make component
//                val taiPanned = gameState.taiPannedPlayers[leftPlayerId]
//                val shouldPlay = gameState.playersToPlay.contains(leftPlayerId)
//                val taiPannedStatus = when (taiPanned) {
//                    null -> ""
//                    TaiPanStatus.NORMAL -> "⭐"
//                    TaiPanStatus.GREAT -> "⭐"
//                }
//                +"Player ${leftPlayerId}: Tai pan: $taiPannedStatus. ${if (shouldPlay) "Should play" else "Should not play"}"

//            styledDiv {
//                css {
//                    position = Position.absolute
//                    top = 0.px
//                    marginTop = (-500).px
//                    transform {
//                        rotateY((-90).deg)
//                        translateZ(300.px)
//                        translateY(500.px)
//                    }
//                }
                child(HiddenPlayerComponent) {
                    attrs.playerId = leftPlayerId
                    attrs.taiPanned = gameState.taiPannedPlayers[leftPlayerId]
                    attrs.shouldPlay = gameState.playersToPlay.contains(leftPlayerId)
                    attrs.numberOfCards = gameState.numberOfCardsPerPlayer[leftPlayerId] ?: 0
                }
//            }
            }

            styledDiv {
                css {
                    flex(1.0, 1.0, 40.pct)

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
                            flex(1.0, 0.0, 2.rem)
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
                        }

                        child(CardListComponent) {
                            attrs.cards = gameState.cards
                            attrs.showHover = false
                        }

                        div {
                            +"Passed cards"
                            styledDiv {
                                css {
                                    display = Display.flex
                                    justifyContent = JustifyContent.spaceEvenly
                                    padding(1.rem, 2.rem)
                                }

                                val (left, forward, right) = exchangeCards
                                styledDiv {
                                    css {
                                        transform {
                                            rotate((-15).deg)
                                        }
                                        put("transform-origin", "bottom")
                                    }

                                    if (left != null) {
                                        child(CardComponent) {
                                            attrs.card = left
                                        }
                                    } else {
                                        child(EmptyCardComponent)

                                    }
                                }
                                div {
                                    if (forward != null) {
                                        child(CardComponent) {
                                            attrs.card = forward
                                        }
                                    } else {
                                        child(EmptyCardComponent)
                                    }
                                }
                                styledDiv {
                                    css {
                                        transform {
                                            rotate((15).deg)
                                        }
                                        put("transform-origin", "bottom")
                                    }

                                    if (right != null) {
                                        child(CardComponent) {
                                            attrs.card = right
                                        }
                                    } else {
                                        child(EmptyCardComponent)
                                    }
                                }
                            }
                        }
                    }
                    styledDiv {
                        css {
                            flex(1.0, 0.0, 2.rem)
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
                    flex(1.0, 1.0, 30.pct)

                    textAlign = TextAlign.right
                    borderRight = "3px solid blue" // TODO color
                    paddingRight = 1.rem
                }

                val rightPlayerId = nextPlayer(nextPlayer(nextPlayer(playerId)))
//                // TODO make component
//                val taiPanned = gameState.taiPannedPlayers[rightPlayerId]
//                val shouldPlay = gameState.playersToPlay.contains(rightPlayerId)
//                val taiPannedStatus = when (taiPanned) {
//                    null -> ""
//                    TaiPanStatus.NORMAL -> "⭐"
//                    TaiPanStatus.GREAT -> "⭐"
//                }
//                +"Player ${rightPlayerId}: Tai pan: $taiPannedStatus. ${if (shouldPlay) "Should play" else "Should not play"}"

//            styledDiv {
//                css {
//                    position = Position.absolute
//                    right = 0.px
//                    top = 0.px
//                    marginTop = (-500).px
//                    transform {
//                        rotateY(90.deg)
//                        translateZ(300.px)
//                        translateY(500.px)
//                    }
//                }
                child(HiddenPlayerComponent) {
                    attrs.playerId = rightPlayerId
                    attrs.taiPanned = gameState.taiPannedPlayers[rightPlayerId]
                    attrs.shouldPlay = gameState.playersToPlay.contains(rightPlayerId)
                    attrs.numberOfCards = gameState.numberOfCardsPerPlayer[rightPlayerId] ?: 0
                }
//            }
            }
        }

        // player
        styledDiv {
            css {
                textAlign = TextAlign.center
            }

            // TODO make component
//            val taiPanned = gameState.taiPannedPlayers[playerId]
//            val shouldPlay = gameState.playersToPlay.contains(playerId)
//            val taiPannedStatus = when (taiPanned) {
//                null -> ""
//                TaiPanStatus.NORMAL -> "⭐"
//                TaiPanStatus.GREAT -> "⭐"
//            }
//            +"Tai pan: $taiPannedStatus. ${if (shouldPlay) "Should play" else "Should not play"}"

//            styledDiv {
//                css {
//                    position = Position.absolute
//                    top = 0.px
//                    marginTop = (-500).px
//                    transform {
//                        translateZ(300.px)
//                        translateY(500.px)
//                    }
//                }

            child(PlayerComponent) {
                attrs.playerId = playerId
                attrs.taiPanned = gameState.taiPannedPlayers[playerId]
                attrs.shouldPlay = gameState.playersToPlay.contains(playerId)
                attrs.cards = gameState.cards.filterNot { it == exchangeCards.first || it == exchangeCards.second || it == exchangeCards.third }.toSet()
            }

//            child(CardListComponent) {
//                attrs.cards = gameState.cards
//            }
//            }
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