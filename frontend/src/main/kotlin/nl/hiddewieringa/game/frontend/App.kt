package nl.hiddewieringa.game.frontend

import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.ButtonType
import kotlinx.html.DIV
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nl.hiddewieringa.game.core.GameParameters
import nl.hiddewieringa.game.core.PlayerActions
import nl.hiddewieringa.game.frontend.games.TaiPanComponent
import nl.hiddewieringa.game.frontend.games.TaiPanParametersComponent
import nl.hiddewieringa.game.frontend.games.TicTacToeComponent
import nl.hiddewieringa.game.frontend.games.TicTacToeParametersComponent
import nl.hiddewieringa.game.taipan.TaiPanPlayerActions
import nl.hiddewieringa.game.taipan.state.TaiPanPlayerState
import nl.hiddewieringa.game.tictactoe.TicTacToePlayerActions
import nl.hiddewieringa.game.tictactoe.state.TicTacToePlayerState
import org.w3c.dom.WebSocket
import org.w3c.fetch.RequestInit
import react.*
import react.dom.*
import react.router.dom.hashRouter
import react.router.dom.route
import react.router.dom.routeLink
import react.router.dom.switch
import styled.css
import styled.styledDiv
import styled.styledH5

// TODO remove, include from the common compiled models
external interface GameDetails {
    val slug: String
    val description: String
}

val GamesComponent = functionalComponent<RProps> {
    val (games, setGames) = useState<Array<GameDetails>>(emptyArray())
    val (isLoaded, setLoaded) = useState(false)

    val fetchData = useCallback({
        MainScope().launch {
            val fetchedGames = window.fetch("${js("HOST")}/games")
                .await()
                .json()
                .await()
                .unsafeCast<Array<GameDetails>>()

            setGames(fetchedGames)
            setLoaded(true)
        }
    }, emptyArray())

    useEffect(listOf(fetchData)) {
        fetchData()
    }

    div {

        if (!isLoaded) {
            div("uk-spinner") {}
            return@div
        }

        h1 {
            +"Welcome, pick a game to play!"
        }

        div("uk-grid uk-child-width-1-4") {
            games.map { item ->
                div {
                    div("uk-card uk-card-default") {
                        key = item.slug
                        div("uk-card-header") {
                            h3 {
                                +item.slug
                            }
                        }
                        div("uk-card-body") {
                            +item.description
                        }
                        div("uk-card-footer") {
                            routeLink("/open/${item.slug}") {
                                button(null, null, ButtonType.button, "uk-button uk-button-primary") {
                                    +"Play"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

external interface GameSlug : RProps {
    val gameSlug: String
}

// TODO remove, include from the common compiled models

external interface OpenGamePlayerSlot {
    val id: String
    val name: String
}

external interface OpenGame {
    val id: String
    val playerSlotIds: Array<OpenGamePlayerSlot>
}

external interface ParametersProps<P : GameParameters> : RProps {
    var startGame: (serializer: KSerializer<P>, properties: P) -> Unit
}

val OpenGamesComponent = functionalComponent<GameSlug> { params ->
    val gameSlug = params.gameSlug

    val (openGames, setOpenGames) = useState<Array<OpenGame>>(emptyArray())

    val parameterComponent = when (gameSlug) {
        "tic-tac-toe" -> TicTacToeParametersComponent
        "tai-pan" -> TaiPanParametersComponent
        else -> throw IllegalStateException("Unknown game slug $gameSlug")
    }

    val fetchData = useCallback({
        MainScope().launch {
            val fetchedOpenGames = window.fetch("${js("HOST")}/games/${gameSlug}/open")
                .await()
                .json()
                .await()
                .unsafeCast<Array<OpenGame>>()

            setOpenGames(fetchedOpenGames)
        }
    }, emptyArray())

    fun <M : GameParameters> startGame(parameterSerializer: KSerializer<M>, parameters: M) {
        console.info("Starting game $gameSlug with parameters", parameters)
        MainScope().launch {
            val startedGame = window.fetch("${js("HOST")}/games/${gameSlug}/start", RequestInit(method = "POST", body = serializer.encodeToString(parameterSerializer, parameters)))
                .await()
                .json()
                .await()
            console.info("start $startedGame")
            fetchData()
        }
    }

    useEffect(listOf(fetchData)) {
        fetchData()
    }

    val listItems: RDOMBuilder<DIV>.() -> Unit = {
        if (openGames.isEmpty()) {
            p {
                +"No open games"
            }
        } else {
            div("uk-grid uk-child-width-1-4") {
                openGames.map { item ->
                    div {
                        key = item.id
                        div("uk-card uk-card-default") {
                            div("uk-card-header") {
                                h3 {
                                    +"Game ${item.id}"
                                }
                            }
                            div("uk-card-body") {
                                item.playerSlotIds.map { playerSlot ->
                                    routeLink("/play/${gameSlug}/${item.id}/${playerSlot.id}") {
                                        button(null, null, ButtonType.button, "uk-button uk-button-default uk-width-1-1") {
                                            span {
                                                attrs["uk-icon"] = "play"
                                            }
                                            +playerSlot.name
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

    div {

        div("uk-margin") {
            ul("uk-breadcrumb") {
                routeLink("") {
                    +"Home"
                }
                span {
                    +"$gameSlug open games"
                }
            }
        }

        listItems()

        div("uk-margin") {
            child(parameterComponent) {
                attrs.startGame = { serializer, parameters -> startGame(serializer, parameters) }
            }
        }
    }
}


external interface GamePlay : GameSlug {
    val instanceId: String
    val playerSlotId: String
}

val serializer = Json.Default

@Serializable
class StateEvent<S : Any>(
    val eventDescription: String?,
    val state: S?,
    val playerId: String?,
)

@Serializable
class PublishedAction<A : PlayerActions>(
    val action: A,
)

external interface DispatchAction<A : PlayerActions> {
    fun dispatch(action: A)
}

external interface GameUiProps<S : Any, A : PlayerActions> : RProps {
    var gameState: S?
    var dispatchAction: DispatchAction<A>
    var playerId: String?
}

val PlayComponent = functionalComponent<GamePlay> { params ->
    val gameSlug = params.gameSlug
    val instanceId = params.instanceId
    val playerSlotId = params.playerSlotId

    val (connected, setConnected) = useState(false)
    val (gameState, setGameState) = useState<Any?>(null)
    val (playerId, setPlayerId) = useState<String?>(null)
    val (gameEvents, addGameEvent) = useReducer<Array<String>, String>({ state, action -> arrayOf(action) + state }, emptyArray())

    val webSocket = useRef<WebSocket>(null)

    val stateSerializer = when (gameSlug) {
        "tic-tac-toe" -> TicTacToePlayerState.serializer()
        "tai-pan" -> TaiPanPlayerState.serializer()
        else -> throw IllegalStateException("Unknown game slug $gameSlug")
    }

    useEffectWithCleanup(listOf(instanceId, playerSlotId)) {
        val currentHostWithProtocol: String = if ((js("HOST") as String?).isNullOrEmpty()) window.location.protocol + window.location.host else js("HOST")
        val socket = WebSocket("${currentHostWithProtocol.replace(Regex("^http"), "ws")}/interaction/$instanceId/$playerSlotId")
        socket.onopen = {
            console.info("open", it)
            setConnected(true)
        }
        socket.onclose = {
            console.info("close", it)
            setConnected(false)
        }
        socket.onmessage = {
            val stateEvent = serializer.decodeFromString(StateEvent.serializer(stateSerializer), it.data.unsafeCast<String>())
            console.log("message", it.data, "event", stateEvent.eventDescription, "state", stateEvent.state, "player", stateEvent.playerId)
            if (stateEvent.state != null) {
                setGameState(stateEvent.state)
            }
            setPlayerId(stateEvent.playerId)
            if (stateEvent.eventDescription != null) {
                addGameEvent(stateEvent.eventDescription)
            }
        }
        socket.onerror = {
            console.error("error", it)
        }

        webSocket.current = socket

        // Cleanup
        {
            console.info("component closes webSocket")
            webSocket.current?.close()
            webSocket.current = null
            setConnected(false)
        }
    }

    fun websocketDispatchAction(action: String) { // Serialized
        console.info("action", action)
        webSocket.current?.send(action)
    }

    styledDiv {
        css {
            height = 100.pct
            width = 100.pct
            display = Display.flex
            flexDirection = FlexDirection.column
        }

        div("uk-margin uk-flex") {
            ul("uk-breadcrumb uk-flex-auto") {
                routeLink("") {
                    +"Home"
                }
                routeLink("/open/$gameSlug") {
                    +gameSlug
                }
                span {
                    +"Play"
                }
            }

            div {
                if (connected) {
                    span("uk-label uk-label-success") {
                        +"Connected"
                    }
                } else {
                    span("uk-label uk-label-danger") {
                        +"Disconnected"
                    }
                }
            }
        }

        styledDiv {
            css {
                display = Display.flex
            }

            // Game events
            styledDiv {
                css {
                    flex(1.0, 1.0, 25.pct)
                    minWidth = 15.rem
                    maxWidth = 40.rem
                    paddingRight = 2.rem
                }

                styledH5 {
                    css {
                        put("font-variant", "small-caps")
                    }
                    +"Game events"
                }

                gameEvents.map { event ->
                    div {
                        +event
                    }
                }
            }

            // Playing area
            styledDiv {
                css {
                    flex(1.0, 1.0, 100.pct)
                }

                when (gameSlug) {
                    "tic-tac-toe" -> {
                        child(TicTacToeComponent) {
                            attrs.dispatchAction = object : DispatchAction<TicTacToePlayerActions> {
                                override fun dispatch(action: TicTacToePlayerActions) {
                                    val serialized = serializer.encodeToString(PublishedAction.serializer(TicTacToePlayerActions.serializer()), PublishedAction(action))
                                    websocketDispatchAction(serialized)
                                }
                            }
                            attrs.gameState = gameState as TicTacToePlayerState?
                            attrs.playerId = playerId
                        }
                    }
                    "tai-pan" -> child(TaiPanComponent) {
                        attrs.dispatchAction = object : DispatchAction<TaiPanPlayerActions> {
                            override fun dispatch(action: TaiPanPlayerActions) {
                                val serialized = serializer.encodeToString(PublishedAction.serializer(TaiPanPlayerActions.serializer()), PublishedAction(action))
                                websocketDispatchAction(serialized)
                            }
                        }
                        attrs.gameState = gameState as TaiPanPlayerState?
                        attrs.playerId = playerId
                    }
                    else -> {
                    }
                }
            }
        }
    }
}

val AppComponent = functionalComponent<RProps> {
    StrictMode {
        hashRouter {
            switch {
                route("/", exact = true) {
                    child(GamesComponent)
                }
                route<GameSlug>("/open/:gameSlug") { props ->
                    child(OpenGamesComponent, props.match.params)
                }
                route<GamePlay>("/play/:gameSlug/:instanceId/:playerSlotId") { props ->
                    child(PlayComponent, props.match.params)
                }
            }
        }
    }
}