package nl.hiddewieringa.game.core

interface PlayerId

sealed class PlayerConfiguration<ID : PlayerId, P : Player<*, *, *, *, *>>(
    val allPlayers: Set<ID>
) : Set<ID> by allPlayers {
    abstract fun player(id: ID): P
}

data class SinglePlayer<P : Player<*, *, *, *, *>>(
    val player: P
) : PlayerConfiguration<SingletonPlayer, P>(setOf(SingletonPlayer.PLAYER)) {
    override fun player(id: SingletonPlayer) =
        player
}

enum class SingletonPlayer : PlayerId {
    PLAYER
}

data class TwoPlayers<P : Player<*, *, *, *, *>>(
    val player1: P,
    val player2: P
) : PlayerConfiguration<TwoPlayerId, P>(setOf(TwoPlayerId.PLAYER1, TwoPlayerId.PLAYER2)) {
    override fun player(id: TwoPlayerId) =
        when (id) {
            TwoPlayerId.PLAYER1 -> player1
            TwoPlayerId.PLAYER2 -> player2
        }
}

enum class TwoPlayerId : PlayerId {
    PLAYER1,
    PLAYER2
}

data class TwoTeams<P : Player<*, *, *, *, *>>(
    val team1: TwoPlayers<P>,
    val team2: TwoPlayers<P>
) : PlayerConfiguration<TwoTeamPlayerId, P>(setOf(TwoTeamPlayerId.PLAYER1, TwoTeamPlayerId.PLAYER2, TwoTeamPlayerId.PLAYER3, TwoTeamPlayerId.PLAYER4)) {
    override fun player(id: TwoTeamPlayerId): P =
        when (id) {
            TwoTeamPlayerId.PLAYER1 -> team1.player(TwoPlayerId.PLAYER1)
            TwoTeamPlayerId.PLAYER2 -> team1.player(TwoPlayerId.PLAYER2)
            TwoTeamPlayerId.PLAYER3 -> team2.player(TwoPlayerId.PLAYER1)
            TwoTeamPlayerId.PLAYER4 -> team2.player(TwoPlayerId.PLAYER2)
        }

    fun team(playerId: TwoTeamPlayerId): TwoTeamTeamId =
        when (playerId) {
            TwoTeamPlayerId.PLAYER1, TwoTeamPlayerId.PLAYER3 -> TwoTeamTeamId.TEAM1
            TwoTeamPlayerId.PLAYER2, TwoTeamPlayerId.PLAYER4 -> TwoTeamTeamId.TEAM2
        }
}

enum class TwoTeamPlayerId(
    val team: TwoTeamTeamId,
) : PlayerId {
    PLAYER1(TwoTeamTeamId.TEAM1),
    PLAYER2(TwoTeamTeamId.TEAM2),
    PLAYER3(TwoTeamTeamId.TEAM1),
    PLAYER4(TwoTeamTeamId.TEAM2),
}

interface TeamId

enum class TwoTeamTeamId : TeamId {
    TEAM1,
    TEAM2
}
