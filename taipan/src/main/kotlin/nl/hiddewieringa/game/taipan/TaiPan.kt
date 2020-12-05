package nl.hiddewieringa.game.taipan

import nl.hiddewieringa.game.core.*
import nl.hiddewieringa.game.taipan.card.*
import nl.hiddewieringa.game.taipan.player.TaiPanPlayer
import kotlin.random.Random

typealias TaiPanGameContext = GameContext<TaiPanPlayerActions, TaiPanEvent, TwoTeamPlayerId, TwoTeams<TaiPanPlayer>>

class TaiPan(
    private val parameters: TaiPanGameParameters
) : Game<
        TaiPanGameParameters,
        TaiPanPlayer,
        TaiPanPlayerActions,
        TaiPanEvent,
        TaiPanGameResult,
        TwoTeamPlayerId,
        TwoTeams<TaiPanPlayer>
        > {

    override suspend fun play(context: TaiPanGameContext): TaiPanGameResult {

        val points = mutableMapOf(
            TwoTeamTeamId.TEAM1 to 0,
            TwoTeamTeamId.TEAM2 to 0,
        )

        // Rounds
        var roundIndex = 1
        while (points.all { (_, p) -> p < parameters.points } || points.getValue(TwoTeamTeamId.TEAM1) == points.getValue(TwoTeamTeamId.TEAM2)) {
            val scores = context.playRound(roundIndex)
            points.computeIfPresent(TwoTeamTeamId.TEAM1) { _, s -> s + scores.getValue(TwoTeamTeamId.TEAM1) }
            points.computeIfPresent(TwoTeamTeamId.TEAM2) { _, s -> s + scores.getValue(TwoTeamTeamId.TEAM2) }
            print("Current team points: $points")
            context.sendToAllPlayers(ScoreUpdated(points))
            roundIndex++
        }

        return if (points.getValue(TwoTeamTeamId.TEAM1) > points.getValue(TwoTeamTeamId.TEAM2)) Team1Won else Team2Won
    }

    private suspend fun TaiPanGameContext.playRound(roundIndex: Int): Map<TwoTeamTeamId, Int> {
        sendToAllPlayers(RoundBegan(roundIndex))

        val shuffled = Random(parameters.seed).shuffle(fullSuit)
        val playerCards: MutableMap<TwoTeamPlayerId, MutableList<Card>> = TwoTeamPlayerId.values()
            .mapIndexed { index, playerId -> playerId to shuffled.subList(index * 8, (index + 1) * 8) }
            .toMap()
            .mapValues { it.value.toMutableList() }
            .onEach { it.value.sortWith(Comparator.naturalOrder()) }
            .toMutableMap()
        val taiPannedPlayers = mutableMapOf<TwoTeamPlayerId, TaiPanStatus>()

        TwoTeamPlayerId.values()
            .forEach { playerId ->
                println("Player $playerId gets cards ${playerCards.getValue(playerId)}")
                sendToPlayer(playerId, CardsHaveBeenDealt(playerId, playerCards.getValue(playerId).toSet()))
            }

        var requestedPlayers = 0
        while (requestedPlayers < 4) {
            val (playerId, action) = receiveFromPlayer()
            when (action) {
                is RequestNextCards ->
                    if (playerCards.getValue(playerId).size > 8) {
                        sendToPlayer(playerId, IllegalAction("Already tai panned", action))
                    } else {
                        val newCards = shuffled.subList(4 * 8 + requestedPlayers * 6, 4 * 8 + (requestedPlayers + 1) * 6)
                        playerCards.getValue(playerId).addAll(newCards)
                        playerCards.getValue(playerId).sortWith(Comparator.naturalOrder())
                        println("Player $playerId requested next cards and gets cards $newCards, all cards are ${playerCards.getValue(playerId).toSet()}")
                        sendToPlayer(playerId, CardsHaveBeenDealt(playerId, playerCards.getValue(playerId).toSet()))
                        requestedPlayers++
                    }
                is CallTaiPan -> when {
                    taiPannedPlayers.containsKey(playerId) ->
                        sendToPlayer(playerId, IllegalAction("Already tai panned", action))
                    playerCards.size < 14 -> {
                        taiPannedPlayers[playerId] = TaiPanStatus.GREAT
                        sendToAllPlayers(PlayerTaiPanned(playerId, TaiPanStatus.GREAT))
                    }
                    else -> {
                        taiPannedPlayers[playerId] = TaiPanStatus.NORMAL
                        sendToAllPlayers(PlayerTaiPanned(playerId, TaiPanStatus.NORMAL))
                    }
                }
                else ->
                    sendToPlayer(playerId, IllegalAction("Illegal move", action))
            }
        }

        TwoTeamPlayerId.values()
            .map { playerId -> sendToPlayer(playerId, RequestPassCards) }

        val passedCards = mutableMapOf<TwoTeamPlayerId, CardPass>()
        while (passedCards.size < 4) {
            val (playerId, pass) = receiveFromPlayer()
            println("Player $playerId passes $pass")
            when (pass) {
                is CardPass -> when {
                    passedCards.containsKey(playerId) ->
                        sendToPlayer(playerId, IllegalAction("Already passed", pass))

                    playerCards.getValue(playerId).containsAll(setOf(pass.left, pass.forward, pass.right)) ->
                        passedCards[playerId] = pass

                    else ->
                        sendToPlayer(playerId, IllegalAction("Player does not have cards", pass))
                }
                is CallTaiPan -> when {
                    taiPannedPlayers.containsKey(playerId) ->
                        sendToPlayer(playerId, IllegalAction("Already tai panned", pass))
                    else -> {
                        taiPannedPlayers[playerId] = TaiPanStatus.NORMAL
                        sendToAllPlayers(PlayerTaiPanned(playerId, TaiPanStatus.NORMAL))
                    }
                }
                else ->
                    sendToPlayer(playerId, IllegalAction("Illegal move", pass))
            }
        }

        passedCards.forEach { (playerId, cards) ->
            playerCards.getValue(playerId).removeIf { it == cards.left || it == cards.forward || it == cards.right }
            playerCards.getValue(nextPlayer(playerId)).add(cards.left)
            playerCards.getValue(nextPlayer(nextPlayer(playerId))).add(cards.forward)
            playerCards.getValue(nextPlayer(nextPlayer(nextPlayer(playerId)))).add(cards.right)
        }

        TwoTeamPlayerId.values()
            .forEach { playerId -> playerCards.getValue(playerId).sortWith(Comparator.naturalOrder()) }

        TwoTeamPlayerId.values()
            .forEach { playerId ->
                println("Player $playerId has cards ${playerCards.getValue(playerId)}")
                sendToPlayer(playerId, CardsHaveBeenPassed(playerId, playerCards.getValue(playerId).toSet()))
            }

        var trickPlayer: TwoTeamPlayerId = TwoTeamPlayerId.values()
            .find { playerId -> playerCards.getValue(playerId).any { it is Mahjong } }
            ?: throw IllegalStateException("No player has the Mahjong")

        val roundCards = TwoTeamPlayerId.values()
            .map { it to mutableSetOf<Card>() }
            .toMap()

        val outOfCardOrder = mutableListOf<TwoTeamPlayerId>()
        val mahjongWish = MahjongWish()

        // Single round, tricks
        while (true) {
            println("Player $trickPlayer begins a trick")
            val (trickPlayerWon, trickOutOfCardOrder, trickCards) = playTrick(trickPlayer, playerCards, taiPannedPlayers, mahjongWish)
            val (trickCardsPlayer, trickCardsCards) = trickCards
            trickPlayer = trickPlayerWon
            outOfCardOrder.addAll(trickOutOfCardOrder)
            roundCards.getValue(trickCardsPlayer).addAll(trickCardsCards)

            println("Player $trickPlayer won the trick, players $trickOutOfCardOrder are out of cards and player $trickCardsPlayer received trick cards $trickCardsCards")

            // This round is done
            if (outOfCardOrder.size == 3 || (outOfCardOrder.size >= 2 && players.team(outOfCardOrder[0]) == players.team(outOfCardOrder[1]))) {
                break
            }

            while (playerCards.getValue(trickPlayer).isEmpty()) {
                println("Player $trickPlayer is out of cards. Next player: ${nextPlayer(trickPlayer)}")
                trickPlayer = nextPlayer(trickPlayer)
            }
        }

        val scores = mutableMapOf(
            TwoTeamTeamId.TEAM1 to 0,
            TwoTeamTeamId.TEAM2 to 0,
        )

        if (players.team(outOfCardOrder[0]) == players.team(outOfCardOrder[1])) { // double game
            scores.computeIfPresent(players.team(outOfCardOrder[0])) { _, s -> s + 200 }
        } else {
            scores.computeIfPresent(players.team(outOfCardOrder[0])) { _, s -> s + roundCards.getValue(outOfCardOrder[0]).sumBy { it.points } }
            scores.computeIfPresent(players.team(outOfCardOrder[1])) { _, s -> s + roundCards.getValue(outOfCardOrder[1]).sumBy { it.points } }
            scores.computeIfPresent(players.team(outOfCardOrder[2])) { _, s -> s + roundCards.getValue(outOfCardOrder[2]).sumBy { it.points } }

            val lastPlayer = TwoTeamPlayerId.values().filterNot { outOfCardOrder.contains(it) }[0]
            // Tricks of last player go to first player.
            scores.computeIfPresent(players.team(outOfCardOrder[0])) { _, s -> s + roundCards.getValue(lastPlayer).sumBy { it.points } }
            // Hand of last player goes to opposite team
            scores.computeIfPresent(players.team(outOfCardOrder[0])) { _, s -> s + playerCards.getValue(lastPlayer).sumBy { it.points } }
        }
        taiPannedPlayers.forEach { (playerId, taiPan) ->
            scores.computeIfPresent(players.team(playerId)) { _, s ->
                val success = outOfCardOrder[0] == playerId
                val multiplier = if (success) 1 else -1
                println("Player $playerId had scored taiPan score ${multiplier * taiPan.score}")
                s + multiplier * taiPan.score
            }
        }

        println("Round ended, score: $scores")
        sendToAllPlayers(RoundEnded(roundIndex, scores))

        return scores
    }

    private suspend fun TaiPanGameContext.playTrick(
        trickPlayer: TwoTeamPlayerId,
        playerCards: Map<TwoTeamPlayerId, MutableList<Card>>,
        taiPannedPlayers: MutableMap<TwoTeamPlayerId, TaiPanStatus>,
        mahjongWish: MahjongWish
    ): kotlin.Triple<TwoTeamPlayerId, List<TwoTeamPlayerId>, Pair<TwoTeamPlayerId, Set<Card>>> {
        // TODO make immutable
        val outOfCardsPlayers = mutableListOf<TwoTeamPlayerId>()

        sendToAllPlayers(TrickBegan(trickPlayer))
        var currentPlayer = trickPlayer
        var folds = 0

        var lastPlayedCards = playInitialCards(currentPlayer, playerCards, mahjongWish, outOfCardsPlayers, taiPannedPlayers)

        if (lastPlayedCards is HighCard && lastPlayedCards.card == Dog) {
            val nextTrickPlayer = nextPlayer(nextPlayer(currentPlayer))
            println("Player $currentPlayer played Dog. Turn for player $nextTrickPlayer")
            sendToAllPlayers(TrickWon(nextTrickPlayer))
            return Triple(nextTrickPlayer, outOfCardsPlayers, nextTrickPlayer to lastPlayedCards.cards.toSet())
        }

        var lastPlayedPlayer = trickPlayer
        val trickCards: MutableSet<Card> = lastPlayedCards.cards.toMutableSet()

        currentPlayer = nextPlayer(currentPlayer)

        while (folds < 3) {
            if (playerCards.getValue(currentPlayer).isEmpty()) {
                println("Player $currentPlayer has no cards and folds")
                folds++
                sendToAllPlayers(PlayerFolds(currentPlayer))
            } else {
                sendToPlayer(currentPlayer, RequestPlayCards)
                while (true) {
                    val (playerId, cards) = receiveFromPlayer()
                    println("Player $playerId sent action $cards")
                    when {
                        playerId == currentPlayer && cards is PlayCards -> {
                            val playedCombination = findCardCombination(cards.cards, cards.addons)

                            println("Player $currentPlayer played cards $playedCombination")

                            if (!playerCards.getValue(playerId).containsAll(cards.cards)) {
                                sendToPlayer(playerId, IllegalAction("Player does not have played cards", cards))
                                continue
                            }
                            if (playedCombination == null) {
                                sendToPlayer(playerId, IllegalAction("Not a valid combination", cards))
                                continue
                            }
                            if (!canCardsBePlayed(lastPlayedCards, playedCombination)) {
                                sendToPlayer(playerId, IllegalAction("This combination is incompatible with previously played cards", cards))
                                continue
                            }
                            if (mahjongWish.present &&
                                !cards.cards.filterIsInstance<NumberedCard>().any { it.value == mahjongWish.value } &&
                                cardsContainWish(mahjongWish.value, lastPlayedCards, playerCards.getValue(currentPlayer))
                            ) {
                                sendToPlayer(playerId, IllegalAction("Must play the Mahjong wish", cards))
                                continue
                            }

                            lastPlayedCards = playedCombination
                            trickCards.addAll(lastPlayedCards.cards)
                            playerCards.getValue(currentPlayer).removeAll(playedCombination.cards)
                            lastPlayedPlayer = currentPlayer
                            folds = 0

                            sendToAllPlayers(PlayerPlayedCards(currentPlayer, playedCombination))

                            if (mahjongWish.present && playedCombination.cards.filterIsInstance<NumberedCard>().any { it.value == mahjongWish.value }) {
                                mahjongWish.fulfilled()
                                sendToAllPlayers(MahjongWishFulfilled)
                            }

                            if (playerCards.getValue(currentPlayer).isEmpty()) {
                                println("Player $currentPlayer is out of cards")
                                outOfCardsPlayers.add(currentPlayer)
                            }

                            break
                        }
                        playerId == currentPlayer && cards is Fold -> {
                            if (mahjongWish.present && cardsContainWish(mahjongWish.value, lastPlayedCards, playerCards.getValue(currentPlayer))) {
                                sendToPlayer(playerId, IllegalAction("Must play the Mahjong wish", cards))
                                continue
                            }

                            println("Player $currentPlayer folds")
                            folds++
                            sendToAllPlayers(PlayerFolds(currentPlayer))
                            break
                        }
                        playerId != currentPlayer && cards is PlayCards -> {
                            val playedCombination = findCardCombination(cards.cards, cards.addons)
                            if (!playerCards.getValue(playerId).containsAll(cards.cards)) {
                                sendToPlayer(playerId, IllegalAction("Player does not have played cards", cards))
                                continue
                            }

                            when (playedCombination) {
                                null -> {
                                    sendToPlayer(playerId, IllegalAction("Not a valid combination", cards))
                                    continue
                                }
                                is Bomb -> {
                                    lastPlayedCards = playedCombination
                                    trickCards.addAll(lastPlayedCards.cards)
                                    playerCards.getValue(playerId).removeAll(playedCombination.cards)
                                    lastPlayedPlayer = playerId
                                    folds = 0

                                    sendToAllPlayers(PlayerPlayedCards(playerId, playedCombination))

                                    if (playerCards.getValue(playerId).isEmpty()) {
                                        println("Player $playerId is out of cards")
                                        outOfCardsPlayers.add(playerId)
                                    }

                                    break
                                }
                                else ->
                                    sendToPlayer(playerId, IllegalAction("Illegal card combination in turn of other player", cards))
                            }
                        }
                        cards is CallTaiPan -> when {
                            taiPannedPlayers.containsKey(playerId) ->
                                sendToPlayer(playerId, IllegalAction("Already tai panned", cards))
                            playerCards.getValue(playerId).size < 14 ->
                                sendToPlayer(playerId, IllegalAction("Already played a card", cards))
                            else -> {
                                taiPannedPlayers[playerId] = TaiPanStatus.NORMAL
                                sendToAllPlayers(PlayerTaiPanned(playerId, TaiPanStatus.NORMAL))
                            }
                        }
                        else ->
                            sendToPlayer(playerId, IllegalAction("Not allowed", cards))
                    }
                }
            }

            currentPlayer = nextPlayer(currentPlayer)
        }

        println("Player $lastPlayedPlayer won the trick, leftover cards $playerCards")
        val nextTrickPlayer = lastPlayedPlayer
        sendToAllPlayers(TrickWon(lastPlayedPlayer))
        return if (lastPlayedCards is HighCard && lastPlayedCards.card == Dragon) {
            sendToPlayer(currentPlayer, RequestPassDragon)
            var dragonPass: DragonPass? = null
            while (dragonPass == null) {
                val (playerId, action) = receiveFromPlayer()
                when {
                    // TODO send event
                    playerId == currentPlayer && action is PassDragonTrick ->
                        dragonPass = action.dragonPass
                    action is CallTaiPan -> when {
                        taiPannedPlayers.containsKey(playerId) ->
                            sendToPlayer(playerId, IllegalAction("Already tai panned", action))
                        playerCards.getValue(playerId).size < 14 ->
                            sendToPlayer(playerId, IllegalAction("Already played a card", action))
                        else -> {
                            taiPannedPlayers[playerId] = TaiPanStatus.NORMAL
                            sendToAllPlayers(PlayerTaiPanned(playerId, TaiPanStatus.NORMAL))
                        }
                    }
                    else ->
                        sendToPlayer(playerId, IllegalAction("Not allowed", action))
                }
            }

            val dragonPlayer = when (dragonPass) {
                DragonPass.LEFT -> nextPlayer(nextTrickPlayer)
                DragonPass.RIGHT -> nextPlayer(nextPlayer(nextPlayer(nextTrickPlayer)))
            }
            Triple(nextTrickPlayer, outOfCardsPlayers, dragonPlayer to trickCards)
        } else {
            Triple(nextTrickPlayer, outOfCardsPlayers, nextTrickPlayer to trickCards)
        }
    }

    private suspend fun TaiPanGameContext.playInitialCards(
        currentPlayer: TwoTeamPlayerId,
        playerCards: Map<TwoTeamPlayerId, MutableList<Card>>,
        mahjongWish: MahjongWish,
        outOfCardsPlayers: MutableList<TwoTeamPlayerId>,
        taiPannedPlayers: MutableMap<TwoTeamPlayerId, TaiPanStatus>
    ): CardCombination {
        sendToPlayer(currentPlayer, RequestPlayCards)
        while (true) {
            val (playerId, cards) = receiveFromPlayer()
            println("Player $playerId played starting cards $cards")

            when {
                playerId == currentPlayer && cards is PlayCards -> {
                    val playedStartingCards = findCardCombination(cards.cards, cards.addons)

                    if (!playerCards.getValue(playerId).containsAll(cards.cards)) {
                        sendToPlayer(playerId, IllegalAction("Player does not have played cards", cards))
                        continue
                    }
                    if (playedStartingCards == null) {
                        sendToPlayer(playerId, IllegalAction("Not a valid combination", cards))
                        continue
                    }
                    if (mahjongWish.present &&
                        !cards.cards.filterIsInstance<NumberedCard>().any { it.value == mahjongWish.value } &&
                        cardsContainWish(mahjongWish.value, playerCards.getValue(currentPlayer))
                    ) {
                        sendToPlayer(playerId, IllegalAction("Must play the Mahjong wish", cards))
                        continue
                    }

                    playerCards.getValue(currentPlayer).removeAll(playedStartingCards.cards)
                    sendToAllPlayers(PlayerPlayedCards(currentPlayer, playedStartingCards))

                    if (playerCards.getValue(currentPlayer).isEmpty()) {
                        println("Player $currentPlayer is out of cards")
                        outOfCardsPlayers.add(currentPlayer)
                    }

                    if (mahjongWish.present && playedStartingCards.cards.filterIsInstance<NumberedCard>().any { it.value == mahjongWish.value }) {
                        mahjongWish.fulfilled()
                        sendToAllPlayers(MahjongWishFulfilled)
                    } else if (playedStartingCards.cards.any { it is Mahjong } && cards.addons.any { it is MahjongRequest }) {
                        mahjongWish.wish(cards.addons.filterIsInstance<MahjongRequest>().first())
                        sendToAllPlayers(MahjongWishRequested(mahjongWish.value))
                    }

                    return playedStartingCards
                }
                cards is CallTaiPan -> when {
                    taiPannedPlayers.containsKey(playerId) ->
                        sendToPlayer(playerId, IllegalAction("Already tai panned", cards))
                    playerCards.getValue(playerId).size < 14 ->
                        sendToPlayer(playerId, IllegalAction("Already played a card", cards))
                    else -> {
                        taiPannedPlayers[playerId] = TaiPanStatus.NORMAL
                        sendToAllPlayers(PlayerTaiPanned(playerId, TaiPanStatus.NORMAL))
                    }
                }
                else ->
                    sendToPlayer(playerId, IllegalAction("Not allowed", cards))
            }
        }
    }

    private fun nextPlayer(playerId: TwoTeamPlayerId): TwoTeamPlayerId =
        when (playerId) {
            TwoTeamPlayerId.PLAYER1 -> TwoTeamPlayerId.PLAYER2
            TwoTeamPlayerId.PLAYER2 -> TwoTeamPlayerId.PLAYER3
            TwoTeamPlayerId.PLAYER3 -> TwoTeamPlayerId.PLAYER4
            TwoTeamPlayerId.PLAYER4 -> TwoTeamPlayerId.PLAYER1
        }

    private fun canCardsBePlayed(previous: CardCombination, current: CardCombination): Boolean {
        return when (previous) {
            is QuadrupleBomb ->
                current is QuadrupleBomb && previous.value < current.value
            is StraightBomb ->
                current is StraightBomb && previous.length < current.length
            is HighCard ->
                current is HighCard && when (previous.card) {
                    is NumberedCard ->
                        current.card is Phoenix || current.card is Dragon ||
                            (current.card is NumberedCard && previous.card.value < current.card.value)
                    is Phoenix -> current.card is Dragon
                    else -> false
                }
            is Tuple ->
                current is Tuple && previous.value < current.value
            is Triple ->
                current is Triple && previous.value < current.value
            is TupleSequence ->
                current is TupleSequence && previous.length == current.length && previous.minValue < current.minValue
            is FullHouse ->
                current is FullHouse && previous.triple.value < current.triple.value
            is Straight ->
                current is Straight && previous.length == current.length && previous.minValue < current.minValue
        }
    }

    private fun cardsContainWish(wish: Int, cards: Collection<Card>): Boolean =
        cards.filterIsInstance<NumberedCard>()
            .any { it.value == wish }

    // TODO unit test the shit out of this
    private fun hasStraightOfLengthAndContainsValue(value: Int, minLength: Int, cards: Collection<Card>): Boolean {
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

    private fun cardsContainWish(wish: Int, previousCards: CardCombination, cards: Collection<Card>): Boolean {
        if (!cardsContainWish(wish, cards)) {
            return false
        }

        val cardValueCount = cards.filterIsInstance<NumberedCard>()
            .groupBy { it.value }
            .mapValues { (_, cards) -> cards.size }
            .withDefault { 0 }

        val hasWishBomb = cardValueCount.getValue(wish) == 4 ||
            cards.filterIsInstance<NumberedCard>()
                .filter { it.value == wish }
                .map { it.suit }
                .any { suit ->
                    val suitCards = cards
                        .filterIsInstance<NumberedCard>()
                        .filter { it.suit == suit }
                    hasStraightOfLengthAndContainsValue(wish, 5, suitCards)
                }
        val hasPhoenix = cards.any { it is Phoenix }

        return when (previousCards) {
            is QuadrupleBomb ->
                previousCards.value < wish && cardValueCount.getValue(wish) == 4
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
                hasWishBomb || (previousCards.card is NumberedCard && previousCards.card.value < wish)
            is Tuple ->
                hasWishBomb || (previousCards.value < wish && (cardValueCount.getValue(wish) >= 2 || cardValueCount.getValue(wish) == 1 && hasPhoenix))
            is Triple ->
                hasWishBomb || (previousCards.value < wish && (cardValueCount.getValue(wish) >= 3 || cardValueCount.getValue(wish) == 2 && hasPhoenix))
            is FullHouse ->
                hasWishBomb ||
                    if (hasPhoenix) {
                        (previousCards.value < wish && cardValueCount.getValue(wish) >= 3 && cardValueCount.filter { (number, count) -> number != wish && count >= 1 }.isNotEmpty()) ||
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
                hasStraightOfLengthAndContainsValue(wish, previousCards.length, cards.filterIsInstance<NumberedCard>().filter { previousCards.minValue < it.value })
        }
    }
}

data class TaiPanGameParameters(val points: Int, val seed: Long) : GameParameters

sealed class PlayCardsAddon
data class PhoenixValue(val value: Int) : PlayCardsAddon() {
    init {
        require(value >= 2) { "The card value should be greater or equal to two." }
        require(value <= NumberedCard.ACE) { "The card value should be less than or equal to ACE (14)." }
    }
}

data class MahjongRequest(val value: Int) : PlayCardsAddon() {
    init {
        require(value >= 2) { "The card value should be greater or equal to two." }
        require(value <= NumberedCard.ACE) { "The card value should be less than or equal to ACE (14)." }
    }
}

class MahjongWish {
    private var wish: Int? = null
    val present: Boolean
        get() = wish != null

    // Check if present before getting value
    val value: Int
        get() = wish!!

    fun wish(request: MahjongRequest) {
        wish = request.value
    }

    fun fulfilled() {
        wish = null
    }
}

sealed class TaiPanGameResult : GameResult
object Team1Won : TaiPanGameResult()
object Team2Won : TaiPanGameResult()
