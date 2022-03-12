package nl.hiddewieringa.game.server.application

import org.springframework.stereotype.Service
import java.util.*

@Service
class InstanceId {

    val id: UUID = UUID.randomUUID()

}