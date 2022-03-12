package nl.hiddewieringa.game.server.data

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI
import javax.sql.DataSource

@Configuration
class PostgresConfiguration {

    @Bean
    fun dataSource(
        @Value("\${database.url}") uri: URI,
        @Value("\${database.ssl}") enableSsl: Boolean,
    ): DataSource {
        val userInfo = uri.userInfo

        return DataSourceBuilder.create()
            .driverClassName("org.postgresql.Driver")
            .url("jdbc:postgresql://${uri.host}:${uri.port}${uri.path}?sslmode=${if (enableSsl) "require" else "disable"}")
            .username(userInfo.split(":")[0])
            .password(userInfo.split(":")[1])
            .build()
    }

}