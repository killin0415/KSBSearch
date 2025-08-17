package com.hybridsearch.service

import com.hybridsearch.model.FinewebData
import com.hybridsearch.model.SearchRequest
import com.hybridsearch.repository.elasticsearch.FinewebElasticsearchRepository
import com.hybridsearch.repository.r2dbc.FinewebDataRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class SearchCacheService(
    private val elasticsearchRepository: FinewebElasticsearchRepository,
    private val r2dbcRepository: FinewebDataRepository
) {

    @Cacheable(value = ["searchResults"], key = "#request.hashCode()")
    fun textSearch(request: SearchRequest): Flux<FinewebData> {
        val pageable = PageRequest.of(request.page, request.size)

        return if (request.query.isBlank()) {
            r2dbcRepository.easyFilter(
                limit = request.size,
                offset = (request.page * request.size).toLong()
            )
        } else {
            elasticsearchRepository.findByTextContaining(request.query, pageable)
        }
    }
}
