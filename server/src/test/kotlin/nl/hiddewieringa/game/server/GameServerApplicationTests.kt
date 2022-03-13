package nl.hiddewieringa.game.server

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = [
    "database.url=postgres://game:game@127.0.0.1:5432/game",
    "database.ssl=false",
])
class GameServerApplicationTests {

    @Test
    fun contextLoads() {
    }
}
