package nl.hiddewieringa.game.server.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import mu.KotlinLogging
import nl.hiddewieringa.game.core.Event
import nl.hiddewieringa.game.core.PlayerActions
import nl.hiddewieringa.game.core.PlayerId
import nl.hiddewieringa.game.server.games.GameInstanceProvider
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

data class WrappedAction<A : PlayerActions>(val action: A)
data class WrappedEvent(val event: Any?, val state: Any)

@Component
class WebSocketController(
    val gameInstanceProvider: GameInstanceProvider,
) : WebSocketHandler {

    private val template = UriTemplate(URI_TEMPLATE)

    private val typeValidator = BasicPolymorphicTypeValidator.builder()
        .allowIfBaseType(Event::class.java)
        .allowIfSubType(Event::class.java)
        .allowIfBaseType(PlayerActions::class.java)
        .allowIfSubType(PlayerActions::class.java)
        .build()

    private val objectMapper = jacksonObjectMapper()
        .activateDefaultTypingAsProperty(typeValidator, ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, "__type")

    override fun handle(session: WebSocketSession): Mono<Void> {
        val path = session.handshakeInfo.uri.path
        val instanceId: UUID = UUID.fromString(template.match(path)["instanceId"])
        val playerSlotId: UUID = UUID.fromString(template.match(path)["playerSlotId"])

        return handle<Any, PlayerId>(playerSlotId, instanceId, session)
    }

    private fun <S : Any, PID : PlayerId> handle(playerSlotId: UUID, instanceId: UUID, session: WebSocketSession): Mono<Void> {
        val gameInstance = gameInstanceProvider.gameInstance(instanceId)

        // If the instance or player slot does not exist, close the connection
        val playerSlot = gameInstance?.playerSlots?.get(playerSlotId)
            ?: return Mono.empty()

        playerSlot.increaseReference()

        // TODO: other scope?
        GlobalScope.launch {
            session.receive()
                // Messages must be retained to make Netty not lose it due to 0 message reference count
                .map(WebSocketMessage::retain)
                .asFlow()
                .collect { (playerSlot.sendChannel as SendChannel<PlayerActions>).send(readAction(it)) }
        }

        // The initial state is published directly
        val stateProvider: (PID) -> S = gameInstance.stateProvider as (PID) -> S
        val initialState = stateProvider(playerSlot.playerId as PID)
        val initialStateFlux = Flux.just(session.stateMessage(initialState))
        val events = playerSlot.receiveChannel.receiveAsFlow().asFlux()
            .map { (event, state) -> session.eventMessage(event, state) }

        return session.send(initialStateFlux.concatWith(events))
            .doFinally { playerSlot.decreaseReference() }
    }

    private fun WebSocketSession.eventMessage(data: Any, state: Any) =
        textMessage(objectMapper.writeValueAsString(WrappedEvent(data, state)))

    private fun <A : PlayerActions> readAction(message: WebSocketMessage): A =
        objectMapper.readValue<WrappedAction<A>>(message.payloadAsText).action

    private fun WebSocketSession.stateMessage(state: Any) =
        textMessage(objectMapper.writeValueAsString(WrappedEvent(null, state)))

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
