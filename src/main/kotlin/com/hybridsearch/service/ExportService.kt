// service/ExportService.kt
package com.hybridsearch.service

import com.hybridsearch.config.RabbitMQConfig
import com.hybridsearch.model.ExportTask
import com.hybridsearch.model.SearchRequest
import com.hybridsearch.model.TaskStatus
import com.hybridsearch.utils.CompressionUtil
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*

@Service
class ExportService(
    private val rabbitTemplate: RabbitTemplate,
    private val searchService: SearchService,
    private val redisTemplate: ReactiveRedisTemplate<String, Any>,
    private val minioService: MinioService
) {

    @Value("\${app.export.max-file-size}")
    private lateinit var maxFileSize: String

    suspend fun requestExport(searchRequest: SearchRequest): String {
        val task = ExportTask(searchRequest = searchRequest)

        // 檢查結果數量
        val resultCount = searchService.countResults(searchRequest)
        if (resultCount > 50000) { // 限制最大匯出數量
            throw IllegalArgumentException("Too many results to export: $resultCount")
        }

        // 保存任務狀態到 Redis
        redisTemplate.opsForValue()
            .set("export:task:${task.taskId}", task, Duration.ofHours(24))
            .awaitSingle()

        // 發送異步任務到 RabbitMQ
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXPORT_EXCHANGE,
            RabbitMQConfig.EXPORT_ROUTING_KEY,
            task
        )

        return task.taskId
    }

    suspend fun getTaskStatus(taskId: String): ExportTask? {
        return redisTemplate.opsForValue()
            .get("export:task:$taskId")
            .cast(ExportTask::class.java)
            .awaitSingleOrNull()
    }

    suspend fun processExportTask(task: ExportTask) {
        try {
            // 更新任務狀態為處理中
            val processingTask = task.copy(status = TaskStatus.PROCESSING)
            updateTaskStatus(processingTask)

            // 獲取搜索結果
            val results = searchService.hybridSearch(task.searchRequest)

            // 生成壓縮文件
            val fileName = "export_${task.taskId}.${task.format.name.lowercase()}"
            val compressedData = CompressionUtil.compressData(results, task.format)

            // 上傳到 MinIO
            val downloadUrl = minioService.uploadFile(fileName, compressedData)

            // 更新任務狀態為完成
            val completedTask = processingTask.copy(
                status = TaskStatus.COMPLETED,
                downloadUrl = downloadUrl,
                completedAt = java.time.LocalDateTime.now()
            )
            updateTaskStatus(completedTask)

        } catch (e: Exception) {
            // 更新任務狀態為失敗
            val failedTask = task.copy(status = TaskStatus.FAILED)
            updateTaskStatus(failedTask)
            throw e
        }
    }

    private suspend fun updateTaskStatus(task: ExportTask) {
        redisTemplate.opsForValue()
            .set("export:task:${task.taskId}", task, Duration.ofHours(24))
            .awaitSingle()
    }
}