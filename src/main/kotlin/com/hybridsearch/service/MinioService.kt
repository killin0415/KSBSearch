package com.hybridsearch.service

import io.minio.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import io.minio.http.Method

@Service
class MinioService(private val minioClient: MinioClient) {

    @Value("\${minio.bucket-name}")
    private lateinit var bucketName: String

    suspend fun uploadFile(fileName: String, data: ByteArray): String {
        // 檢查 bucket 是否存在
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
        }

        // 上傳文件
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(fileName)
                .stream(ByteArrayInputStream(data), data.size.toLong(), -1)
                .contentType("application/octet-stream")
                .build()
        )

        // 生成預簽名下載 URL (7天有效期)
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .`object`(fileName)
                .expiry(7 * 24 * 60 * 60) // 7 days
                .build()
        )
    }

    suspend fun deleteFile(fileName: String) {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .`object`(fileName)
                .build()
        )
    }
}