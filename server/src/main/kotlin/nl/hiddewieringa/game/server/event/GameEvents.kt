package nl.hiddewieringa.game.server.event

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.cloud.pubsub.v1.*
import com.google.common.util.concurrent.MoreExecutors
import com.google.protobuf.ByteString
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PubsubMessage
import com.google.pubsub.v1.PushConfig
import com.google.pubsub.v1.TopicName
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KLogging
import nl.hiddewieringa.game.core.Event
import nl.hiddewieringa.game.server.application.InstanceId
import nl.hiddewieringa.game.server.games.GameProvider
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

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

@Service
class GooglePubSubGameEvents(
    instanceId: InstanceId,
    private val gameProvider: GameProvider,
) : GameEvents, MessageReceiver, InitializingBean, DisposableBean {

    private val subscriptionAdmin = SubscriptionAdminClient.create()
    private val subscriptionName = ProjectSubscriptionName.of(PROJECT_ID, "instance-${instanceId.id}")

    private val flows: MutableMap<Pair<UUID, *>, MutableSharedFlow<Pair<*, *>>> = ConcurrentHashMap()

    private var subscriber: Subscriber? = null
    private var publisher: Publisher? = null

    // TODO make service
    private val serializer = Json.Default

    override fun afterPropertiesSet() {
        subscriptionAdmin.createSubscription(subscriptionName, TOPIC_NAME, PushConfig.getDefaultInstance(), SUBSCRIPTION_DEADLINE.toSeconds().toInt())
        logger.info("Created subscription $subscriptionName on topic $TOPIC")

        subscriber = Subscriber.newBuilder(subscriptionName, this).build()
        subscriber?.startAsync()
        logger.info("Started async event subscriber")

        publisher = Publisher.newBuilder(TOPIC_NAME).build()
        logger.info("Created publisher")
    }

    override fun destroy() {
        subscriptionAdmin.deleteSubscription(subscriptionName)
        logger.info("Deleted subscription $subscriptionName")

        subscriber?.stopAsync()
        logger.info("Stopped async event subscriber")

        logger.info("Shutting down publisher")
        publisher?.shutdown()
        publisher?.awaitTermination(10, TimeUnit.SECONDS)
        logger.info("Shut down publisher")
    }

    override fun receiveMessage(message: PubsubMessage, consumer: AckReplyConsumer) {
        try {
            val messageData = message.data.toStringUtf8()

            val gameEvent = serializer.decodeFromString(GameEvent.serializer(), messageData)
            val game = gameProvider.bySlug(gameEvent.gameSlug)
            val event = serializer.decodeFromString(game.eventSerializer, gameEvent.serializedEvent)
            val state = serializer.decodeFromString(game.playerStateSerializer, gameEvent.serializedPlayerState)

            logger.info("Received event message for instance ${gameEvent.instanceId} and player slot ${gameEvent.playerSlotId}")

            val emitSuccess = flowFor(UUID.fromString(gameEvent.instanceId), UUID.fromString(gameEvent.playerSlotId))
                .tryEmit(event to state)

            if (emitSuccess) {
                consumer.ack()
                logger.debug("Emitted event to instance ${gameEvent.instanceId} for player slot ${gameEvent.playerSlotId}")
            } else {
                logger.warn("Could not emit event to instance ${gameEvent.instanceId} for player slot ${gameEvent.playerSlotId}")
            }
        } catch (e: Exception) {
            logger.error("Uncaught Exception ${e.message}", e)
        }
    }

    override fun <E : Event, S : Any> publish(gameSlug: String, instanceId: UUID, playerSlotId: UUID, event: E, state: S) {
        val game = gameProvider.bySlug(gameSlug)

        val serializedEvent = serializer.encodeToString(game.eventSerializer as KSerializer<E>, event)
        val serializedPlayerState = serializer.encodeToString(game.playerStateSerializer as KSerializer<S>, state)
        val gameEvent = GameEvent(instanceId.toString(), playerSlotId.toString(), gameSlug, serializedEvent, serializedPlayerState)

        val messageData = serializer.encodeToString(GameEvent.serializer(), gameEvent)
        val message = PubsubMessage.newBuilder()
            .setData(ByteString.copyFromUtf8(messageData))
            .build()

        logger.info("Publishing event for instance $instanceId and player slot $playerSlotId")

        val publishFuture = publisher?.publish(message)
        if (publishFuture != null) {
            ApiFutures.addCallback(
                publishFuture,
                object : ApiFutureCallback<String> {
                    override fun onFailure(error: Throwable) =
                        logger.warn("Error during publishing of message of instance ${gameEvent.instanceId} for player slot ${gameEvent.playerSlotId}", error)

                    override fun onSuccess(result: String) =
                        logger.debug("Successfully published message for instance ${gameEvent.instanceId} for player slot ${gameEvent.playerSlotId}")
                },
                MoreExecutors.directExecutor()
            )
        }
    }

    override fun <E : Event, S : Any> receiveEvents(instanceId: UUID, playerSlotId: UUID): Flow<Pair<E, S>> =
        flowFor(instanceId, playerSlotId)
            .map { it.first as E to it.second as S }

    private fun flowFor(instanceId: UUID, playerSlotId: UUID): MutableSharedFlow<Pair<*, *>> =
        flows.getOrPut(instanceId to playerSlotId) {
            logger.info("Creating new local mutable shared flow for instance $instanceId and player slot $playerSlotId")
            val ret = MutableSharedFlow<Pair<*, *>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
//            ret.onSubscription { logger.info("Subscription. Currently ${ret.subscriptionCount}") }
//            ret.onCompletion { logger.info("Completion. Currently ${ret.subscriptionCount}") }
            ret
        }

    companion object : KLogging() {
        private const val PROJECT_ID = "game-318420"
        private const val TOPIC = "game-events"
        private val TOPIC_NAME = TopicName.ofProjectTopicName(PROJECT_ID, TOPIC)
        private val SUBSCRIPTION_DEADLINE = Duration.ofSeconds(10)
    }
}