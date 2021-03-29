package nl.hiddewieringa.game.taipan

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import nl.hiddewieringa.game.core.*
import nl.hiddewieringa.game.core.TwoTeamPlayerId.*
import nl.hiddewieringa.game.taipan.card.*
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.ACE
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.JACK
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.KING
import nl.hiddewieringa.game.taipan.card.NumberedCard.Companion.QUEEN
import nl.hiddewieringa.game.taipan.card.Suit.*
import nl.hiddewieringa.game.taipan.player.SimpleTaiPanPlayer
import nl.hiddewieringa.game.test.taipan.support.runTest
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.seconds

class TaiPanTest {

    @Test
    fun playRound() = runTest {
        val parameters = TaiPanGameParameters(10, 0)
        val game = TaiPan(parameters)
        val players = TwoTeams(
            TwoPlayers(SimpleTaiPanPlayer(), SimpleTaiPanPlayer()),
            TwoPlayers(SimpleTaiPanPlayer(), SimpleTaiPanPlayer()),
        )

        val gamePlayerChannel = Channel<kotlin.Triple<TwoTeamPlayerId, TaiPanEvent, TaiPanState>>(capacity = Channel.UNLIMITED)
        val playerGameChannel = Channel<Pair<TwoTeamPlayerId, TaiPanPlayerActions>>(capacity = Channel.UNLIMITED)

        val playerAsserts = channelAssert(gamePlayerChannel) {
            assertAllPlayersNext(CardsHaveBeenDealt::class)
            assertAllPlayersNext(CardsHaveBeenDealt::class)
            assertAllPlayersNext(CardsHaveBeenDealt::class)
            assertAllPlayersNext(CardsHaveBeenDealt::class)

            playerGameChannel.send(PLAYER1 to RequestNextCards)
            playerGameChannel.send(PLAYER2 to RequestNextCards)
            playerGameChannel.send(PLAYER3 to RequestNextCards)
            playerGameChannel.send(PLAYER4 to RequestNextCards)

            assertAllPlayersNext(CardsHaveBeenDealt::class)
            assertAllPlayersNext(CardsHaveBeenDealt::class)
            assertAllPlayersNext(CardsHaveBeenDealt::class)
            assertAllPlayersNext(CardsHaveBeenDealt::class)

            assertAllPlayersNext(AllPlayersHaveReceivedCards::class)

            playerGameChannel.send(PLAYER1 to CardPass(ThreeWayPass(Dog, ACE of HEARTS, 3 of DIAMONDS)))
            playerGameChannel.send(PLAYER2 to CardPass(ThreeWayPass(3 of SPADES, ACE of SPADES, 9 of HEARTS)))
            playerGameChannel.send(PLAYER4 to CardPass(ThreeWayPass(3 of HEARTS, QUEEN of CLUBS, 6 of HEARTS)))
            playerGameChannel.send(PLAYER3 to CardPass(ThreeWayPass(2 of HEARTS, JACK of DIAMONDS, 6 of SPADES)))

            assertAllPlayersNext(CardsHaveBeenExchanged::class)
            assertAllPlayersNext(CardsHaveBeenExchanged::class)
            assertAllPlayersNext(CardsHaveBeenExchanged::class)
            assertAllPlayersNext(CardsHaveBeenExchanged::class)

            assertAllPlayersNext(AllPlayersHaveExchangedCards::class)

            playerGameChannel.send(
                PLAYER4 to PlayCards(
                    setOf(Mahjong),
                    setOf(MahjongRequest(8)),
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            assertAllPlayersNext(MahjongWishRequested::class)

            playerGameChannel.send(
                PLAYER1 to PlayCards(
                    setOf(8 of DIAMONDS)
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            assertAllPlayersNext(MahjongWishFulfilled::class)
            playerGameChannel.send(
                PLAYER2 to PlayCards(
                    setOf(Dragon)
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            playerGameChannel.send(PLAYER3 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER4 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER1 to Fold)
            assertAllPlayersNext(PlayerFolds::class)

            assertAllPlayersNext(DragonTrickWon::class)

            playerGameChannel.send(
                PLAYER2 to PassDragonTrick(DragonPass.LEFT)
            )
            assertAllPlayersNext(PlayerPassedDragon::class)

            playerGameChannel.send(
                PLAYER2 to PlayCards(
                    setOf(Dog),
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            assertAllPlayersNext(TrickWon::class)

            playerGameChannel.send(
                PLAYER4 to PlayCards(
                    setOf(
                        3 of DIAMONDS
                    ),
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            playerGameChannel.send(PLAYER1 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER2 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER3 to Fold)
            assertAllPlayersNext(PlayerFolds::class)

            assertAllPlayersNext(TrickWon::class)

            playerGameChannel.send(
                PLAYER4 to PlayCards(
                    setOf(
                        5 of HEARTS,
                        5 of DIAMONDS,
                        10 of DIAMONDS,
                        10 of SPADES,
                        10 of CLUBS,
                    ),
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            playerGameChannel.send(PLAYER1 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER2 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER3 to Fold)
            assertAllPlayersNext(PlayerFolds::class)

            assertAllPlayersNext(TrickWon::class)

            playerGameChannel.send(
                PLAYER4 to PlayCards(
                    setOf(
                        JACK of SPADES,
                    ),
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            playerGameChannel.send(PLAYER1 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER2 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER3 to Fold)
            assertAllPlayersNext(PlayerFolds::class)

            assertAllPlayersNext(TrickWon::class)

            playerGameChannel.send(
                PLAYER4 to PlayCards(
                    setOf(
                        9 of DIAMONDS,
                    ),
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            playerGameChannel.send(PLAYER1 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER2 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER3 to Fold)
            assertAllPlayersNext(PlayerFolds::class)

            assertAllPlayersNext(TrickWon::class)

            playerGameChannel.send(
                PLAYER4 to PlayCards(
                    setOf(
                        ACE of SPADES
                    ),
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            playerGameChannel.send(PLAYER1 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER2 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(
                PLAYER4 to PlayCards(
                    setOf(
                        2 of HEARTS,
                        2 of DIAMONDS,
                        2 of SPADES,
                        2 of CLUBS,
                    ),
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            assertAllPlayersNext(PlayerIsOutOfCards::class)
            playerGameChannel.send(PLAYER1 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER2 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER3 to Fold)
            assertAllPlayersNext(PlayerFolds::class)

            // Player 4 folds again because no more cards.
            assertAllPlayersNext(PlayerFolds::class)

            assertAllPlayersNext(TrickWon::class)

            playerGameChannel.send(
                PLAYER1 to PlayCards(
                    setOf(
                        3 of HEARTS,
                    ),
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            playerGameChannel.send(
                PLAYER2 to PlayCards(
                    setOf(
                        Phoenix,
                    ),
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            playerGameChannel.send(PLAYER3 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER1 to Fold)
            assertAllPlayersNext(PlayerFolds::class)

            assertAllPlayersNext(TrickWon::class)

            playerGameChannel.send(
                PLAYER2 to PlayCards(
                    setOf(
                        3 of CLUBS,
                        4 of CLUBS,
                        5 of SPADES,
                        6 of SPADES,
                        7 of DIAMONDS,
                    ),
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            playerGameChannel.send(PLAYER3 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER1 to Fold)
            assertAllPlayersNext(PlayerFolds::class)

            assertAllPlayersNext(TrickWon::class)

            playerGameChannel.send(
                PLAYER2 to PlayCards(
                    setOf(
                        6 of DIAMONDS
                    ),
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            playerGameChannel.send(PLAYER3 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER1 to Fold)
            assertAllPlayersNext(PlayerFolds::class)

            assertAllPlayersNext(TrickWon::class)

            playerGameChannel.send(
                PLAYER2 to PlayCards(
                    setOf(
                        10 of HEARTS
                    ),
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            playerGameChannel.send(PLAYER3 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER1 to Fold)
            assertAllPlayersNext(PlayerFolds::class)

            assertAllPlayersNext(TrickWon::class)

            playerGameChannel.send(
                PLAYER2 to PlayCards(
                    setOf(
                        JACK of HEARTS,
                        JACK of CLUBS,
                    ),
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            playerGameChannel.send(PLAYER3 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER1 to Fold)
            assertAllPlayersNext(PlayerFolds::class)

            assertAllPlayersNext(TrickWon::class)

            playerGameChannel.send(
                PLAYER2 to PlayCards(
                    setOf(
                        QUEEN of CLUBS
                    ),
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            playerGameChannel.send(PLAYER3 to Fold)
            assertAllPlayersNext(PlayerFolds::class)
            assertAllPlayersNext(PlayerFolds::class)
            playerGameChannel.send(PLAYER1 to Fold)
            assertAllPlayersNext(PlayerFolds::class)

            assertAllPlayersNext(TrickWon::class)

            playerGameChannel.send(
                PLAYER2 to PlayCards(
                    setOf(
                        KING of CLUBS
                    ),
                )
            )
            assertAllPlayersNext(PlayerPlayedCards::class)
            playerGameChannel.send(PLAYER3 to Fold)
            assertAllPlayersNext(PlayerIsOutOfCards::class)

            assertAllPlayersNext(RoundEnded::class)
            assertAllPlayersNext(GameEnded(TwoTeamTeamId.TEAM2))
        }

        launch {
            withTimeout(2.seconds) {
                val gameResult = GameContext(players, game, gamePlayerChannel, Channel(capacity = Channel.UNLIMITED), playerGameChannel, { this }).playGame()
                assertEquals(TaiPanFinalScore(TwoTeamTeamId.TEAM2, mapOf(TwoTeamTeamId.TEAM1 to 0, TwoTeamTeamId.TEAM2 to 200)), gameResult)
            }
        }

        withTimeout(1.seconds) {
            playerAsserts.await()
        }
        if (!gamePlayerChannel.isEmpty) {
            val messages = buildList {
                while (!gamePlayerChannel.isEmpty) {
                    add(gamePlayerChannel.poll())
                }
            }
            gamePlayerChannel.close()
            fail("Leftover messages for players: $messages")
        }
    }

    class ChannelAsserter<E : Any>(
        internal val channel: ReceiveChannel<E>,
    )

    private fun <E : Any> CoroutineScope.channelAssert(channel: ReceiveChannel<E>, asserter: suspend ChannelAsserter<E>.() -> Unit) =
        async {
            ChannelAsserter(channel).asserter()
        }

    private suspend fun <A : Any, B : Any> ChannelAsserter<Pair<A, B>>.assertNext(first: A) {
        val (key, value) = channel.receive()
        assertTrue(key == first, "Got ($key, $value), expected ($first)")
    }

    private suspend fun <A : Any, B : Any, T : B, C : Any> ChannelAsserter<kotlin.Triple<A, B, C>>.assertNext(first: A, second: KClass<T>) {
        val (a, b, c) = channel.receive()
        assertTrue(a == first && second.isInstance(b), "Got ($a, $b, $c), expected ($first, instance of $second)")
    }

    private suspend fun <B : Any, T : B, C : Any> ChannelAsserter<kotlin.Triple<TwoTeamPlayerId, B, C>>.assertAllPlayersNext(second: KClass<T>) {
        val toFind = TwoTeamPlayerId.values().toMutableSet()
        while (toFind.isNotEmpty()) {
            val (a, b, c) = channel.receive()
            assertTrue(toFind.contains(a) && second.isInstance(b), "Got ($a, $b, $c), expected (element of $toFind, instance of $second)")
            toFind.remove(a)
        }
    }

    private suspend fun <B : Any, C : Any> ChannelAsserter<kotlin.Triple<TwoTeamPlayerId, B, C>>.assertAllPlayersNext(second: B) {
        val toFind = TwoTeamPlayerId.values().toMutableSet()
        while (toFind.isNotEmpty()) {
            val (a, b, c) = channel.receive()
            assertTrue(toFind.contains(a) && second == b, "Got ($a, $b, $c), expected (element of $toFind, $second)")
            toFind.remove(a)
        }
    }
}
