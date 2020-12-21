package nl.hiddewieringa.game.server.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import nl.hiddewieringa.game.core.Event
import nl.hiddewieringa.game.core.PlayerActions
import nl.hiddewieringa.game.server.games.GameDetails
import nl.hiddewieringa.game.server.games.GameInstance
import nl.hiddewieringa.game.server.games.GameInstanceProvider
import nl.hiddewieringa.game.server.games.GameProvider
import nl.hiddewieringa.game.tictactoe.TicTacToeEvent
import nl.hiddewieringa.game.tictactoe.TicTacToePlayerActions
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
import reactor.core.publisher.Mono
import java.util.*

@RestController
class HomeController(
    val gameProvider: GameProvider,
    val gameInstanceProvider: GameInstanceProvider,
) {

    @GetMapping("games")
    fun games(): List<GameDetails> =
        gameProvider.games()

    @GetMapping("games/{gameId}/open")
    fun openGames(@PathVariable gameId: UUID): List<GameInstance> =
        gameInstanceProvider.openGames(gameId)

    @GetMapping("games/{gameId}/instance/{instanceId}")
    fun instanceDetails(@PathVariable gameId: UUID, @PathVariable instanceId: UUID): GameInstance? =
        gameInstanceProvider.gameInstance(gameId)

    @PostMapping("games/{gameId}/start")
    suspend fun startGame(@PathVariable gameId: UUID): UUID =
        gameInstanceProvider.startGame(gameId)
}

@Component
class ReactiveWebSocketHandler(
    val gameInstanceProvider: GameInstanceProvider,
) : WebSocketHandler {

    private val template = UriTemplate("/interaction/{instanceId}")

    private val typeValidator = BasicPolymorphicTypeValidator.builder()
        .allowIfBaseType(TicTacToeEvent::class.java)
        .allowIfSubType(TicTacToeEvent::class.java)
        .allowIfBaseType(TicTacToePlayerActions::class.java)
        .allowIfSubType(TicTacToePlayerActions::class.java)
        .build()

    private val objectMapper = jacksonObjectMapper()
        .activateDefaultTypingAsProperty(typeValidator, ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, "@type")

    data class WrappedAction<T : PlayerActions>(val action: T)
    data class WrappedEvent<T : Event>(val event: T)

    override fun handle(session: WebSocketSession): Mono<Void> {
        val instanceId: UUID = UUID.fromString(template.match(session.handshakeInfo.uri.path)["instanceId"])

        val playerSlots = gameInstanceProvider.gameInstance(instanceId)?.playerSlots?.get(0)
            ?: return Mono.empty()

        // TODO: other scope?
        GlobalScope.launch {
            session.receive()
                // Messages must be retained to make Netty not lose it due to 0 message reference count
                .map(WebSocketMessage::retain)
                .asFlow()
                .collect { playerSlots.sendChannel.send(objectMapper.readValue<WrappedAction<TicTacToePlayerActions>>(it.payloadAsText).action) }
        }

        val output = playerSlots.receiveChannel.receiveAsFlow().asFlux()
            .map { session.textMessage(objectMapper.writeValueAsString(WrappedEvent(it))) }

        return session.send(output)
    }
}

@Configuration
class WebsocketConfiguration {

    @Bean
    fun webSocketHandlerMapping(webSocketHandler: WebSocketHandler): HandlerMapping =
        SimpleUrlHandlerMapping(
            mapOf(
                "/interaction/{instanceId}" to webSocketHandler,
            ),
            1
        )
}
