// service/SearchService.kt
package com.hybridsearch.service

import com.hybridsearch.model.FinewebData
import com.hybridsearch.model.SearchRequest
import com.hybridsearch.repository.elasticsearch.FinewebElasticsearchRepository
import com.hybridsearch.repository.r2dbc.FinewebDataRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import java.util.*


@Service
class SearchService(
    private val searchCacheService: SearchCacheService,
    private val r2dbcRepository: FinewebDataRepository,
    private val elasticsearchRepository: FinewebElasticsearchRepository
) {

    suspend fun hybridSearch(request: SearchRequest): List<FinewebData> {
        return searchCacheService.textSearch(request).collectList().awaitSingle()
    }

    suspend fun getById(id: UUID): FinewebData? {
        return r2dbcRepository.findById(id).awaitSingleOrNull()
    }

    suspend fun countResults(request: SearchRequest): Long {
        return if (request.query.isBlank()) {
            r2dbcRepository.count().awaitSingle()
        } else {
            elasticsearchRepository.count().awaitSingle()
        }
    }
}