package com.hybridsearch.model

import java.time.LocalDateTime

data class SearchRequest(
    val query: String,
    val page: Int = 0,
    val size: Int = 20,
    val useVector: Boolean = false,
    val filters: Map<String, Any> = emptyMap()
)