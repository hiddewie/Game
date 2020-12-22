package nl.hiddewieringa.game.server.configuration

import nl.hiddewieringa.game.core.GameManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BeanProvider {

    @Bean
    fun gameManager(): GameManager =
        GameManager()
}
