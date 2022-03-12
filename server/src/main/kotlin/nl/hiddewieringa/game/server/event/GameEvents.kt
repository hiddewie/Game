package nl.hiddewieringa.game.server.event

import io.vertx.core.Vertx
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KLogging
import nl.hiddewieringa.game.core.Event
import nl.hiddewieringa.game.server.games.GameProvider
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow
import kotlin.math.roundToLong

interface GameEvents {

    fun <E : Event, S : Any> publish(gameSlug: String, instanceId: UUID, playerSlotId: UUID, event: E, state: S)

    fun <E : Event, S : Any> receiveEvents(instanceId: UUID, playerSlotId: UUID): Flow<Pair<E, S>>

}

@Serializable
data class GameEvent(
    val instanceId: String,
    val playerSlotId: String,
    val gameSlug: String,
    val serializedEvent: String,
    val serializedPlayerState: String,
)

@ConfigurationProperties("postgres")
class PostgresConfiguration {

    var host: String = "127.0.0.1"
    var port: Int = 5432
    lateinit var database: String
    lateinit var user: String
    lateinit var password: String

}

@Configuration
class PostgresEventSetup {

    @Bean
    fun vertx(): Vertx =
        Vertx.vertx()

    @Bean
    fun postgresConnectOptions(postgresConfiguration: PostgresConfiguration): PgConnectOptions =
        PgConnectOptions()
            .setPort(postgresConfiguration.port)
            .setHost(postgresConfiguration.host)
            .setDatabase(postgresConfiguration.database)
            .setUser(postgresConfiguration.user)
            .setPassword(postgresConfiguration.password)

    @Bean
    fun postgresPoolOptions(): PoolOptions =
        PoolOptions()
            .setMaxSize(5)

    @Bean(destroyMethod = "close")
    fun postgresClient(vertx: Vertx, connectOptions: PgConnectOptions, poolOptions: PoolOptions): SqlClient =
        PgPool.client(vertx, connectOptions, poolOptions)

    @Bean(initMethod = "connect", destroyMethod = "close")
    fun postgresSubscriber(vertx: Vertx, connectOptions: PgConnectOptions): PgSubscriber =
        PgSubscriber.subscriber(vertx, connectOptions)
            .reconnectPolicy { retries ->
                100 * 2.0.pow(retries.toDouble()).roundToLong()
            }

}

@Service
class PostgresPubSubGameEvents(
    val gameProvider: GameProvider,
    val postgresClient: SqlClient,
    val postgresSubscriber: PgSubscriber,
) : GameEvents, InitializingBean, DisposableBean {

    private val flows: MutableMap<Pair<UUID, *>, MutableSharedFlow<Pair<*, *>>> = ConcurrentHashMap()

    // TODO make service
    private val serializer = Json.Default

    override fun afterPropertiesSet() {
        postgresSubscriber.channel(EVENT_CHANNEL_NAME)
            .handler(this::receive)

        logger.info("Subscribed to game events channel")
    }

    override fun destroy() {
        postgresSubscriber.channel(EVENT_CHANNEL_NAME)
            .handler(null)

        logger.info("Unsubscribed from game events channel")
    }

    private fun receive(message: String) {
        try {
            val gameEvent = serializer.decodeFromString(GameEvent.serializer(), message)
            val game = gameProvider.bySlug(gameEvent.gameSlug)
            val event = serializer.decodeFromString(game.eventSerializer, gameEvent.serializedEvent)
            val state = serializer.decodeFromString(game.playerStateSerializer, gameEvent.serializedPlayerState)

            logger.info("Received event message for instance ${gameEvent.instanceId} and player slot ${gameEvent.playerSlotId}")

            val emitSuccess = flowFor(UUID.fromString(gameEvent.instanceId), UUID.fromString(gameEvent.playerSlotId))
                .tryEmit(event to state)

            if (emitSuccess) {
                logger.debug("Emitted event to instance ${gameEvent.instanceId} for player slot ${gameEvent.playerSlotId}")
            } else {
                logger.warn("Could not emit event to instance ${gameEvent.instanceId} for player slot ${gameEvent.playerSlotId}")
            }
        } catch (e: Exception) {
            logger.error("Uncaught Exception ${e.message}", e)
        }
    }

    private fun publish(message: String) {
        val escapedMessage = message.replace("'", "\\'")

        postgresClient
            .query("NOTIFY \"$EVENT_CHANNEL_NAME\", '$escapedMessage'")
            .execute { ar ->
                if (ar.failed()) {
                    logger.warn { "Failed to publish message '$message', cause ${ar.cause()}" }
                }
            }
    }

    override fun <E : Event, S : Any> publish(gameSlug: String, instanceId: UUID, playerSlotId: UUID, event: E, state: S) {
        val game = gameProvider.bySlug(gameSlug)

        val serializedEvent = serializer.encodeToString(game.eventSerializer as KSerializer<E>, event)
        val serializedPlayerState = serializer.encodeToString(game.playerStateSerializer as KSerializer<S>, state)
        val gameEvent = GameEvent(instanceId.toString(), playerSlotId.toString(), gameSlug, serializedEvent, serializedPlayerState)

        val message = serializer.encodeToString(GameEvent.serializer(), gameEvent)
        publish(message)

        logger.info("Publishing event for instance $instanceId and player slot $playerSlotId")
    }

    override fun <E : Event, S : Any> receiveEvents(instanceId: UUID, playerSlotId: UUID): Flow<Pair<E, S>> =
        flowFor(instanceId, playerSlotId)
            .map { it.first as E to it.second as S }

    private fun flowFor(instanceId: UUID, playerSlotId: UUID): MutableSharedFlow<Pair<*, *>> =
        flows.getOrPut(instanceId to playerSlotId) {
            logger.info("Creating new local mutable shared flow for instance $instanceId and player slot $playerSlotId")
            val ret = MutableSharedFlow<Pair<*, *>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
            ret
        }

    companion object : KLogging() {
        const val EVENT_CHANNEL_NAME = "game"
    }
}