package com.example.ksbsearch

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun elasticsearchContainer(): ElasticsearchContainer {
        return ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.17.10"))
    }

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> {
        return PostgreSQLContainer(DockerImageName.parse("postgres:latest"))
    }

    @Bean
    @ServiceConnection
    fun rabbitContainer(): RabbitMQContainer {
        return RabbitMQContainer(DockerImageName.parse("rabbitmq:latest"))
    }

    @Bean
    @ServiceConnection(name = "redis")
    fun redisContainer(): GenericContainer<*> {
        return GenericContainer(DockerImageName.parse("redis:latest")).withExposedPorts(6379)
    }

    @Bean
    @ServiceConnection(name = "redis")
    fun redisStackContainer(): GenericContainer<*> {
        return GenericContainer(DockerImageName.parse("redis/redis-stack:latest")).withExposedPorts(6379)
    }

}
