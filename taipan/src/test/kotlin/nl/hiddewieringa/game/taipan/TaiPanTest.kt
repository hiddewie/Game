package nl.hiddewieringa.game.taipan

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import nl.hiddewieringa.game.core.*
import nl.hiddewieringa.game.core.TwoTeamPlayerId.*
import nl.hiddewieringa.game.taipan.card.*
import nl.hiddewieringa.game.taipan.player.SimpleTaiPanPlayer
import nl.hiddewieringa.game.taipan.player.TaiPanPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.reflect.KClass
import kotlin.time.seconds

class TaiPanTest {

    @Test
    fun playRound() {
        runBlocking {
            val parameters = TaiPanGameParameters(10, 0)
            val game = TaiPan(parameters)
            val players = TwoTeams(
                TwoPlayers<TaiPanPlayer>(SimpleTaiPanPlayer(), SimpleTaiPanPlayer()),
                TwoPlayers(SimpleTaiPanPlayer(), SimpleTaiPanPlayer()),
            )

            val allPlayers = Channel<Pair<TaiPanEvent, TaiPanState>>(capacity = Channel.UNLIMITED)
            val gamePlayerChannel = Channel<kotlin.Triple<TwoTeamPlayerId, TaiPanEvent, TaiPanState>>(capacity = Channel.UNLIMITED)
            val playerGameChannel = Channel<Pair<TwoTeamPlayerId, TaiPanPlayerActions>>(capacity = Channel.UNLIMITED)

            val gameAsserts = channelAssert(allPlayers) {
                assertNext(RoundBegan(1))

                assertNext(TrickBegan(PLAYER2))
                assertNext(
                    PlayerPlayedCards(
                        PLAYER2,
                        MahjongStraight(
                            setOf(
                                2 of Suit.HEARTS,
                                3 of Suit.HEARTS,
                                4 of Suit.CLUBS,
                                5 of Suit.HEARTS,
                                6 of Suit.HEARTS,
                                7 of Suit.SPADES
                            ),
                            Mahjong
                        )
                    )
                )
                assertNext(MahjongWishRequested(8))
                assertNext(PlayerFolds(PLAYER3))
                assertNext(PlayerFolds(PLAYER4))
                assertNext(PlayerFolds(PLAYER1))
                assertNext(TrickWon(PLAYER2))

                assertNext(TrickBegan(PLAYER2))
                assertNext(PlayerPlayedCards(PLAYER2, PhoenixTriple(NumberedCard.QUEEN of Suit.HEARTS, NumberedCard.QUEEN of Suit.SPADES, Phoenix)))
                assertNext(PlayerFolds(PLAYER3))
                assertNext(PlayerFolds(PLAYER4))
                assertNext(PlayerFolds(PLAYER1))
                assertNext(TrickWon(PLAYER2))

                assertNext(TrickBegan(PLAYER2))
                assertNext(PlayerPlayedCards(PLAYER2, HighCard(2 of Suit.DIAMONDS)))
                assertNext(PlayerPlayedCards(PLAYER3, HighCard(8 of Suit.CLUBS)))
                assertNext(MahjongWishFulfilled)
                assertNext(PlayerFolds(PLAYER4))
                assertNext(PlayerFolds(PLAYER1))
                assertNext(PlayerPlayedCards(PLAYER2, HighCard(9 of Suit.CLUBS)))
                assertNext(PlayerFolds(PLAYER3))
                assertNext(PlayerFolds(PLAYER4))
                assertNext(PlayerFolds(PLAYER1))
                assertNext(TrickWon(PLAYER2))

                assertNext(TrickBegan(PLAYER2))
                assertNext(PlayerPlayedCards(PLAYER2, HighCard(3 of Suit.DIAMONDS)))
                assertNext(PlayerFolds(PLAYER3))
                assertNext(PlayerFolds(PLAYER4))
                assertNext(PlayerFolds(PLAYER1))
                assertNext(TrickWon(PLAYER2))

                assertNext(TrickBegan(PLAYER2))
                assertNext(PlayerPlayedCards(PLAYER2, HighCard(10 of Suit.CLUBS)))
                assertNext(PlayerFolds(PLAYER3))
                assertNext(PlayerFolds(PLAYER4))
                assertNext(PlayerFolds(PLAYER1))
                assertNext(TrickWon(PLAYER2))

                assertNext(TrickBegan(PLAYER3))
                assertNext(
                    PlayerPlayedCards(
                        PLAYER3,
                        FullHouse(NumberedTuple(7 of Suit.HEARTS, 7 of Suit.DIAMONDS), NumberedTriple(5 of Suit.DIAMONDS, 5 of Suit.SPADES, 5 of Suit.CLUBS))
                    )
                )
                assertNext(
                    PlayerPlayedCards(
                        PLAYER4,
                        FullHouse(NumberedTuple(8 of Suit.HEARTS, 8 of Suit.SPADES), NumberedTriple(6 of Suit.DIAMONDS, 6 of Suit.SPADES, 6 of Suit.CLUBS))
                    )
                )
                assertNext(PlayerFolds(PLAYER1))
                assertNext(PlayerFolds(PLAYER2))
                assertNext(PlayerFolds(PLAYER3))
                assertNext(TrickWon(PLAYER4))

                assertNext(TrickBegan(PLAYER4))
                assertNext(PlayerPlayedCards(PLAYER4, HighCard(2 of Suit.SPADES)))
                assertNext(PlayerFolds(PLAYER1))
                assertNext(PlayerFolds(PLAYER2))
                assertNext(PlayerFolds(PLAYER3))
                assertNext(TrickWon(PLAYER4))

                assertNext(TrickBegan(PLAYER4))
                assertNext(PlayerPlayedCards(PLAYER4, HighCard(3 of Suit.SPADES)))
                assertNext(PlayerFolds(PLAYER1))
                assertNext(PlayerFolds(PLAYER2))
                assertNext(PlayerFolds(PLAYER3))
                assertNext(TrickWon(PLAYER4))

                assertNext(TrickBegan(PLAYER4))
                assertNext(PlayerPlayedCards(PLAYER4, HighCard(4 of Suit.DIAMONDS)))
                assertNext(PlayerFolds(PLAYER1))
                assertNext(PlayerFolds(PLAYER2))
                assertNext(PlayerFolds(PLAYER3))
                assertNext(TrickWon(PLAYER4))

                assertNext(TrickBegan(PLAYER4))
                assertNext(PlayerPlayedCards(PLAYER4, HighCard(9 of Suit.SPADES)))
                assertNext(PlayerFolds(PLAYER1))
                assertNext(PlayerFolds(PLAYER2))
                assertNext(PlayerFolds(PLAYER3))
                assertNext(TrickWon(PLAYER4))

                assertNext(TrickBegan(PLAYER4))
                assertNext(PlayerPlayedCards(PLAYER4, HighCard(10 of Suit.SPADES)))
                assertNext(PlayerFolds(PLAYER1))
                assertNext(PlayerFolds(PLAYER2))
                assertNext(PlayerFolds(PLAYER3))
                assertNext(TrickWon(PLAYER4))

                assertNext(TrickBegan(PLAYER4))
                assertNext(PlayerPlayedCards(PLAYER4, HighCard(NumberedCard.JACK of Suit.DIAMONDS)))
                assertNext(PlayerFolds(PLAYER1))
                assertNext(PlayerFolds(PLAYER2))
                assertNext(PlayerFolds(PLAYER3))
                assertNext(TrickWon(PLAYER4))

                assertNext(TrickBegan(PLAYER4))
                assertNext(PlayerPlayedCards(PLAYER4, NumberedTriple(NumberedCard.KING of Suit.HEARTS, NumberedCard.KING of Suit.DIAMONDS, NumberedCard.KING of Suit.CLUBS)))
                assertNext(PlayerFolds(PLAYER1))
                assertNext(PlayerFolds(PLAYER2))
                assertNext(PlayerFolds(PLAYER3))
                assertNext(TrickWon(PLAYER4))

                assertNext(RoundEnded(1, mapOf(TwoTeamTeamId.TEAM1 to 0, TwoTeamTeamId.TEAM2 to 200)))
                assertNext(ScoreUpdated(mapOf(TwoTeamTeamId.TEAM1 to 0, TwoTeamTeamId.TEAM2 to 200)))
            }

            val playerAsserts = channelAssert(gamePlayerChannel) {
                assertNext(PLAYER1, CardsHaveBeenDealt::class)
                assertNext(PLAYER2, CardsHaveBeenDealt::class)
                assertNext(PLAYER3, CardsHaveBeenDealt::class)
                assertNext(PLAYER4, CardsHaveBeenDealt::class)

                playerGameChannel.send(PLAYER1 to RequestNextCards)
                playerGameChannel.send(PLAYER2 to RequestNextCards)
                playerGameChannel.send(PLAYER3 to RequestNextCards)
                playerGameChannel.send(PLAYER4 to RequestNextCards)

                assertNext(PLAYER1, CardsHaveBeenDealt::class)
                assertNext(PLAYER2, CardsHaveBeenDealt::class)
                assertNext(PLAYER3, CardsHaveBeenDealt::class)
                assertNext(PLAYER4, CardsHaveBeenDealt::class)

                assertNext(PLAYER1, RequestPassCards::class)
                assertNext(PLAYER2, RequestPassCards::class)
                assertNext(PLAYER3, RequestPassCards::class)
                assertNext(PLAYER4, RequestPassCards::class)

                playerGameChannel.send(PLAYER4 to CardPass(Dog, NumberedCard.QUEEN of Suit.HEARTS, Dragon))
                playerGameChannel.send(PLAYER2 to CardPass(7 of Suit.HEARTS, NumberedCard.KING of Suit.CLUBS, 9 of Suit.HEARTS))
                playerGameChannel.send(PLAYER1 to CardPass(2 of Suit.HEARTS, NumberedCard.KING of Suit.SPADES, 3 of Suit.SPADES))
                playerGameChannel.send(PLAYER3 to CardPass(2 of Suit.SPADES, NumberedCard.QUEEN of Suit.CLUBS, 3 of Suit.HEARTS))

                assertNext(PLAYER1, CardsHaveBeenPassed::class)
                assertNext(PLAYER2, CardsHaveBeenPassed::class)
                assertNext(PLAYER3, CardsHaveBeenPassed::class)
                assertNext(PLAYER4, CardsHaveBeenPassed::class)

                assertNext(PLAYER2, RequestPlayCards::class)
                playerGameChannel.send(
                    PLAYER2 to PlayCards(
                        setOf(
                            Mahjong,
                            2 of Suit.HEARTS,
                            3 of Suit.HEARTS,
                            4 of Suit.CLUBS,
                            5 of Suit.HEARTS,
                            6 of Suit.HEARTS,
                            7 of Suit.SPADES
                        ),
                        setOf(MahjongRequest(8))
                    )
                )

                assertNext(PLAYER3, RequestPlayCards::class)
                playerGameChannel.send(PLAYER3 to Fold)

                assertNext(PLAYER4, RequestPlayCards::class)
                playerGameChannel.send(PLAYER4 to Fold)

                assertNext(PLAYER1, RequestPlayCards::class)
                playerGameChannel.send(PLAYER1 to Fold)

                assertNext(PLAYER2, RequestPlayCards::class)
                playerGameChannel.send(
                    PLAYER2 to PlayCards(
                        setOf(
                            NumberedCard.QUEEN of Suit.SPADES,
                            NumberedCard.QUEEN of Suit.HEARTS,
                            Phoenix,
                        )
                    )
                )

                assertNext(PLAYER3, RequestPlayCards::class)
                playerGameChannel.send(PLAYER3 to Fold)

                assertNext(PLAYER4, RequestPlayCards::class)
                playerGameChannel.send(PLAYER4 to Fold)

                assertNext(PLAYER1, RequestPlayCards::class)
                playerGameChannel.send(PLAYER1 to Fold)

                assertNext(PLAYER2, RequestPlayCards::class)
                playerGameChannel.send(PLAYER2 to PlayCards(setOf(2 of Suit.DIAMONDS)))

                assertNext(PLAYER3, RequestPlayCards::class)
                playerGameChannel.send(PLAYER3 to PlayCards(setOf(8 of Suit.CLUBS)))

                assertNext(PLAYER4, RequestPlayCards::class)
                playerGameChannel.send(PLAYER4 to Fold)

                assertNext(PLAYER1, RequestPlayCards::class)
                playerGameChannel.send(PLAYER1 to Fold)

                assertNext(PLAYER2, RequestPlayCards::class)
                playerGameChannel.send(PLAYER2 to PlayCards(setOf(9 of Suit.CLUBS)))

                assertNext(PLAYER3, RequestPlayCards::class)
                playerGameChannel.send(PLAYER3 to Fold)

                assertNext(PLAYER4, RequestPlayCards::class)
                playerGameChannel.send(PLAYER4 to Fold)

                assertNext(PLAYER1, RequestPlayCards::class)
                playerGameChannel.send(PLAYER1 to Fold)

                assertNext(PLAYER2, RequestPlayCards::class)
                playerGameChannel.send(PLAYER2 to PlayCards(setOf(3 of Suit.DIAMONDS)))

                assertNext(PLAYER3, RequestPlayCards::class)
                playerGameChannel.send(PLAYER3 to Fold)

                assertNext(PLAYER4, RequestPlayCards::class)
                playerGameChannel.send(PLAYER4 to Fold)

                assertNext(PLAYER1, RequestPlayCards::class)
                playerGameChannel.send(PLAYER1 to Fold)

                assertNext(PLAYER2, RequestPlayCards::class)
                playerGameChannel.send(PLAYER2 to PlayCards(setOf(10 of Suit.CLUBS)))

                assertNext(PLAYER3, RequestPlayCards::class)
                playerGameChannel.send(PLAYER3 to Fold)

                assertNext(PLAYER4, RequestPlayCards::class)
                playerGameChannel.send(PLAYER4 to Fold)

                assertNext(PLAYER1, RequestPlayCards::class)
                playerGameChannel.send(PLAYER1 to Fold)

                assertNext(PLAYER3, RequestPlayCards::class)
                playerGameChannel.send(
                    PLAYER3 to PlayCards(
                        setOf(
                            7 of Suit.HEARTS,
                            7 of Suit.DIAMONDS,
                            5 of Suit.DIAMONDS,
                            5 of Suit.CLUBS,
                            5 of Suit.SPADES
                        )
                    )
                )

                assertNext(PLAYER4, RequestPlayCards::class)
                playerGameChannel.send(
                    PLAYER4 to PlayCards(
                        setOf(
                            8 of Suit.HEARTS,
                            8 of Suit.SPADES,
                            6 of Suit.DIAMONDS,
                            6 of Suit.CLUBS,
                            6 of Suit.SPADES
                        )
                    )
                )

                assertNext(PLAYER1, RequestPlayCards::class)
                playerGameChannel.send(PLAYER1 to Fold)

                assertNext(PLAYER3, RequestPlayCards::class)
                playerGameChannel.send(PLAYER3 to Fold)

                assertNext(PLAYER4, RequestPlayCards::class)
                playerGameChannel.send(PLAYER4 to PlayCards(setOf(2 of Suit.SPADES)))

                assertNext(PLAYER1, RequestPlayCards::class)
                playerGameChannel.send(PLAYER1 to Fold)

                assertNext(PLAYER3, RequestPlayCards::class)
                playerGameChannel.send(PLAYER3 to Fold)

                assertNext(PLAYER4, RequestPlayCards::class)
                playerGameChannel.send(PLAYER4 to PlayCards(setOf(3 of Suit.SPADES)))

                assertNext(PLAYER1, RequestPlayCards::class)
                playerGameChannel.send(PLAYER1 to Fold)

                assertNext(PLAYER3, RequestPlayCards::class)
                playerGameChannel.send(PLAYER3 to Fold)

                assertNext(PLAYER4, RequestPlayCards::class)
                playerGameChannel.send(PLAYER4 to PlayCards(setOf(4 of Suit.DIAMONDS)))

                assertNext(PLAYER1, RequestPlayCards::class)
                playerGameChannel.send(PLAYER1 to Fold)

                assertNext(PLAYER3, RequestPlayCards::class)
                playerGameChannel.send(PLAYER3 to Fold)

                assertNext(PLAYER4, RequestPlayCards::class)
                playerGameChannel.send(PLAYER4 to PlayCards(setOf(9 of Suit.SPADES)))

                assertNext(PLAYER1, RequestPlayCards::class)
                playerGameChannel.send(PLAYER1 to Fold)

                assertNext(PLAYER3, RequestPlayCards::class)
                playerGameChannel.send(PLAYER3 to Fold)

                assertNext(PLAYER4, RequestPlayCards::class)
                playerGameChannel.send(PLAYER4 to PlayCards(setOf(10 of Suit.SPADES)))

                assertNext(PLAYER1, RequestPlayCards::class)
                playerGameChannel.send(PLAYER1 to Fold)

                assertNext(PLAYER3, RequestPlayCards::class)
                playerGameChannel.send(PLAYER3 to Fold)

                assertNext(PLAYER4, RequestPlayCards::class)
                playerGameChannel.send(PLAYER4 to PlayCards(setOf(NumberedCard.JACK of Suit.DIAMONDS)))

                assertNext(PLAYER1, RequestPlayCards::class)
                playerGameChannel.send(PLAYER1 to Fold)

                assertNext(PLAYER3, RequestPlayCards::class)
                playerGameChannel.send(PLAYER3 to Fold)

                assertNext(PLAYER4, RequestPlayCards::class)
                playerGameChannel.send(PLAYER4 to PlayCards(setOf(NumberedCard.KING of Suit.HEARTS, NumberedCard.KING of Suit.DIAMONDS, NumberedCard.KING of Suit.CLUBS)))

                assertNext(PLAYER1, RequestPlayCards::class)
                playerGameChannel.send(PLAYER1 to Fold)

                assertNext(PLAYER3, RequestPlayCards::class)
                playerGameChannel.send(PLAYER3 to Fold)
            }

            launch {
                withTimeout(2.seconds) {
                    val gameResult = game.play(GameContext(players, { game.state }, allPlayers, gamePlayerChannel, playerGameChannel))
                    assertEquals(Team2Won, gameResult)
                }
            }

            withTimeout(1.seconds) {
                gameAsserts.await()
                playerAsserts.await()
            }

            if (!allPlayers.isEmpty) {
                val messages = buildList {
                    while (!allPlayers.isEmpty) {
                        add(allPlayers.poll())
                    }
                }

                allPlayers.close()
                fail { "Leftover messages for all players: $messages" }
            }
            if (!gamePlayerChannel.isEmpty) {
                val messages = buildList {
                    while (!gamePlayerChannel.isEmpty) {
                        add(gamePlayerChannel.poll())
                    }
                }
                gamePlayerChannel.close()
                fail { "Leftover messages for players: $messages" }
            }
        }
    }

    class ChannelAsserter<E : Any>(
        internal val channel: ReceiveChannel<E>
    ) {
        suspend fun <T : E> assertNext(value: T) =
            assertEquals(value, channel.receive())
    }

    private fun <E : Any> CoroutineScope.channelAssert(channel: ReceiveChannel<E>, asserter: suspend ChannelAsserter<E>.() -> Unit) =
        async {
            ChannelAsserter(channel).asserter()
        }

    private suspend fun <A : Any, B : Any> ChannelAsserter<Pair<A, B>>.assertNext(first: A) {
        val (key, value) = channel.receive()
        assertTrue(key == first) { "Got ($key, $value), expected ($first)" }
    }

    private suspend fun <A : Any, B : Any, T : B, C : Any> ChannelAsserter<kotlin.Triple<A, B, C>>.assertNext(first: A, second: KClass<T>) {
        val (a, b, c) = channel.receive()
        assertTrue(a == first && second.isInstance(b)) { "Got ($a, $b, $c), expected ($first, instance of $second)" }
    }
}
