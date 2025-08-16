// HybridSearchApplication.kt
package com.hybridsearch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@SpringBootApplication
@EnableCaching
@EnableR2dbcRepositories(basePackages = ["com.hybridsearch.repository.r2dbc"])
@EnableReactiveElasticsearchRepositories(basePackages = ["com.hybridsearch.repository.elasticsearch"])
class HybridSearchApplication

fun main(args: Array<String>) {
    runApplication<HybridSearchApplication>(*args)
}