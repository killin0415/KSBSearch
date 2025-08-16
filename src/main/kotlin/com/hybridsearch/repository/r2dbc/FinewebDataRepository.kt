package com.hybridsearch.repository.r2dbc

import com.hybridsearch.model.FinewebData
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import java.time.LocalDateTime
import java.util.*

interface FinewebDataRepository : ReactiveCrudRepository<FinewebData, UUID> {

}