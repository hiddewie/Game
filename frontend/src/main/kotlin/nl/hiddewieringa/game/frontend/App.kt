package nl.hiddewieringa.game.frontend

import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
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
        h1 { +"Games" }
        p {
            +"loaded: ${if (isLoaded) "true" else "false"}"
        }
        ul {
            games.map { item ->
                li {
                    key = item.slug
                    routeLink("/open/${item.slug}") {
                        +item.slug
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
external interface OpenGame {
    val id: String
    val playerSlotIds: Array<String>
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

    val listItems = {
        ul {
            openGames.map { item ->
                li {
                    key = item.id
                    ul {
                        item.playerSlotIds.map { playerSlotId ->
                            li {
                                key = playerSlotId
                                routeLink("/play/${gameSlug}/${item.id}/${playerSlotId}") {
                                    +"${item.id} ${Typography.mdash} ${playerSlotId}"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    div {
        listItems()
        button {
            attrs.onClickFunction = { startGame() }
            +"START"
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
}

val PlayComponent = functionalComponent<GamePlay> { params ->
    val gameSlug = params.gameSlug
    val instanceId = params.instanceId
    val playerSlotId = params.playerSlotId

    val (connected, setConnected) = useState(false)
    val (gameState, setGameState) = useState<Json?>(null)

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
            console.info("message", it.data, "event", stateEvent.event, "state", stateEvent.state)
            setGameState(stateEvent.state)
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
        p {
            +"Game $gameSlug instance $instanceId player $playerSlotId"
        }
        p {
            if (connected) {
                +"websocket connected"
            } else {
                +"websocket disconnected"
            }
        }

        when (gameSlug) {
            "tic-tac-toe" -> {
                child(TicTacToeComponent) {
                    attrs.dispatchAction = dispatchAction
                    attrs.gameState = gameState
                }
            }
            "tai-pan" -> child(TaiPanComponent) {
                attrs.dispatchAction = dispatchAction
                attrs.gameState = gameState
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