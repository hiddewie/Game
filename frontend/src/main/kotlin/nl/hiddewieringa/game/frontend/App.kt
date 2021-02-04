package nl.hiddewieringa.game.frontend

import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
import kotlinx.html.DIV
import kotlinx.html.js.onClickFunction
import nl.hiddewieringa.game.frontend.games.TaiPanComponent
import nl.hiddewieringa.game.frontend.games.TicTacToeComponent
import org.w3c.dom.WebSocket
import org.w3c.fetch.RequestInit
import react.*
import react.dom.*
import react.router.dom.browserRouter
import react.router.dom.route
import react.router.dom.routeLink
import react.router.dom.switch
import kotlin.js.Json
import kotlin.js.json

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
            val fetchedGames = window.fetch("http://localhost:8081/games")
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

val OpenGamesComponent = functionalComponent<GameSlug> { params ->
    val gameSlug = params.gameSlug

    val (openGames, setOpenGames) = useState<Array<OpenGame>>(emptyArray())

    val fetchData = useCallback({
        MainScope().launch {
            val openGames = window.fetch("http://localhost:8081/games/${gameSlug}/open")
                .await()
                .json()
                .await()
                .unsafeCast<Array<OpenGame>>()

            setOpenGames(openGames)
        }
    }, emptyArray())

    val startGame = {
        MainScope().launch {
            val startedGame = window.fetch("http://localhost:8081/games/${gameSlug}/start", RequestInit(method = "POST"))
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
            button(null, null, ButtonType.button, "uk-button uk-button-primary") {
                attrs.onClickFunction = { startGame() }
                +"Start a new game"
            }
        }
    }
}


external interface GamePlay : GameSlug {
    val instanceId: String
    val playerSlotId: String
}

// TODO add generics for game
external interface StateEvent {
    val event: Json?
    val state: Json?
    val playerId: Array<String?>
}

val PlayComponent = functionalComponent<GamePlay> { params ->
    val gameSlug = params.gameSlug
    val instanceId = params.instanceId
    val playerSlotId = params.playerSlotId

    val (connected, setConnected) = useState(false)
    val (gameState, setGameState) = useState<Json?>(null)
    val (playerId, setPlayerId) = useState<String?>(null)

    val webSocket = useRef<WebSocket?>(null)

    useEffectWithCleanup(listOf(instanceId, playerSlotId)) {
        val socket = WebSocket("ws://localhost:8081/interaction/$instanceId/$playerSlotId")
        socket.onopen = {
            console.info("open", it)
            setConnected(true)
        }
        socket.onclose = {
            console.info("close", it)
            setConnected(false)
        }
        socket.onmessage = {
            val stateEvent = JSON.parse<StateEvent>(it.data.unsafeCast<String>())
            console.info("message", it.data, "event", stateEvent.event, "state", stateEvent.state, "player", stateEvent.playerId)
            setGameState(stateEvent.state)
            setPlayerId(stateEvent.playerId[1])
        }
        socket.onerror = {
            console.info("error", it)
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

    val dispatchAction = { action: Json ->
        console.info("action", action)
        webSocket.current?.send(JSON.stringify(json(
            "action" to action
        )))
        Unit
    }

    div {

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

        when (gameSlug) {
            "tic-tac-toe" -> {
                child(TicTacToeComponent) {
                    attrs.dispatchAction = dispatchAction
                    attrs.gameState = gameState
                    attrs.playerId = playerId
                }
            }
            "tai-pan" -> child(TaiPanComponent) {
                attrs.dispatchAction = dispatchAction
                attrs.gameState = gameState
                attrs.playerId = playerId
            }
            else -> {
            }
        }
    }
}

val AppComponent = functionalComponent<RProps> {
    StrictMode {
        browserRouter {
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