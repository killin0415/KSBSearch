package com.hybridsearch.config


import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.transaction.ReactiveTransactionManager

@Configuration
class DatabaseConfig : AbstractR2dbcConfiguration() {

    @Value("\${spring.r2dbc.url}")
    private lateinit var url: String

    @Value("\${spring.r2dbc.username}")
    private lateinit var username: String

    @Value("\${spring.r2dbc.password}")
    private lateinit var password: String

    @Bean
    override fun connectionFactory() = PostgresqlConnectionFactory(
        PostgresqlConnectionConfiguration.builder()
            .host("localhost")
            .port(5432)
            .database("hybridsearch")
            .username(username)
            .password(password)
            .build()
    )

    @Bean
    fun transactionManager(): ReactiveTransactionManager {
        return R2dbcTransactionManager(connectionFactory())
    }
}