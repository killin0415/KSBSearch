package com.hybridsearch.config

import io.minio.MinioClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
class WebFluxConfig : WebFluxConfigurer {

    @Value("\${minio.endpoint}")
    private lateinit var minioEndpoint: String

    @Value("\${minio.access-key}")
    private lateinit var accessKey: String

    @Value("\${minio.secret-key}")
    private lateinit var secretKey: String

    @Value("\${minio.bucket-name}")
    private lateinit var bucketName: String


    @Bean
    fun minioClient(): MinioClient {
        return MinioClient.builder()
            .endpoint(minioEndpoint)
            .credentials(accessKey, secretKey)
            .build()
    }

    @Bean
    fun minioBucketName(): String {
        return bucketName
    }
}