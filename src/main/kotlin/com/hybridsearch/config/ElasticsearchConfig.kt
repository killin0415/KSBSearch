package com.hybridsearch.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchConfiguration
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories

@Configuration
@EnableReactiveElasticsearchRepositories
class ElasticsearchConfig : ReactiveElasticsearchConfiguration() {

    @Value("\${spring.elasticsearch.uris}")
    private lateinit var uris: String

    @Value("\${spring.elasticsearch.username}")
    private lateinit var username: String

    @Value("\${spring.elasticsearch.password}")
    private lateinit var password: String

    override fun clientConfiguration(): ClientConfiguration {
        return ClientConfiguration.builder()
            .connectedTo(uris.removePrefix("http://"))
            .withBasicAuth(username, password)
            .build()
    }
}

