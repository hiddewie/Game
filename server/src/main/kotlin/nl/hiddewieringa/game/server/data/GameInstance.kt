package nl.hiddewieringa.game.server.data

import org.hibernate.annotations.Cascade
import org.hibernate.annotations.CascadeType
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.persistence.*

@Entity
class GameInstance(
    @Id
    var id: UUID,

    var gameSlug: String,

    @Lob
    @Column(columnDefinition = "TEXT")
    var serializedState: String,

    @OneToMany(orphanRemoval = true, mappedBy = "gameInstance")
    @Cascade(CascadeType.ALL)
    @MapKey(name = "id")
    var playerSlots: Map<UUID, PlayerSlot>
)

@Repository
interface GameInstanceRepository : CrudRepository<GameInstance, UUID> {

    @Query("select g from GameInstance g where g.gameSlug = :gameSlug and (select count(ps) from g.playerSlots ps where ps.lockedUntil <= :referenceTime) > 0")
    fun openGames(gameSlug: String, referenceTime: Instant): List<GameInstance>

}

@Entity
class PlayerSlot(
    @Id
    var id: UUID,

    @ManyToOne
    @JoinColumn(name = "game_instance")
    private var gameInstance: GameInstance,

    var serializedPlayerId: String,

    var lockedUntil: Instant,
) {
    companion object {
        val PLAYER_SLOT_LOCK_DURATION: Duration = Duration.ofSeconds(30)
    }
}

@Repository
interface PlayerSlotRepository : CrudRepository<PlayerSlot, UUID> {

    @Modifying
    @Query("update PlayerSlot ps set ps.lockedUntil = :until where ps.gameInstance.id = :gameInstance and ps.id = :playerSlot")
    fun lockUntil(gameInstance: UUID, playerSlot: UUID, until: Instant)

    @Modifying
    fun removeLock(gameInstance: UUID, playerSlot: UUID) =
        lockUntil(gameInstance, playerSlot, Instant.EPOCH)

}