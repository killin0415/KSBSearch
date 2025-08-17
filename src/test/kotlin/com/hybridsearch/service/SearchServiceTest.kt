// test/kotlin/com/hybridsearch/SearchServiceTest.kt
package com.hybridsearch.service

import com.hybridsearch.model.SearchRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
@TestPropertySource(locations = ["classpath:application-test.properties"])
class SearchServiceTest {

    companion object {
        @Container
        @JvmStatic
        val elasticsearch = ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
            .withExposedPorts(9200)
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
    }

    @Test
    fun `test basic search functionality`() = runBlocking {
        // 基礎搜索功能測試
        val searchRequest = SearchRequest(
            query = "test",
            page = 0,
            size = 10
        )

        // 實際測試邏輯將在完整實現後添加
    }
}