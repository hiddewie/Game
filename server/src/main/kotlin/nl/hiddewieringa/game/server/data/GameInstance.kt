package nl.hiddewieringa.game.server.data

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob

@Entity
data class GameInstance(
    @Id
    val id: UUID,

    val gameSlug: String,

    @Lob
    @Column(columnDefinition = "TEXT")
    val serializedPlayerSlots: String,

    @Lob
    @Column(columnDefinition = "TEXT")
    val serializedState: String,
) {

    // TODO
//    fun open() =
//        openSlots > 0

}

interface GameInstanceRepository : CrudRepository<GameInstance, UUID> {

    @Query("select g from GameInstance g where g.gameSlug = :gameSlug") // TODO and openSlots > 0
    fun openGames(gameSlug: String): List<GameInstance>

}