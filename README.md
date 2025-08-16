
# **HybridSearch-ES（Kotlin Spring Boot 版本）完整設計文檔**

## 1. 專案簡介

HybridSearch-ES 是一個基於 **Kotlin + Spring Boot 3.x** 的高併發分散式檢索與匯出系統，主要用於為 LLM Fine-Tuning（FT）提供大規模文本檢索與封裝下載功能。
新版使用 **Spring WebFlux + Coroutines + Virtual Threads** 提供非阻塞高併發處理能力，並與 PostgreSQL、Elasticsearch、Redis、RabbitMQ 深度整合，支援秒級檢索與異步檔案生成，重點優化了可維護性與擴展性。

***

## 2. Tech Stack

- **Language**：Kotlin (JDK 21)
- **Framework**：Spring Boot 3.x（WebFlux，Virtual Threads）
- **Database**：
    - PostgreSQL（Metadata 儲存 + 結構化查詢）
    - Elasticsearch（全文檢索 + 向量檢索）
- **Cache**：Spring Cache（Redis 後端）
- **Message Queue**：RabbitMQ（Spring AMQP）
- **Container Orchestration**：Kubernetes
- **Interface**：HTTP RESTful API（Spring WebFlux）
- **Object Storage**：MinIO / AWS S3
- **Observability**：Spring Boot Actuator + Prometheus + OpenTelemetry

***

## 3. 系統需求與特性

### 核心功能

- 關鍵字檢索（BM25 + 向量檢索混合）
- 以文件集合生成壓縮包（ZIP/TAR）
- 透過 Object Storage 提供檔案下載連結


### 核心特性

- 高併發 \& 非阻塞
- 多層快取策略
- 異步匯出工作流
- 支援數千萬筆檢索數據
- 自動化監控 \& 健康檢查

***

## 4. 資料結構（PostgreSQL）

```sql
create table fineweb_data
(
    id uuid primary key,
    text text,
    url text,
    date timestamp,
    language varchar(8),
    language_score double precision,
    script varchar(32),
    top_langs jsonb,
    avg_words_per_line double precision,
    metadata jsonb
);

create index idx_fineweb_data_lang on fineweb_data(language);
create index idx_fineweb_data_date on fineweb_data(date);
create index idx_fineweb_data_metadata on fineweb_data using gin (metadata);
```

> 原則同 Go 版本，差異在於後續透過 **Spring Data JPA** 與 **R2DBC** 進行非阻塞存取。

***

## 5. 架構設計與優化策略

### 調整重點

1. **Reactive 架構**：Spring WebFlux 搭配 Coroutine Flow 處理檢索流程
2. **異步任務**：使用 Spring AMQP 消費 RabbitMQ 任務
3. **快取整合**：Spring Cache + Redis 儲存熱查詢和檔案 URL
4. **Streaming 壓縮**：Kotlin Coroutine + `java.util.zip` 邊讀邊寫
5. **限流**：Spring Cloud Gateway 層加入 RateLimiter + Resilience4j

***

## 6. Elasticsearch 索引建議

```json
{
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "text": { "type": "text", "analyzer": "standard" },
      "vector_embedding": { "type": "dense_vector", "dims": 768 },
      "language": { "type": "keyword" },
      "date": { "type": "date" },
      "script": { "type": "keyword" },
      "metadata": { "type": "nested" }
    }
  }
}
```

> 實作透過 **Spring Data Elasticsearch**，方便版本管理與測試。

***

## 7. 高併發資料流（Spring Boot + WebFlux 版本）

```
[Client Request]
↓
[Spring WebFlux Controller]
↓ (Redis Cache 檢查)
Cache Hit → 返回 URL
Cache Miss →
↓
[Elasticsearch 檢索 (Reactive Client)]
↓
[任務封裝後發送至 RabbitMQ]
↓
[Spring AMQP Worker 消費字任務]
↓
[Streaming 壓縮檔案 → 存 Object Storage]
↓
[更新 Redis Cache（URL）]
↓
返回下載連結
```


***

## 8. 效能優化重點

1. **JVM 調優**：G1GC，`-XX:MaxRAMPercentage=75`，Virtual Threads 適用於阻塞 I/O
2. **非阻塞 API**：使用 WebFlux + Coroutines 取代 Servlet Thread Pool 模型
3. **快取層級**：
    - 查詢結果 TTL：5 分鐘
    - 檔案 URL TTL：60 分鐘
    - 熱資料預載入
4. **分區表策略**：PostgreSQL 按時間或語言 Hash 分區
5. **Elasticsearch 分片**：20-40GB/分片 + SSD 優化

***

## 9. SLA / 預期效能

- 資料量：50,000,000 筆
- P95 查詢延遲：< 600ms（Reactive 架構）
- 檔案生成延遲：秒級～分鐘級
- 系統可用性：≥ 99.95%
- 最終一致性收斂：< 5 秒
