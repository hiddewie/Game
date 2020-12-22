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
import nl.hiddewieringa.game.core.Event
import nl.hiddewieringa.game.core.GameState
import nl.hiddewieringa.game.core.PlayerActions
import nl.hiddewieringa.game.server.games.GameDetails
import nl.hiddewieringa.game.server.games.GameInstance
import nl.hiddewieringa.game.server.games.GameInstanceProvider
import nl.hiddewieringa.game.server.games.GameProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@RestController
class HomeController(
    val gameProvider: GameProvider,
    val gameInstanceProvider: GameInstanceProvider,
) {

    @GetMapping("games")
    fun games(): List<GameDetails<*, *, *, *, *, *, *, *>> =
        gameProvider.games()

    data class OpenGame(val id: UUID, val playerSlotIds: Set<UUID>)

    @GetMapping("games/{gameId}/open")
    fun openGames(@PathVariable gameId: UUID): List<OpenGame> =
        gameInstanceProvider.openGames(gameId)
            .map { OpenGame(it.id, it.playerSlots.keys) }

    @GetMapping("games/{gameId}/instance/{instanceId}")
    fun instanceDetails(@PathVariable gameId: UUID, @PathVariable instanceId: UUID): GameInstance<*, *, *>? =
        gameInstanceProvider.gameInstance(gameId)

    @PostMapping("games/{gameId}/start")
    suspend fun startGame(@PathVariable gameId: UUID): UUID =
        gameInstanceProvider.start(gameProvider.byId(gameId))
}

@Component
class ReactiveWebSocketHandler(
    val gameInstanceProvider: GameInstanceProvider,
) : WebSocketHandler {

    private val template = UriTemplate(URI_TEMPLATE)

    // TODO make generic
    private val typeValidator = BasicPolymorphicTypeValidator.builder()
        .allowIfBaseType(Event::class.java)
        .allowIfSubType(Event::class.java)
        .allowIfBaseType(PlayerActions::class.java)
        .allowIfSubType(PlayerActions::class.java)
        .build()

    private val objectMapper = jacksonObjectMapper()
        .activateDefaultTypingAsProperty(typeValidator, ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, "@type")

    data class WrappedAction<A : PlayerActions>(val action: A)
    data class WrappedEvent<E : Event, S : GameState>(val event: E?, val state: S)

    override fun handle(session: WebSocketSession): Mono<Void> {
        val path = session.handshakeInfo.uri.path
        val instanceId: UUID = UUID.fromString(template.match(path)["instanceId"])
        val playerSlotId: UUID = UUID.fromString(template.match(path)["playerSlotId"])

        return handle(playerSlotId, instanceId, session)
    }

    private fun handle(playerSlotId: UUID, instanceId: UUID, session: WebSocketSession): Mono<Void> {
        println("player slot $playerSlotId")

        val gameInstance = gameInstanceProvider.gameInstance(instanceId)
        val playerSlots = gameInstance?.playerSlots?.get(playerSlotId)
            ?: return Mono.empty()

        // TODO: other scope?
        GlobalScope.launch {
            session.receive()
                // Messages must be retained to make Netty not lose it due to 0 message reference count
                .map(WebSocketMessage::retain)
                .asFlow()
                .collect { (playerSlots.sendChannel as SendChannel<PlayerActions>).send(readAction(it)) }
        }

        // The initial state is published directly
        val initialState = Flux.generate<WebSocketMessage> { session.stateMessage(gameInstance.stateProvider()) }
        val events = playerSlots.receiveChannel.receiveAsFlow().asFlux()
            .map { (event, state) -> session.eventMessage(event, state) }

        return session.send(initialState.concatWith(events))
    }

    private fun <E : Event, S : GameState> WebSocketSession.eventMessage(data: E, state: S) =
        textMessage(objectMapper.writeValueAsString(WrappedEvent(data, state)))

    private fun <A : PlayerActions> readAction(message: WebSocketMessage): A =
        objectMapper.readValue<WrappedAction<A>>(message.payloadAsText).action

    private fun <S : GameState> WebSocketSession.stateMessage(state: S) =
        textMessage(objectMapper.writeValueAsString(WrappedEvent(null, state)))

    companion object {
        const val URI_TEMPLATE = "/interaction/{instanceId}/{playerSlotId}"
    }
}

@Configuration
class WebsocketConfiguration {

    @Bean
    fun webSocketHandlerMapping(webSocketHandler: WebSocketHandler): HandlerMapping =
        SimpleUrlHandlerMapping(
            mapOf(
                ReactiveWebSocketHandler.URI_TEMPLATE to webSocketHandler,
            ),
            1
        )
}
