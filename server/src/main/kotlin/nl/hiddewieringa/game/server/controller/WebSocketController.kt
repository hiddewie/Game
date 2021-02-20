package nl.hiddewieringa.game.server.controller

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import nl.hiddewieringa.game.core.Event
import nl.hiddewieringa.game.core.PlayerActions
import nl.hiddewieringa.game.core.PlayerId
import nl.hiddewieringa.game.server.games.GameInstance
import nl.hiddewieringa.game.server.games.GameInstanceProvider
import nl.hiddewieringa.game.server.games.PlayerSlot
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

private val logger = KotlinLogging.logger {}

@Serializable
data class WrappedAction<A : PlayerActions>(val action: A)

// TODO make playerId to 'metadata' object
@Serializable
data class WrappedEvent<E : Event, S : Any, PID : PlayerId>(val event: E?, val state: S, val playerId: PID)

@Component
class WebSocketController(
    val gameInstanceProvider: GameInstanceProvider,
) : WebSocketHandler {

    private val template = UriTemplate(URI_TEMPLATE)

    private val serializer = Json {}

    override fun handle(session: WebSocketSession): Mono<Void> {
        val path = session.handshakeInfo.uri.path
        val instanceId: UUID = UUID.fromString(template.match(path)["instanceId"])
        val playerSlotId: UUID = UUID.fromString(template.match(path)["playerSlotId"])

        return handle(playerSlotId, instanceId, session)
    }

    private fun handle(playerSlotId: UUID, instanceId: UUID, session: WebSocketSession): Mono<Void> {
        val gameInstance = gameInstanceProvider.gameInstance(instanceId)
        return handle(playerSlotId, session, gameInstance)
    }

    private fun <A : PlayerActions, E : Event, S : Any, PID : PlayerId> handle(playerSlotId: UUID, session: WebSocketSession, gameInstance: GameInstance<A, E, S, PID>?): Mono<Void> {
        // If the instance or player slot does not exist, close the connection
        val playerSlot = gameInstance?.playerSlots?.get(playerSlotId)
            ?: return Mono.empty()

        return handle(playerSlotId, session, gameInstance, playerSlot)
    }

    private fun <A : PlayerActions, E : Event, S : Any, PID : PlayerId> handle(playerSlotId: UUID, session: WebSocketSession, gameInstance: GameInstance<A, E, S, PID>, playerSlot: PlayerSlot<A, E, S, PID>): Mono<Void> {
        playerSlot.increaseReference()

        // TODO: other scope? create our own scope that is coupled with bean lifetime
        GlobalScope.launch {
            session.receive()
                // Messages must be retained to make Netty not lose it due to 0 message reference count
                .map(WebSocketMessage::retain)
                .asFlow()
                .collect { playerSlot.sendChannel.send(readAction(it, gameInstance.actionSerializer)) }
        }

        val wrappedEventSerializer = WrappedEvent.serializer(gameInstance.eventSerializer, gameInstance.stateSerializer, gameInstance.playerIdSerializer)

        // The initial state is published directly
        val initialStateFlux = Flux.just(session.stateMessage(gameInstance.playerState(playerSlotId), playerSlot.playerId, wrappedEventSerializer))
        // TODO modify events to be player specific, cleaned of information not destined for a player
        val events = playerSlot.receiveChannel.receiveAsFlow().asFlux()
            .map { (event, state) -> session.eventMessage(event, state, playerSlot.playerId, wrappedEventSerializer) }

        return session.send(initialStateFlux.concatWith(events))
            .doFinally { playerSlot.decreaseReference() }
    }

    private fun <E : Event, S : Any, PID : PlayerId> WebSocketSession.eventMessage(data: E, state: S, playerId: PID, eventSerializer: KSerializer<WrappedEvent<E, S, PID>>) =
        textMessage(serializer.encodeToString(eventSerializer, WrappedEvent(data, state, playerId)))

    // TODO add exception handling and send message when something is wrong with the payload.
    private fun <A : PlayerActions> readAction(message: WebSocketMessage, actionSerializer: KSerializer<A>) =
        serializer.decodeFromString(WrappedAction.serializer(actionSerializer), message.payloadAsText).action

    private fun <E : Event, S : Any, PID : PlayerId> WebSocketSession.stateMessage(state: S, playerId: PID, eventSerializer: KSerializer<WrappedEvent<E, S, PID>>) =
        textMessage(serializer.encodeToString(eventSerializer, WrappedEvent(null, state, playerId)))

    companion object {
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
