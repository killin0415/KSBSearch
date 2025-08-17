// handler/ExportTaskHandler.kt
package com.hybridsearch.handler

import com.hybridsearch.config.RabbitMQConfig
import com.hybridsearch.model.ExportTask
import com.hybridsearch.service.ExportService
import kotlinx.coroutines.runBlocking
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

@Component
class ExportTaskHandler(private val exportService: ExportService) {

    private val logger = LoggerFactory.getLogger(ExportTaskHandler::class.java)

    @RabbitListener(queues = [RabbitMQConfig.EXPORT_QUEUE])
    fun handleExportTask(task: ExportTask) {
        logger.info("Processing export task: ${task.taskId}")

        try {
            runBlocking {
                exportService.processExportTask(task)
            }
            logger.info("Export task completed: ${task.taskId}")
        } catch (e: Exception) {
            logger.error("Export task failed: ${task.taskId}", e)
        }
    }
}