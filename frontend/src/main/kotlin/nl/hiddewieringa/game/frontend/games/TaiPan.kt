package nl.hiddewieringa.game.frontend.games

import kotlinx.css.*
import kotlinx.css.properties.deg
import kotlinx.css.properties.rotate
import kotlinx.css.properties.transform
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.classes
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import nl.hiddewieringa.game.core.TwoTeamPlayerId
import nl.hiddewieringa.game.frontend.GameUiProps
import nl.hiddewieringa.game.frontend.ParametersProps
import nl.hiddewieringa.game.frontend.games.taipan.*
import nl.hiddewieringa.game.taipan.*
import nl.hiddewieringa.game.taipan.card.*
import nl.hiddewieringa.game.taipan.state.TaiPanPlayerState
import nl.hiddewieringa.game.taipan.state.TaiPanPlayerStateType
import org.w3c.dom.HTMLInputElement
import react.child
import react.dom.*
import react.functionalComponent
import react.useState
import styled.css
import styled.styledDiv
import styled.styledImg
import styled.styledInput
import kotlin.Triple
import kotlin.random.Random

val TaiPanParametersComponent = functionalComponent<ParametersProps<TaiPanGameParameters>> { props ->
    val startGame = props.startGame
    val (seed) = useState(Random.nextLong(0, Long.MAX_VALUE - 1))
    val (parameters, setParameters) = useState(TaiPanGameParameters(1000, seed))

    form(classes = "uk-form-stacked") {
        div("uk-margin") {
            div("uk-form-label") {
                +"Game points"
            }
            div("uk-form-controls") {
                label {
                    input(InputType.radio, classes = "uk-radio", name = "points") {
                        attrs.onChangeFunction = {
                            setParameters(parameters.copy(points = 500))
                        }
                    }
                    +" 500 points"
                }
                br {}
                label {
                    input(InputType.radio, classes = "uk-radio", name = "points") {
                        attrs.defaultChecked = true
                        attrs.onChangeFunction = {
                            setParameters(parameters.copy(points = 1000))
                        }
                    }
                    +" 1000 points"
                }
            }
            div("uk-form-label") {
                +"Seed"
            }
            div("uk-form-controls") {
                styledInput(InputType.number) {
                    css {
                        maxWidth = 20.rem
                    }
                    attrs.defaultValue = seed.toString()
                    attrs.classes = setOf("uk-input")
                    attrs.name = "seed"
                    attrs.min = "0"
                    attrs.step = "1"
                    attrs.max = Long.MAX_VALUE.toString()
                    attrs.onChangeFunction = { event ->
                        setParameters(parameters.copy(seed = (event.target as HTMLInputElement).value.toLong()))
                    }
                }
            }
        }
    }

    div("uk-margin") {
        button(null, null, ButtonType.button, "uk-button uk-button-primary") {
            attrs.onClickFunction = { startGame(TaiPanGameParameters.serializer(), parameters) }
            +"Start a new game"
        }
    }
}

// TODO implement auto-fold with random delay

val TaiPanComponent = functionalComponent<GameUiProps<TaiPanPlayerState, TaiPanPlayerActions>> { props ->
    val gameState = props.gameState
    val dispatchAction = props.dispatchAction
    val playerId = props.playerId?.let { TwoTeamPlayerId.valueOf(it) }

    val (selectedCards, setSelectedCards) = useState(emptySet<Card>())
    val (exchangeCards, setExchangeCards) = useState<Triple<Card?, Card?, Card?>>(Triple(null, null, null))
    val (mahjongRequestValue, selectMahjongRequest) = useState<Int?>(null)
    val (phoenixValue, selectPhoenixValue) = useState<Int?>(null)

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
            val addons = setOfNotNull(
                mahjongRequestValue?.let(::MahjongRequest),
                phoenixValue?.let(::PhoenixValue),
            )

            dispatchAction.dispatch(PlayCards(selectedCards, addons))

            selectMahjongRequest(null)
            selectPhoenixValue(null)
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

    if (playerId == null || gameState == null) {
        p {
            +"No player ID or game state"
        }
        return@functionalComponent
    }

    if ((selectedCards - gameState.cards).isNotEmpty()) {
        setSelectedCards(selectedCards.intersect(gameState.cards))
    }

    div {
        child(ScoreComponent) {
            attrs.points = gameState.points
            attrs.playerId = playerId
            attrs.round = gameState.roundIndex
            attrs.trick = gameState.trickIndex
        }

        // TODO to better component
        styledDiv {
            css {}
            +"Played cards: ${gameState.playedCards.joinToString(" - ")}"
        }

        val leftPlayerId = nextPlayer(playerId)
        val partnerPlayerId = nextPlayer(nextPlayer(playerId))
        val rightPlayerId = nextPlayer(nextPlayer(nextPlayer(playerId)))

        val lastPlayedPlayer = gameState.lastPlayedCards?.first
        val lastPlayedCards = gameState.lastPlayedCards?.second

        // partner
        styledDiv {
            css {
                textAlign = TextAlign.center
            }

            child(HiddenPlayerComponent) {
                attrs.playerId = partnerPlayerId
                attrs.taiPanned = gameState.taiPannedPlayers[partnerPlayerId]
                attrs.shouldPlay = gameState.playersToPlay.contains(partnerPlayerId)
                attrs.shouldExchange = false
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

                    display = Display.flex
                    flexDirection = FlexDirection.column
                    justifyContent = JustifyContent.center
                }

                child(HiddenPlayerComponent) {
                    attrs.playerId = leftPlayerId
                    attrs.taiPanned = gameState.taiPannedPlayers[leftPlayerId]
                    attrs.shouldPlay = gameState.playersToPlay.contains(leftPlayerId)
                    attrs.shouldExchange = false
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
                        visibility = if (lastPlayedPlayer == partnerPlayerId) Visibility.visible else Visibility.hidden
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
                            visibility = if (lastPlayedPlayer == leftPlayerId) Visibility.visible else Visibility.hidden
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
                                if (gameState.playersToPlay.contains(playerId)) {
                                    styledDiv {
                                        css {
                                            textAlign = TextAlign.center
                                        }

                                        div {
                                            attrs.classes = setOf("uk-alert", "uk-alert-primary")
                                            +"Receive next cards or tai pan"
                                        }
                                        button {
                                            attrs.classes = setOf("uk-button", "uk-button-primary")
                                            attrs.onClickFunction = { requestNextCards() }
                                            +"Receive next cards"
                                        }
                                    }
                                } else {
                                    styledDiv {
                                        css {
                                            textAlign = TextAlign.center
                                        }

                                        attrs.classes = setOf("uk-alert", "uk-alert-primary")
                                        +"Wait for the other players to tai pan or receive their cards"
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
                                    styledDiv {
                                        css {
                                            textAlign = TextAlign.center
                                        }

                                        attrs.classes = setOf("uk-alert", "uk-alert-primary")
                                        +"Wait for the other players to exchange"
                                    }
                                }
                            }
                            TaiPanPlayerStateType.PASS_DRAGON -> {
                                styledDiv {
                                    css {
                                        visibility = if (gameState.playersToPlay.contains(playerId)) Visibility.visible else Visibility.hidden
                                    }

                                    styledDiv {
                                        css {
                                            textAlign = TextAlign.center
                                        }

                                        attrs.classes = setOf("uk-alert", "uk-alert-primary")
                                        +"Pass the dragon"
                                    }

                                    styledDiv {
                                        css {
                                            display = Display.flex
                                            justifyContent = JustifyContent.spaceBetween
                                        }

                                        button {
                                            attrs.classes = setOf("uk-button", "uk-button-primary")
                                            attrs.onClickFunction = { passDragonTrick(DragonPass.LEFT) }
                                            +"Pass dragon left"
                                        }
                                        button {
                                            attrs.classes = setOf("uk-button", "uk-button-primary")
                                            attrs.onClickFunction = { passDragonTrick(DragonPass.RIGHT) }
                                            +"Pass dragon right"
                                        }
                                    }
                                }
                            }
                            TaiPanPlayerStateType.PLAY -> {
                                // TODO allow viewing the played cards
//                                child(CardListComponent) {
//                                    attrs.cards = gameState.trickCards
//                                    attrs.hoverControls = null
//                                    attrs.small = true
//                                }
                                child(CardListComponent) {
                                    attrs.cards = lastPlayedCards?.cards ?: emptySet()
                                    attrs.hoverControls = null
                                    attrs.selectedCards = emptySet()
                                    attrs.cardSelected = { }
                                    attrs.cardDeselected = { }
                                    attrs.canSelect = false
                                }

                                styledDiv {
                                    css {
                                        display = Display.flex
                                        justifyContent = JustifyContent.center
                                        textAlign = TextAlign.center
                                        visibility = if (gameState.playersToPlay.contains(playerId)) Visibility.visible else Visibility.hidden
                                    }

                                    button {
                                        attrs.classes = setOf("uk-button", "uk-button-primary")
                                        attrs.onClickFunction = { playCards() }
                                        +"Play"
                                    }
                                    button {
                                        attrs.classes = setOf("uk-button", "uk-button-primary")
                                        attrs.onClickFunction = { fold() }
                                        +"Fold"
                                    }
                                }

                                styledDiv {
                                    css {
                                        display = Display.flex
                                        justifyContent = JustifyContent.center
                                        visibility = if (gameState.playersToPlay.contains(playerId) && selectedCards.contains(Phoenix)) Visibility.visible else Visibility.hidden
                                    }

                                    div {
                                        i {
                                            +"Select Phoenix value"
                                        }
                                    }

                                    styledDiv {
                                        css {
                                            display = Display.flex
                                            justifyContent = JustifyContent.center
                                        }

                                        val choices: Map<String, Int?> = NumberedCard.VALUES.associateBy(NumberedCard::stringifyValue)
                                        div("uk-button-group") {
                                            choices.forEach { (label, value) ->
                                                button {
                                                    attrs.classes = setOf("uk-button", if (mahjongRequestValue == value) "uk-button-primary" else "uk-button-default", "uk-button-small")
                                                    attrs.onClickFunction = { selectPhoenixValue(value) }
                                                    +label
                                                }
                                            }
                                        }
                                    }
                                }

                                styledDiv {
                                    css {
                                        display = Display.flex
                                        flexDirection = FlexDirection.column
                                        justifyContent = JustifyContent.center
                                        textAlign = TextAlign.center
                                        visibility = if (gameState.playersToPlay.contains(playerId) && selectedCards.contains(Mahjong)) Visibility.visible else Visibility.hidden
                                    }

                                    div {
                                        i {
                                            +"Select Mahjong value"
                                        }
                                    }

                                    // TODO extract to shared 'select value' component
                                    styledDiv {
                                        css {
                                            display = Display.flex
                                            justifyContent = JustifyContent.center
                                        }

                                        val choices: Map<String, Int?> = mapOf("∅" to null) + NumberedCard.VALUES.associateBy(NumberedCard::stringifyValue)
                                        div("uk-button-group") {
                                            choices.forEach { (label, value) ->
                                                button {
                                                    attrs.classes = setOf("uk-button", if (mahjongRequestValue == value) "uk-button-primary" else "uk-button-default", "uk-button-small")
                                                    attrs.onClickFunction = { selectMahjongRequest(value) }
                                                    +label
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                child(CardListComponent) {
                                    attrs.cards = gameState.cards
                                    attrs.selectedCards = emptySet()
                                    attrs.hoverControls = null
                                    attrs.cardSelected = { }
                                    attrs.cardDeselected = { }
                                    attrs.canSelect = false
                                }
                            }
                        }
                    }
                    styledDiv {
                        css {
                            flex(0.0, 0.0, 2.rem)
                            display = Display.flex
                            visibility = if (lastPlayedPlayer == rightPlayerId) Visibility.visible else Visibility.hidden
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
                        visibility = if (lastPlayedPlayer == playerId) Visibility.visible else Visibility.hidden
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

                    display = Display.flex
                    flexDirection = FlexDirection.column
                    justifyContent = JustifyContent.center

                    textAlign = TextAlign.right
                }

                child(HiddenPlayerComponent) {
                    attrs.playerId = rightPlayerId
                    attrs.taiPanned = gameState.taiPannedPlayers[rightPlayerId]
                    attrs.shouldPlay = gameState.playersToPlay.contains(rightPlayerId)
                    attrs.shouldExchange = false
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
                attrs.shouldExchange = gameState.stateType == TaiPanPlayerStateType.EXCHANGE_CARDS // TODO check if already exchanged
                attrs.exchangeCardLeft = { card -> setExchangeCards(Triple(card, exchangeCards.second, exchangeCards.third)) }
                attrs.exchangeCardForward = { card -> setExchangeCards(Triple(exchangeCards.first, card, exchangeCards.third)) }
                attrs.exchangeCardRight = { card -> setExchangeCards(Triple(exchangeCards.first, exchangeCards.second, card)) }
                attrs.cardSelected = { card -> setSelectedCards(selectedCards + card) }
                attrs.cardDeselected = { card -> setSelectedCards(selectedCards - card) }
                attrs.taiPan = { callTaiPan() }
                attrs.canTaiPan = !gameState.taiPannedPlayers.containsKey(playerId) && when (gameState.stateType) {
                    TaiPanPlayerStateType.RECEIVE_CARDS -> true
                    TaiPanPlayerStateType.PLAY -> gameState.cards.size == 14
                    else -> false
                }
                attrs.cards = gameState.cards.filterNot { it == exchangeCards.first || it == exchangeCards.second || it == exchangeCards.third }.toSet()
                attrs.selectedCards = selectedCards
            }
        }
    }
}