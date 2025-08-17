// util/CompressionUtil.kt
package com.hybridsearch.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hybridsearch.model.ExportFormat
import com.hybridsearch.model.FinewebData
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object CompressionUtil {

    private val objectMapper = jacksonObjectMapper()

    fun compressData(data: List<FinewebData>, format: ExportFormat): ByteArray {
        return when (format) {
            ExportFormat.ZIP -> compressToZip(data)
            ExportFormat.TAR -> compressToTar(data) // 簡化實現
        }
    }

    private fun compressToZip(data: List<FinewebData>): ByteArray {
        val outputStream = ByteArrayOutputStream()

        ZipOutputStream(outputStream).use { zipOut ->
            // 添加數據文件
            val jsonData = objectMapper.writeValueAsString(data)
            val entry = ZipEntry("data.json")
            entry.size = jsonData.toByteArray().size.toLong()

            zipOut.putNextEntry(entry)
            zipOut.write(jsonData.toByteArray())
            zipOut.closeEntry()

            // 添加元數據文件
            val metadata = mapOf(
                "exportedAt" to java.time.LocalDateTime.now().toString(),
                "totalRecords" to data.size,
                "format" to "JSON"
            )
            val metadataJson = objectMapper.writeValueAsString(metadata)
            val metadataEntry = ZipEntry("metadata.json")
            metadataEntry.size = metadataJson.toByteArray().size.toLong()

            zipOut.putNextEntry(metadataEntry)
            zipOut.write(metadataJson.toByteArray())
            zipOut.closeEntry()
        }

        return outputStream.toByteArray()
    }

    private fun compressToTar(data: List<FinewebData>): ByteArray {
        // 簡化的 TAR 實現，實際項目中應使用專業的 TAR 庫
        return compressToZip(data) // 暫時使用 ZIP 格式
    }
}