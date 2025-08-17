// controller/SearchController.kt
package com.hybridsearch.controller

import com.hybridsearch.model.ExportTask
import com.hybridsearch.model.FinewebData
import com.hybridsearch.model.SearchRequest
import com.hybridsearch.service.ExportService
import com.hybridsearch.service.SearchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = ["*"])
class SearchController(
    private val searchService: SearchService,
    private val exportService: ExportService
) {

    @PostMapping("/search")
    suspend fun search(@RequestBody request: SearchRequest): ResponseEntity<SearchResponse> {
        val results = searchService.hybridSearch(request)
        val total = searchService.countResults(request)

        return ResponseEntity.ok(
            SearchResponse(
                results = results,
                total = total,
                page = request.page,
                size = request.size,
                hasMore = (request.page + 1) * request.size < total
            )
        )
    }

    @GetMapping("/search/{id}")
    suspend fun getById(@PathVariable id: UUID): ResponseEntity<FinewebData> {
        val result = searchService.getById(id)
        return if (result != null) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/export")
    suspend fun requestExport(@RequestBody request: SearchRequest): ResponseEntity<ExportResponse> {
        val taskId = exportService.requestExport(request)
        return ResponseEntity.accepted().body(
            ExportResponse(
                taskId = taskId,
                message = "Export task created successfully",
                statusUrl = "/api/v1/export/$taskId/status"
            )
        )
    }

    @GetMapping("/export/{taskId}/status")
    suspend fun getExportStatus(@PathVariable taskId: String): ResponseEntity<ExportTask> {
        val task = exportService.getTaskStatus(taskId)
        return if (task != null) {
            ResponseEntity.ok(task)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/health")
    suspend fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "timestamp" to java.time.LocalDateTime.now().toString()
        ))
    }
}

data class SearchResponse(
    val results: List<FinewebData>,
    val total: Long,
    val page: Int,
    val size: Int,
    val hasMore: Boolean
)

data class ExportResponse(
    val taskId: String,
    val message: String,
    val statusUrl: String
)