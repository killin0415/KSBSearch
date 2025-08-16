package com.hybridsearch.repository.elasticsearch

import com.hybridsearch.model.FinewebData
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository
import reactor.core.publisher.Flux
import java.util.*

interface FinewebElasticsearchRepository : ReactiveElasticsearchRepository<FinewebData, UUID> {
    fun findByTextContaining(text: String, pageable: Pageable): Flux<FinewebData>
}