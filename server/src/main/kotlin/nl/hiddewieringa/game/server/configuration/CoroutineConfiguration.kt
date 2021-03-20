package nl.hiddewieringa.game.server.configuration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineConfiguration {

    @Bean
    fun coroutineScope(): CoroutineScope =
        CoroutineScope(Dispatchers.Default)
}
