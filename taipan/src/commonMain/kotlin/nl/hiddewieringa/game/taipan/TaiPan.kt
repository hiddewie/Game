package nl.hiddewieringa.game.taipan

import nl.hiddewieringa.game.core.*
import nl.hiddewieringa.game.taipan.card.*
import kotlin.random.Random

sealed class TaiPanState : GameState<TaiPanState>

fun nextPlayer(playerId: TwoTeamPlayerId): TwoTeamPlayerId =
    when (playerId) {
        TwoTeamPlayerId.PLAYER1 -> TwoTeamPlayerId.PLAYER2
        TwoTeamPlayerId.PLAYER2 -> TwoTeamPlayerId.PLAYER3
        TwoTeamPlayerId.PLAYER3 -> TwoTeamPlayerId.PLAYER4
        TwoTeamPlayerId.PLAYER4 -> TwoTeamPlayerId.PLAYER1
    }

private fun dragonPassTargetPlayer(playerId: TwoTeamPlayerId, pass: DragonPass) =
    when (pass) {
        DragonPass.LEFT -> nextPlayer(playerId)
        DragonPass.RIGHT -> nextPlayer(nextPlayer(nextPlayer(playerId)))
    }

private fun canCardsBePlayed(previous: CardCombination, current: CardCombination): Boolean {
    val playedBomb = current is Bomb
    return when (previous) {
        is QuadrupleBomb ->
            (current is QuadrupleBomb && previous.value < current.value) || current is StraightBomb
        is StraightBomb ->
            current is StraightBomb && previous.length < current.length
        is HighCard ->
            current is Bomb || (current is HighCard && previous.value < current.value)
        is Tuple ->
            playedBomb || (current is Tuple && previous.value < current.value)
        is Triple ->
            playedBomb || (current is Triple && previous.value < current.value)
        is TupleSequence ->
            playedBomb || (current is TupleSequence && previous.length == current.length && previous.minValue < current.minValue)
        is FullHouse ->
            playedBomb || (current is FullHouse && previous.triple.value < current.triple.value)
        is Straight ->
            playedBomb || (current is Straight && previous.length == current.length && previous.minValue < current.minValue)
    }
}

private fun cardsContainWish(wish: Int, cards: Collection<Card>): Boolean =
    cards.filterIsInstance<NumberedCard>()
        .any { it.value == wish }

internal fun hasStraightOfLengthAndContainsValue(value: Int, minLength: Int, cards: Collection<Card>): Boolean {
    val hasValue = cards.filterIsInstance<NumberedCard>()
        .groupBy { it.value }
        .mapValues { true }
        .withDefault { false }

    if (!hasValue.getValue(value)) {
        return false
    }

    var maxValue = value
    var minValue = value
    while (hasValue.getValue(maxValue)) {
        maxValue++
    }
    maxValue--
    while (hasValue.getValue(minValue)) {
        minValue--
    }
    minValue++
    if (maxValue - minValue + 1 >= minLength) {
        return true
    }

    val hasPhoenix = cards.any { it is Phoenix }
    if (!hasPhoenix) {
        return false
    }

    var maxValueWithPhoenixUp = value
    var minValueWithPhoenixDown = value
    var skipUp = 1
    while (skipUp > 0 || hasValue.getValue(maxValueWithPhoenixUp)) {
        if (!hasValue.getValue(maxValueWithPhoenixUp)) {
            skipUp--
        }
        maxValueWithPhoenixUp++
    }
    maxValueWithPhoenixUp--

    var skipDown = 1
    while (skipDown > 0 || hasValue.getValue(minValueWithPhoenixDown)) {
        if (!hasValue.getValue(minValueWithPhoenixDown)) {
            skipDown--
        }
        minValueWithPhoenixDown--
    }
    minValueWithPhoenixDown++

    return (maxValueWithPhoenixUp - minValue + 1 >= minLength) ||
            (maxValue - minValueWithPhoenixDown + 1 >= minLength)
}

internal fun cardsContainWish(wish: Int, previousCards: CardCombination, cards: Collection<Card>): Boolean {
    if (!cardsContainWish(wish, cards)) {
        return false
    }

    val cardValueCount = cards.filterIsInstance<NumberedCard>()
        .groupBy { it.value }
        .mapValues { (_, cards) -> cards.size }
        .withDefault { 0 }

    val hasWishQuadrupleBomb = cardValueCount.getValue(wish) == 4
    val hasWishStraightBomb = cards.filterIsInstance<NumberedCard>()
        .filter { it.value == wish }
        .map { it.suit }
        .any { suit ->
            val suitCards = cards
                .filterIsInstance<NumberedCard>()
                .filter { it.suit == suit }
            hasStraightOfLengthAndContainsValue(wish, 5, suitCards)
        }
    val hasWishBomb = hasWishQuadrupleBomb || hasWishStraightBomb

    val hasPhoenix = cards.any { it is Phoenix }

    return when (previousCards) {
        is QuadrupleBomb ->
            hasWishStraightBomb ||
                    (previousCards.value < wish && cardValueCount.getValue(wish) == 4)
        is StraightBomb ->
            cards.filterIsInstance<NumberedCard>()
                .filter { it.value == wish }
                .map { it.suit }
                .any { suit ->
                    val suitCards = cards
                        .filterIsInstance<NumberedCard>()
                        .filter { it.suit == suit }
                    hasStraightOfLengthAndContainsValue(wish, previousCards.length + 1, suitCards)
                }
        is HighCard ->
            hasWishBomb || previousCards.value < wish.toFloat()
        is Tuple ->
            hasWishBomb || (previousCards.value < wish && (cardValueCount.getValue(wish) >= 2 || cardValueCount.getValue(wish) == 1 && hasPhoenix))
        is Triple ->
            hasWishBomb || (previousCards.value < wish && (cardValueCount.getValue(wish) >= 3 || cardValueCount.getValue(wish) == 2 && hasPhoenix))
        is FullHouse ->
            hasWishBomb ||
                    if (hasPhoenix) {
                        (previousCards.value < wish && cardValueCount.getValue(wish) >= 3 && cardValueCount.filter { (number, count) -> number != wish && count >= 1 }.isNotEmpty()) ||
                                (previousCards.value < wish && cardValueCount.getValue(wish) >= 2 && cardValueCount.filter { (number, count) -> number != wish && count >= 2 }.isNotEmpty()) ||
                                (cardValueCount.getValue(wish) >= 2 && cardValueCount.filter { (number, count) -> number > wish && count >= 2 }.isNotEmpty())
                    } else {
                        (previousCards.value < wish && cardValueCount.getValue(wish) >= 3 && cardValueCount.filter { (number, count) -> number != wish && count >= 2 }.isNotEmpty()) ||
                                (cardValueCount.getValue(wish) >= 2 && cardValueCount.filter { (number, count) -> number > wish && count >= 3 }.isNotEmpty())
                    }
        is TupleSequence ->
            hasWishBomb ||
                    if (hasPhoenix) {
                        val hasDouble = cardValueCount.filter { (_, count) -> count >= 2 }.mapValues { true }.withDefault { false }
                        val hasOnlySingle = cardValueCount.filter { (_, count) -> count == 1 }.mapValues { true }.withDefault { false }
                        ((previousCards.minValue + 1)..(NumberedCard.ACE - previousCards.length + 1)).any { value ->
                            val doubleCount = (value until (value + previousCards.length)).count { hasDouble.getValue(it) }
                            val singleCount = (value until (value + previousCards.length)).count { hasOnlySingle.getValue(it) }
                            doubleCount == previousCards.length || (doubleCount == previousCards.length - 1 && singleCount == 1)
                        }
                    } else {
                        val hasDouble = cardValueCount.filter { (_, count) -> count >= 2 }.mapValues { true }.withDefault { false }
                        ((previousCards.minValue + 1)..(NumberedCard.ACE - previousCards.length + 1)).any { value ->
                            (value until (value + previousCards.length)).all { hasDouble.getValue(it) }
                        }
                    }
        is Straight ->
            hasWishBomb ||
                    hasStraightOfLengthAndContainsValue(wish, previousCards.length, cards.filterIsInstance<NumberedCard>().filter { previousCards.minValue < it.value } + cards.filterIsInstance<Phoenix>())
    }
}

// TODO rename tai pan receive cards
data class TaiPan(
    private val parameters: TaiPanGameParameters,
    val points: Map<TwoTeamTeamId, Int>,
    val playerCardsToGive: Map<TwoTeamPlayerId, List<Card>>,
    val playerCards: Map<TwoTeamPlayerId, Set<Card>>,
    val taiPannedPlayers: Map<TwoTeamPlayerId, TaiPanStatus>,
    val roundIndex: Int,
) : TaiPanState(), IntermediateGameState<TwoTeamPlayerId, TaiPanPlayerActions, TaiPanEvent, TaiPanState> {

    constructor(
        parameters: TaiPanGameParameters,
        points: Map<TwoTeamTeamId, Int>,
        roundIndex: Int,
    ) : this(
        parameters,
        points,
        // This might not generate enough permutations to make all possible 52! shuffles possible!
        //   We would need 226 bits of entropy to be able to generate all combinations
        //   See https://www.wikiwand.com/en/Fisher%E2%80%93Yates_shuffle#/Pseudorandom_generators
        fullDeck.shuffled(Random(parameters.seed + roundIndex))
            .let { shuffledCards ->
                TwoTeamPlayerId.values()
                    .mapIndexed { index, playerId -> playerId to shuffledCards.subList(index * 14, (index + 1) * 14) }
                    .toMap()
                    .mapValues { it.value.toMutableList() }
                    .onEach { it.value.sortWith(naturalOrder()) }
            },
        TwoTeamPlayerId.values().associate { it to setOf<Card>() },
        mapOf<TwoTeamPlayerId, TaiPanStatus>(),
        roundIndex
    )

    constructor(parameters: TaiPanGameParameters) : this(
        parameters,
        mapOf(
            TwoTeamTeamId.TEAM1 to 0,
            TwoTeamTeamId.TEAM2 to 0,
        ),
        1
    )

    override fun applyEvent(event: TaiPanEvent): TaiPanState =
        when (event) {
            is CardsHaveBeenDealt ->
                copy(
                    playerCards = playerCards + mapOf(event.player to ((playerCards.getValue(event.player) + event.cards))),
                )

            is PlayerTaiPanned ->
                copy(taiPannedPlayers = taiPannedPlayers + mapOf(event.player to event.status))

            is GameEnded ->
                TaiPanFinalScore(
                    event.winningTeam,
                    points,
                )

            is AllPlayersHaveReceivedCards ->
                TaiPanPassCards(
                    parameters,
                    points,
                    playerCards,
                    taiPannedPlayers,
                    roundIndex
                )

            else ->
                this
        }

    override fun processPlayerAction(playerId: TwoTeamPlayerId, action: TaiPanPlayerActions): TaiPanEvent =
        when (action) {

            is RequestNextCards ->
                when {
                    playerCards.getValue(playerId).size > 8 ->
                        IllegalAction("Already received next cards", playerId, action)

                    else ->
                        CardsHaveBeenDealt(playerId, playerCardsToGive.getValue(playerId).subList(8, 14).toSet())
                }

            is CallTaiPan ->
                when {
                    taiPannedPlayers.containsKey(playerId) ->
                        IllegalAction("Already tai panned", playerId, action)
                    playerCards.getValue(playerId).size < 14 ->
                        PlayerTaiPanned(playerId, TaiPanStatus.GREAT)
                    else ->
                        PlayerTaiPanned(playerId, TaiPanStatus.NORMAL)
                }

            else ->
                IllegalAction("Illegal move", playerId, action)
        }

    override val gameDecisions: List<GameDecision<TaiPanEvent>> =
        listOf(
            GameDecision(points.any { (_, p) -> p >= parameters.points } && points.getValue(TwoTeamTeamId.TEAM1) != points.getValue(TwoTeamTeamId.TEAM2)) {
                val winningTeam = if (points.getValue(TwoTeamTeamId.TEAM1) > points.getValue(TwoTeamTeamId.TEAM2)) {
                    TwoTeamTeamId.TEAM1
                } else {
                    TwoTeamTeamId.TEAM2
                }
                GameEnded(winningTeam)
            },
            playerCards.keys
                .filter { player -> playerCards.getValue(player).isEmpty() }
                .let { playersWithNoCards ->
                    GameDecision(playersWithNoCards.isNotEmpty()) {
                        val player = playersWithNoCards.first()
                        CardsHaveBeenDealt(player, playerCardsToGive.getValue(player).subList(0, 8).toSet())
                    }
                },
            GameDecision(playerCards.all { (_, cards) -> cards.size == 14 }) {
                AllPlayersHaveReceivedCards
            },
            // TODO add game decision if great tai pan and not all cards, receive next cards.
        )
}

// TODO rename exchange
data class TaiPanPassCards(
    private val parameters: TaiPanGameParameters,
    val points: Map<TwoTeamTeamId, Int>,
    val playerCards: Map<TwoTeamPlayerId, Set<Card>>,
    val taiPannedPlayers: Map<TwoTeamPlayerId, TaiPanStatus>,
    val passedCards: Map<TwoTeamPlayerId, ThreeWayPass>,
    val roundIndex: Int,
) : TaiPanState(), IntermediateGameState<TwoTeamPlayerId, TaiPanPlayerActions, TaiPanEvent, TaiPanState> {

    constructor(
        parameters: TaiPanGameParameters,
        points: Map<TwoTeamTeamId, Int>,
        playerCards: Map<TwoTeamPlayerId, Set<Card>>,
        taiPannedPlayers: Map<TwoTeamPlayerId, TaiPanStatus>,
        roundIndex: Int,
    ) : this(
        parameters,
        points,
        playerCards,
        taiPannedPlayers,
        emptyMap<TwoTeamPlayerId, ThreeWayPass>(),
        roundIndex,
    )

    override fun applyEvent(event: TaiPanEvent): TaiPanState =
        when (event) {
            is CardsHaveBeenExchanged ->
                copy(passedCards = passedCards + mapOf(event.player to event.pass))

            is PlayerTaiPanned ->
                copy(taiPannedPlayers = taiPannedPlayers + mapOf(event.player to event.status))

            AllPlayersHaveExchangedCards ->
                TaiPanPlayTrick(
                    parameters,
                    points,
                    playerCards.distributePassedCards(passedCards),
                    taiPannedPlayers,
                    roundIndex,
                )

            else ->
                this
        }

    override fun processPlayerAction(playerId: TwoTeamPlayerId, action: TaiPanPlayerActions): TaiPanEvent =
        when (action) {
            is CardPass ->
                when {
                    passedCards.containsKey(playerId) ->
                        IllegalAction("Already passed", playerId, action)

                    playerCards.getValue(playerId).containsAll(setOf(action.cardPass.left, action.cardPass.forward, action.cardPass.right)) ->
                        CardsHaveBeenExchanged(playerId, action.cardPass)

                    else ->
                        IllegalAction("Player does not have cards", playerId, action)
                }

            is CallTaiPan ->
                when {
                    taiPannedPlayers.containsKey(playerId) ->
                        IllegalAction("Already tai panned", playerId, action)

                    else ->
                        PlayerTaiPanned(playerId, TaiPanStatus.NORMAL)
                }

            else ->
                IllegalAction("Illegal move", playerId, action)
        }

    override val gameDecisions: List<GameDecision<TaiPanEvent>> =
        listOf(
            GameDecision(passedCards.size == 4) {
                AllPlayersHaveExchangedCards
            }
        )

    private fun Map<TwoTeamPlayerId, Set<Card>>.distributePassedCards(passedCards: Map<TwoTeamPlayerId, ThreeWayPass>): Map<TwoTeamPlayerId, Set<Card>> {
        val newPlayerCards: MutableMap<TwoTeamPlayerId, Set<Card>> = this.toMutableMap()

        passedCards.forEach { (playerId, cards) ->
            newPlayerCards.computeIfPresent(playerId) { value -> value - setOf(cards.left, cards.forward, cards.right) }
            newPlayerCards.computeIfPresent(nextPlayer(playerId)) { value -> value + setOf(cards.left) }
            newPlayerCards.computeIfPresent(nextPlayer(nextPlayer(playerId))) { value -> value + setOf(cards.forward) }
            newPlayerCards.computeIfPresent(nextPlayer(nextPlayer(nextPlayer(playerId)))) { value -> value + setOf(cards.right) }
        }

        return newPlayerCards
    }
}

data class TaiPanPlayTrick(
    private val parameters: TaiPanGameParameters,
    val points: Map<TwoTeamTeamId, Int>,
    val playerCards: Map<TwoTeamPlayerId, Set<Card>>,
    val taiPannedPlayers: Map<TwoTeamPlayerId, TaiPanStatus>,
    val roundIndex: Int,
    val trickIndex: Int,
    val currentPlayer: TwoTeamPlayerId,
    val roundCards: Map<TwoTeamPlayerId, CardSet>,
    val trickCards: CardSet,
    val lastPlayedCards: kotlin.Triple<TwoTeamPlayerId, CardCombination, MahjongRequest?>?,
    val folds: Int,
    val outOfCardOrder: List<TwoTeamPlayerId>,
    val mahjongWish: MahjongWish,
) : TaiPanState(), IntermediateGameState<TwoTeamPlayerId, TaiPanPlayerActions, TaiPanEvent, TaiPanState> {

    constructor(
        parameters: TaiPanGameParameters,
        points: Map<TwoTeamTeamId, Int>,
        playerCards: Map<TwoTeamPlayerId, Set<Card>>,
        taiPannedPlayers: Map<TwoTeamPlayerId, TaiPanStatus>,
        roundIndex: Int,
    ) : this(
        parameters,
        points,
        playerCards,
        taiPannedPlayers,
        roundIndex,
        1,
        TwoTeamPlayerId.values()
            .find { playerId -> playerCards.getValue(playerId).any { it is Mahjong } }
            ?: throw IllegalStateException("No player has the Mahjong"),
        TwoTeamPlayerId.values().associate { it to mutableSetOf<Card>() },
        emptySet(),
        null,
        0,
        emptyList(),
        MahjongWish(),
    )

    override fun applyEvent(event: TaiPanEvent): TaiPanState =
        when (event) {

            is PlayerPlayedCards ->
                copy(
                    playerCards = playerCards + mapOf(event.player to (playerCards.getValue(event.player) - event.cards.cards)),
                    lastPlayedCards = Triple(event.player, event.cards, event.mahjongRequest),
                    folds = 0,
                    currentPlayer = nextPlayer(event.player),
                    trickCards = trickCards + event.cards.cards
                )

            is PlayerFolds ->
                copy(
                    currentPlayer = nextPlayer(currentPlayer),
                    folds = folds + 1,
                )

            is PlayerTaiPanned ->
                copy(taiPannedPlayers = taiPannedPlayers + mapOf(event.player to event.status))

            is MahjongWishRequested ->
                copy(mahjongWish = mahjongWish.wish(MahjongRequest(event.value)))

            is MahjongWishFulfilled ->
                copy(mahjongWish = mahjongWish.fulfilled())

            is PlayerIsOutOfCards ->
                copy(outOfCardOrder = outOfCardOrder + listOf(event.player))

            is DragonTrickWon ->
                TaiPanDragonPass(this)

            is TrickWon ->
                copy(
                    roundCards = roundCards + mapOf(event.playerToReceiveTrickCards to roundCards.getValue(event.playerToReceiveTrickCards) + trickCards),
                    trickCards = emptySet(),
                    trickIndex = trickIndex + 1,
                    currentPlayer = event.nextPlayer,
                    lastPlayedCards = null,
                    folds = 0,
                )

            is RoundEnded ->
                TaiPan(
                    parameters,
                    mapOf(
                        TwoTeamTeamId.TEAM1 to points.getValue(TwoTeamTeamId.TEAM1) + event.roundScore.getValue(TwoTeamTeamId.TEAM1),
                        TwoTeamTeamId.TEAM2 to points.getValue(TwoTeamTeamId.TEAM2) + event.roundScore.getValue(TwoTeamTeamId.TEAM2),
                    ),
                    event.roundIndex + 1,
                )

            else ->
                this
        }

    override fun processPlayerAction(playerId: TwoTeamPlayerId, action: TaiPanPlayerActions): TaiPanEvent =
        when {

            playerId == currentPlayer && action is PlayCards -> {
                val playedCards = findCardCombination(lastPlayedCards?.second, action.cards, action.addons)
                println("Played combination $playedCards")

                when {
                    !playerCards.getValue(playerId).containsAll(action.cards) ->
                        IllegalAction("Player does not have played cards", playerId, action)

                    playedCards == null ->
                        IllegalAction("Not a valid combination", playerId, action)

                    lastPlayedCards != null && !canCardsBePlayed(lastPlayedCards.second, playedCards) ->
                        IllegalAction("This combination is incompatible with previously played cards", playerId, action)

                    mahjongWish.present &&
                            !action.cards.filterIsInstance<NumberedCard>().any { it.value == mahjongWish.value } &&
                            cardsContainWish(mahjongWish.value, playerCards.getValue(currentPlayer)) ->
                        IllegalAction("Must play the Mahjong wish", playerId, action)

                    else ->
                        PlayerPlayedCards(currentPlayer, playedCards, action.addons.filterIsInstance<MahjongRequest>().firstOrNull())
                }
            }

            playerId == currentPlayer && action is Fold ->
                when {
                    lastPlayedCards != null && mahjongWish.present && cardsContainWish(mahjongWish.value, lastPlayedCards.second, playerCards.getValue(currentPlayer)) ->
                        IllegalAction("Must play the Mahjong wish", playerId, action)

                    lastPlayedCards == null && mahjongWish.present && cardsContainWish(mahjongWish.value, playerCards.getValue(currentPlayer)) ->
                        IllegalAction("Must play the Mahjong wish", playerId, action)

                    lastPlayedCards == null ->
                        IllegalAction("Must play cards because player begins trick", playerId, action)

                    else ->
                        PlayerFolds(currentPlayer)
                }

            playerId != currentPlayer && action is PlayCards -> {
                val playedCombination = findCardCombination(lastPlayedCards?.second, action.cards, action.addons)
                when {
                    playedCombination == null ->
                        IllegalAction("Not a valid combination", playerId, action)

                    !playerCards.getValue(playerId).containsAll(action.cards) ->
                        IllegalAction("Player does not have played cards", playerId, action)

                    playedCombination is Bomb ->
                        PlayerPlayedCards(playerId, playedCombination)

                    else ->
                        IllegalAction("Illegal card combination in turn of other player", playerId, action)
                }
            }

            action is CallTaiPan ->
                when {
                    taiPannedPlayers.containsKey(playerId) ->
                        IllegalAction("Already tai panned", playerId, action)

                    playerCards.getValue(playerId).size < 14 ->
                        IllegalAction("Already played a card", playerId, action)

                    else ->
                        PlayerTaiPanned(playerId, TaiPanStatus.NORMAL)
                }

            else ->
                IllegalAction("Illegal move", playerId, action)
        }

    override val gameDecisions: List<GameDecision<TaiPanEvent>> =
        listOf(
            (playerCards.keys - outOfCardOrder)
                .filter { player -> playerCards.getValue(player).isEmpty() }
                .let { newOutOfCardPlayers ->
                    GameDecision(newOutOfCardPlayers.isNotEmpty()) {
                        PlayerIsOutOfCards(newOutOfCardPlayers.first())
                    }
                },
            GameDecision(mahjongWish.present && lastPlayedCards?.second?.cards?.filterIsInstance<NumberedCard>()?.any { it.value == mahjongWish.value } ?: false) {
                MahjongWishFulfilled
            },
            GameDecision(lastPlayedCards != null && !mahjongWish.present && lastPlayedCards.second.cards.any { it is Mahjong } && lastPlayedCards.third != null) {
                MahjongWishRequested(lastPlayedCards!!.third!!.value)
            },
            GameDecision(lastPlayedCards != null && lastPlayedCards.second is HighCard && (lastPlayedCards.second as HighCard).card == Dog) {
                TrickWon(lastPlayedCards!!.first, nextPlayer(nextPlayer(lastPlayedCards.first)))
            },
            GameDecision(playerCards.getValue(currentPlayer).isEmpty()) {
                println("Player $currentPlayer has no cards and folds")
                PlayerFolds(currentPlayer)
            },
            GameDecision(lastPlayedCards != null && folds >= 3) {
                if (lastPlayedCards!!.second is HighCard && (lastPlayedCards.second as HighCard).card == Dragon) {
                    DragonTrickWon(lastPlayedCards.first)
                } else {
                    println("Player ${lastPlayedCards.first} won the trick and received trick cards $trickCards")

                    var nextPlayerToPlay = lastPlayedCards.first
                    while (playerCards.getValue(nextPlayerToPlay).isEmpty()) {
                        println("Player $nextPlayerToPlay is out of cards. Next player: ${nextPlayer(nextPlayerToPlay)}")
                        nextPlayerToPlay = nextPlayer(nextPlayerToPlay)
                    }
                    TrickWon(currentPlayer, nextPlayerToPlay)
                }
            },
            GameDecision(outOfCardOrder.size == 3 || (outOfCardOrder.size >= 2 && outOfCardOrder[0].team == outOfCardOrder[1].team)) {
                // This round is done

                val scores = mutableMapOf(
                    TwoTeamTeamId.TEAM1 to 0,
                    TwoTeamTeamId.TEAM2 to 0,
                )

                if (outOfCardOrder[0].team == outOfCardOrder[1].team) { // double game
                    scores.computeIfPresent(outOfCardOrder[0].team) { s -> s + 200 }
                } else {
                    scores.computeIfPresent(outOfCardOrder[0].team) { s -> s + roundCards.getValue(outOfCardOrder[0]).sumBy { it.points } }
                    scores.computeIfPresent(outOfCardOrder[1].team) { s -> s + roundCards.getValue(outOfCardOrder[1]).sumBy { it.points } }
                    scores.computeIfPresent(outOfCardOrder[2].team) { s -> s + roundCards.getValue(outOfCardOrder[2]).sumBy { it.points } }

                    val lastPlayer = TwoTeamPlayerId.values().filterNot { outOfCardOrder.contains(it) }[0]
                    // Tricks of last player go to first player.
                    scores.computeIfPresent(outOfCardOrder[0].team) { s -> s + roundCards.getValue(lastPlayer).sumBy { it.points } }
                    // Hand of last player goes to opposite team
                    scores.computeIfPresent(outOfCardOrder[0].team) { s -> s + playerCards.getValue(lastPlayer).sumBy { it.points } }
                }
                taiPannedPlayers.forEach { (playerId, taiPan) ->
                    scores.computeIfPresent(playerId.team) { s ->
                        val success = outOfCardOrder[0] == playerId
                        val multiplier = if (success) 1 else -1
                        println("Player $playerId had scored taiPan score ${multiplier * taiPan.score}")
                        s + multiplier * taiPan.score
                    }
                }

                println("Round ended, score: $scores")

                RoundEnded(roundIndex, scores)
            },
        )
}

// TODO implement automatic dragon pass in case of no difference
data class TaiPanDragonPass(
    val trick: TaiPanPlayTrick,
) : TaiPanState(), IntermediateGameState<TwoTeamPlayerId, TaiPanPlayerActions, TaiPanEvent, TaiPanState> {

    override fun applyEvent(event: TaiPanEvent): TaiPanState =
        when (event) {
            is PlayerPassedDragon ->
                trick.copy(
                    roundCards = trick.roundCards + mapOf(dragonPassTargetPlayer(event.player, event.dragonPass) to trick.trickCards),
                    trickCards = emptySet(),
                    currentPlayer = event.player,
                    trickIndex = trick.trickIndex + 1,
                    lastPlayedCards = null,
                    folds = 0,
                )

            else ->
                this
        }

    override fun processPlayerAction(playerId: TwoTeamPlayerId, action: TaiPanPlayerActions): TaiPanEvent =
        when {
            playerId == trick.currentPlayer && action is PassDragonTrick ->
                PlayerPassedDragon(playerId, action.dragonPass)

            else ->
                IllegalAction("Not allowed", playerId, action)
        }

    override val gameDecisions: List<GameDecision<TaiPanEvent>> =
        emptyList()
}

data class TaiPanFinalScore(
    val teamWon: TwoTeamTeamId,
    val points: Map<TwoTeamTeamId, Int>,
) : TaiPanState()

data class TaiPanGameParameters(val points: Int, val seed: Long) : GameParameters

data class MahjongWish(
    val wish: Int? = null,
) {
    val present: Boolean
        get() = wish != null

    // Check if present before getting value
    val value: Int
        get() = wish!!

    fun wish(request: MahjongRequest): MahjongWish =
        copy(wish = request.value)

    fun fulfilled(): MahjongWish =
        copy(wish = null)
}

fun <K, V> MutableMap<K, V>.computeIfPresent(key: K, computer: (oldValue: V) -> V) {
    if (containsKey(key)) {
        this[key] = computer(this.getValue(key))
    }
}
