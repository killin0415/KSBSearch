# HybridSearch-ES 配置與代碼文件

## 1. build.gradle.kts

```kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    kotlin("plugin.jpa") version "1.9.20"
}

group = "com.hybridsearch"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Database
    runtimeOnly("org.postgresql:r2dbc-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // MinIO/S3
    implementation("io.minio:minio:8.5.7")
    implementation("software.amazon.awssdk:s3:2.21.29")

    // Monitoring
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:elasticsearch")
    testImplementation("org.testcontainers:rabbitmq")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

## 2. application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: hybrid-search-es
  
  # R2DBC PostgreSQL Configuration
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/hybridsearch
    username: postgres
    password: password
    pool:
      initial-size: 10
      max-size: 50
      max-idle-time: 30m
      validation-query: SELECT 1

  # Elasticsearch Configuration
  elasticsearch:
    uris: http://localhost:9200
    username: elastic
    password: password
    connection-timeout: 10s
    socket-timeout: 30s

  # Redis Configuration
  data:
    redis:
      host: localhost
      port: 6379
      password: password
      database: 0
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms

  # RabbitMQ Configuration
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        concurrency: 5
        max-concurrency: 10
        retry:
          enabled: true
          initial-interval: 1000ms
          max-attempts: 3

  # Cache Configuration
  cache:
    type: redis
    redis:
      time-to-live: 600000 # 10 minutes
      cache-null-values: false

# MinIO Configuration
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: hybridsearch-exports

# Application Specific Configuration
app:
  search:
    max-results: 10000
    default-page-size: 20
    max-page-size: 100
  export:
    max-file-size: 1GB
    compression-level: 6
    temp-dir: /tmp/exports

# Logging Configuration
logging:
  level:
    com.hybridsearch: DEBUG
    org.springframework.data.elasticsearch: DEBUG
    reactor.netty: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# Management/Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

## 3. 核心 Kotlin 代碼

### 3.1 主應用程式類

```kotlin
// HybridSearchApplication.kt
package com.hybridsearch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@SpringBootApplication
@EnableCaching
@EnableR2dbcRepositories(basePackages = ["com.hybridsearch.repository.r2dbc"])
@EnableReactiveElasticsearchRepositories(basePackages = ["com.hybridsearch.repository.elasticsearch"])
class HybridSearchApplication

fun main(args: Array<String>) {
    runApplication<HybridSearchApplication>(*args)
}
```

### 3.2 數據模型

```kotlin
// model/FinewebData.kt
package com.hybridsearch.model

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

@Table("fineweb_data")
@Document(indexName = "fineweb_data")
data class FinewebData(
    @Id
    @Field(type = FieldType.Keyword)
    val id: UUID = UUID.randomUUID(),
    
    @Field(type = FieldType.Text, analyzer = "standard")
    val text: String,
    
    @Field(type = FieldType.Keyword)
    val url: String?,
    
    @Field(type = FieldType.Date)
    val date: LocalDateTime?,
    
    @Field(type = FieldType.Keyword)
    val language: String?,
    
    @Field(type = FieldType.Double)
    val languageScore: Double?,
    
    @Field(type = FieldType.Keyword)
    val script: String?,
    
    @Field(type = FieldType.Object)
    val topLangs: Map<String, Any>?,
    
    @Field(type = FieldType.Double)
    val avgWordsPerLine: Double?,
    
    @Field(type = FieldType.Object)
    val metadata: Map<String, Any>?,
    
    @Field(type = FieldType.Dense_Vector, dims = 768)
    val vectorEmbedding: FloatArray? = null
)

// model/SearchRequest.kt
data class SearchRequest(
    val query: String,
    val language: String? = null,
    val dateFrom: LocalDateTime? = null,
    val dateTo: LocalDateTime? = null,
    val page: Int = 0,
    val size: Int = 20,
    val useVector: Boolean = false,
    val filters: Map<String, Any> = emptyMap()
)

// model/ExportTask.kt
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
```

### 3.3 Repository 層

```kotlin
// repository/r2dbc/FinewebDataRepository.kt
package com.hybridsearch.repository.r2dbc

import com.hybridsearch.model.FinewebData
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import java.time.LocalDateTime
import java.util.*

interface FinewebDataRepository : ReactiveCrudRepository<FinewebData, UUID> {
    
    @Query("""
        SELECT * FROM fineweb_data 
        WHERE (:language IS NULL OR language = :language)
        AND (:dateFrom IS NULL OR date >= :dateFrom)
        AND (:dateTo IS NULL OR date <= :dateTo)
        ORDER BY date DESC
        LIMIT :limit OFFSET :offset
    """)
    fun findByFilters(
        language: String?,
        dateFrom: LocalDateTime?,
        dateTo: LocalDateTime?,
        limit: Int,
        offset: Long
    ): Flux<FinewebData>
}

// repository/elasticsearch/ElasticsearchRepository.kt
package com.hybridsearch.repository.elasticsearch

import com.hybridsearch.model.FinewebData
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository
import reactor.core.publisher.Flux
import java.util.*

interface FinewebElasticsearchRepository : ReactiveElasticsearchRepository<FinewebData, UUID> {
    fun findByTextContaining(text: String, pageable: Pageable): Flux<FinewebData>
    fun findByLanguage(language: String, pageable: Pageable): Flux<FinewebData>
}
```

### 3.4 配置類

```kotlin
// config/DatabaseConfig.kt
package com.hybridsearch.config

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.transaction.ReactiveTransactionManager

@Configuration
class DatabaseConfig : AbstractR2dbcConfiguration() {
    
    @Value("\${spring.r2dbc.url}")
    private lateinit var url: String
    
    @Value("\${spring.r2dbc.username}")
    private lateinit var username: String
    
    @Value("\${spring.r2dbc.password}")
    private lateinit var password: String
    
    @Bean
    override fun connectionFactory() = PostgresqlConnectionFactory(
        PostgresqlConnectionConfiguration.builder()
            .host("localhost")
            .port(5432)
            .database("hybridsearch")
            .username(username)
            .password(password)
            .build()
    )
    
    @Bean
    fun transactionManager(): ReactiveTransactionManager {
        return R2dbcTransactionManager(connectionFactory())
    }
}

// config/ElasticsearchConfig.kt
package com.hybridsearch.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchConfiguration
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories

@Configuration
@EnableReactiveElasticsearchRepositories
class ElasticsearchConfig : ReactiveElasticsearchConfiguration() {
    
    @Value("\${spring.elasticsearch.uris}")
    private lateinit var uris: String
    
    @Value("\${spring.elasticsearch.username}")
    private lateinit var username: String
    
    @Value("\${spring.elasticsearch.password}")
    private lateinit var password: String
    
    override fun clientConfiguration(): ClientConfiguration {
        return ClientConfiguration.builder()
            .connectedTo(uris.removePrefix("http://"))
            .withBasicAuth(username, password)
            .build()
    }
}

// config/RabbitMQConfig.kt
package com.hybridsearch.config

import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {
    
    companion object {
        const val EXPORT_EXCHANGE = "export.exchange"
        const val EXPORT_QUEUE = "export.queue"
        const val EXPORT_ROUTING_KEY = "export.task"
    }
    
    @Bean
    fun exportExchange(): TopicExchange = TopicExchange(EXPORT_EXCHANGE)
    
    @Bean
    fun exportQueue(): Queue = QueueBuilder
        .durable(EXPORT_QUEUE)
        .withArgument("x-dead-letter-exchange", "$EXPORT_EXCHANGE.dlx")
        .build()
    
    @Bean
    fun exportBinding(): Binding = BindingBuilder
        .bind(exportQueue())
        .to(exportExchange())
        .with(EXPORT_ROUTING_KEY)
    
    @Bean
    fun jsonMessageConverter(): Jackson2JsonMessageConverter = Jackson2JsonMessageConverter()
    
    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = jsonMessageConverter()
        return template
    }
}

// config/RedisConfig.kt
package com.hybridsearch.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
class RedisConfig {
    
    @Value("\${spring.data.redis.host}")
    private lateinit var host: String
    
    @Value("\${spring.data.redis.port}")
    private var port: Int = 6379
    
    @Bean
    fun reactiveRedisConnectionFactory(): ReactiveRedisConnectionFactory {
        return LettuceConnectionFactory(host, port)
    }
    
    @Bean
    fun reactiveRedisTemplate(
        connectionFactory: ReactiveRedisConnectionFactory
    ): ReactiveRedisTemplate<String, Any> {
        val context = RedisSerializationContext.newSerializationContext<String, Any>()
            .key(StringRedisSerializer())
            .value(GenericJackson2JsonRedisSerializer())
            .build()
        
        return ReactiveRedisTemplate(connectionFactory, context)
    }
    
    @Bean
    fun cacheManager(connectionFactory: ReactiveRedisConnectionFactory): CacheManager {
        val config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(GenericJackson2JsonRedisSerializer()))
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build()
    }
}
```

## 4. Docker Compose 配置

```yaml
# docker-compose.yml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:15-alpine
    container_name: hybridsearch-postgres
    environment:
      POSTGRES_DB: hybridsearch
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - hybridsearch-network

  # Elasticsearch
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: hybridsearch-elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms1g -Xmx1g"
    ports:
      - "9200:9200"
      - "9300:9300"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    networks:
      - hybridsearch-network

  # Redis
  redis:
    image: redis:7-alpine
    container_name: hybridsearch-redis
    command: redis-server --requirepass password
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - hybridsearch-network

  # RabbitMQ
  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: hybridsearch-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    networks:
      - hybridsearch-network

  # MinIO
  minio:
    image: minio/minio:latest
    container_name: hybridsearch-minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data
    networks:
      - hybridsearch-network

  # Application (開發階段可選)
  app:
    build: .
    container_name: hybridsearch-app
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - elasticsearch
      - redis
      - rabbitmq
      - minio
    environment:
      SPRING_PROFILES_ACTIVE: docker
    networks:
      - hybridsearch-network

volumes:
  postgres_data:
  elasticsearch_data:
  redis_data:
  rabbitmq_data:
  minio_data:

networks:
  hybridsearch-network:
    driver: bridge
```

## 5. Dockerfile

```dockerfile
# Dockerfile
FROM amazoncorretto:21-alpine as builder

WORKDIR /app
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew build -x test

FROM amazoncorretto:21-alpine

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

## 6. 初始化腳本

```sql
-- scripts/init.sql
CREATE TABLE IF NOT EXISTS fineweb_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    text TEXT NOT NULL,
    url TEXT,
    date TIMESTAMP,
    language VARCHAR(8),
    language_score DOUBLE PRECISION,
    script VARCHAR(32),
    top_langs JSONB,
    avg_words_per_line DOUBLE PRECISION,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_fineweb_data_lang ON fineweb_data(language);
CREATE INDEX IF NOT EXISTS idx_fineweb_data_date ON fineweb_data(date);
CREATE INDEX IF NOT EXISTS idx_fineweb_data_metadata ON fineweb_data USING gin (metadata);
CREATE INDEX IF NOT EXISTS idx_fineweb_data_text ON fineweb_data USING gin (to_tsvector('english', text));

-- Sample data for testing
INSERT INTO fineweb_data (text, url, language, language_score, script)
VALUES 
    ('Sample English text for testing purposes', 'https://example.com/1', 'en', 0.95, 'Latin'),
    ('這是一個中文測試文本', 'https://example.com/2', 'zh', 0.98, 'Han'),
    ('Ceci est un texte de test français', 'https://example.com/3', 'fr', 0.92, 'Latin');
```