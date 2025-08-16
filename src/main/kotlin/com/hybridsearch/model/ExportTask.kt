package com.hybridsearch.model

import java.time.LocalDateTime
import java.util.UUID

data class ExportTask(
    val taskId: String = UUID.randomUUID().toString(),
    val searchRequest: SearchRequest,
    val format: ExportFormat = ExportFormat.ZIP,
    val status: TaskStatus = TaskStatus.PENDING,
    val downloadUrl: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null
)

enum class ExportFormat { ZIP, TAR }
enum class TaskStatus { PENDING, PROCESSING, COMPLETED, FAILED }
