package nl.hiddewieringa.game.server.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.mono
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KLogging
import nl.hiddewieringa.game.core.Event
import nl.hiddewieringa.game.core.GameState
import nl.hiddewieringa.game.core.PlayerActions
import nl.hiddewieringa.game.core.PlayerId
import nl.hiddewieringa.game.server.games.GameDetails
import nl.hiddewieringa.game.server.games.GameInstance
import nl.hiddewieringa.game.server.games.GameInstanceProvider
import nl.hiddewieringa.game.server.games.GameProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriTemplate
import reactor.core.publisher.Mono
import java.util.*

@Serializable
data class WrappedAction<A : PlayerActions>(val action: A)

@Serializable
data class WrappedEvent<S : Any, PID : PlayerId>(val eventDescription: String?, val state: S, val playerId: PID)

@Component
class WebSocketController(
    val gameInstanceProvider: GameInstanceProvider,
    val gameProvider: GameProvider,
) : WebSocketHandler {

    private val template = UriTemplate(URI_TEMPLATE)
    private val serializer = Json.Default

    override fun handle(session: WebSocketSession): Mono<Void> {
        val path = session.handshakeInfo.uri.path
        val instanceId = UUID.fromString(template.match(path)["instanceId"])
        val playerSlotId = UUID.fromString(template.match(path)["playerSlotId"])

        return handleRequest(playerSlotId, instanceId, session)
    }

    private fun handleRequest(playerSlotId: UUID, instanceId: UUID, session: WebSocketSession): Mono<Void> {
        val gameInstance = gameInstanceProvider.gameInstance(instanceId)
        return handleGameInstance(playerSlotId, session, gameInstance)
    }

    private fun <S : GameState<S>, PID : PlayerId> handleGameInstance(playerSlotId: UUID, session: WebSocketSession, gameInstance: GameInstance<S, PID>?): Mono<Void> {
        // If the instance or player slot does not exist, close the connection
        val playerSlot = gameInstance?.playerSlots?.get(playerSlotId)
            ?: return Mono.empty()

        val gameDetails = gameProvider.bySlug(gameInstance.gameSlug)
        return handle(playerSlotId, session, gameInstance, gameDetails as GameDetails<*, *, *, *, PID, *, S, *>, playerSlot)
    }

    private fun <A : PlayerActions, E : Event, S : GameState<S>, PS : Any, PID : PlayerId> handle(playerSlotId: UUID, session: WebSocketSession, gameInstance: GameInstance<S, PID>, gameDetails: GameDetails<*, *, A, E, PID, *, S, PS>, playerId: PID): Mono<Void> {
        gameInstanceProvider.increasePlayerSlotReference(gameInstance.id, playerSlotId)

        CoroutineScope(Dispatchers.Default).launch {
            session.receive()
                // Messages must be retained to make Netty not lose it due to 0 message reference count
                .map(WebSocketMessage::retain)
                .asFlow()
                .collect {
                    gameInstanceProvider.applyPlayerAction<A, E, PID, S, PS>(gameInstance.id, playerId, readAction(it, gameDetails.actionSerializer))
                    it.release()
                }
        }

        val wrappedEventSerializer = WrappedEvent.serializer(gameDetails.playerStateSerializer, gameDetails.playerIdSerializer)

        // The initial player state is published directly
        val initialStateFlux = mono {
            val initialState = gameDetails.playerState.invoke(gameInstance.state, playerId)
            logger.info("Initial websocket message ${session.stateMessage(initialState, playerId, wrappedEventSerializer).payloadAsText}")
            session.stateMessage(initialState, playerId, wrappedEventSerializer)
        }

        val events = gameInstanceProvider.gameInstanceEvents<E, PS>(gameInstance.id, playerSlotId)
                .map { (event, state) -> session.eventMessage(event, state, playerId, wrappedEventSerializer) }
                .asFlux()

        return session.send(initialStateFlux.concatWith(events))
            .doFinally { gameInstanceProvider.decreasePlayerSlotReference(gameInstance.id, playerSlotId) }
    }

    private fun <E : Event, S : Any, PID : PlayerId> WebSocketSession.eventMessage(event: E, state: S, playerId: PID, eventSerializer: KSerializer<WrappedEvent<S, PID>>) =
        textMessage(serializer.encodeToString(eventSerializer, WrappedEvent(event.toString(), state, playerId)))

    // TODO add exception handling and send message when something is wrong with the payload.
    private fun <A : PlayerActions> readAction(message: WebSocketMessage, actionSerializer: KSerializer<A>) =
        serializer.decodeFromString(WrappedAction.serializer(actionSerializer), message.payloadAsText).action

    private fun <S : Any, PID : PlayerId> WebSocketSession.stateMessage(state: S, playerId: PID, eventSerializer: KSerializer<WrappedEvent<S, PID>>) =
        textMessage(serializer.encodeToString(eventSerializer, WrappedEvent(null, state, playerId)))

    companion object : KLogging() {
        const val URI_TEMPLATE = "/interaction/{instanceId}/{playerSlotId}"
    }
}

@Configuration
class WebSocketConfiguration {

    @Bean
    fun webSocketHandlerMapping(webSocketHandler: WebSocketHandler): HandlerMapping =
        SimpleUrlHandlerMapping(
            mapOf(
                WebSocketController.URI_TEMPLATE to webSocketHandler,
            ),
            1
        )
}
