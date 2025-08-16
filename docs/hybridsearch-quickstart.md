# HybridSearch-ES 兩天開發快速開始指南

## 🚀 快速開始 - 兩天完成目標

這是一個完整的兩天開發計劃，幫助你快速構建一個生產就緒的 Kotlin Spring Boot 混合搜索系統。

## 📋 前置準備 (開發前 30 分鐘)

### 開發環境要求
- **JDK 21** 或更高版本
- **IntelliJ IDEA** (推薦 Ultimate 版)
- **Docker** 和 **Docker Compose**
- **Git** 版本控制
- **Kotlin** 插件已安裝

### 快速環境設置
```bash
# 1. 克隆模板專案 (或創建新專案)
mkdir hybridsearch-es && cd hybridsearch-es

# 2. 啟動開發環境
docker-compose up -d postgres elasticsearch redis rabbitmq minio

# 3. 驗證服務狀態
docker-compose ps
```

## 🎯 第一天 - 核心架構 (8小時)

### ⏰ 上午 (09:00-13:00)

#### 09:00-09:30 專案初始化
```bash
# 使用 Spring Initializr 或複製提供的 build.gradle.kts
curl https://start.spring.io/starter.zip \
  -d dependencies=webflux,data-r2dbc,data-elasticsearch,amqp,data-redis,actuator \
  -d type=gradle-project \
  -d language=kotlin \
  -d jvmVersion=21 \
  -d groupId=com.hybridsearch \
  -d artifactId=hybrid-search-es \
  -o hybridsearch.zip

unzip hybridsearch.zip
```

#### 09:30-10:00 專案結構搭建
直接複製提供的專案結構：
```
src/main/kotlin/com/hybridsearch/
├── HybridSearchApplication.kt
├── config/         # 配置類
├── controller/     # REST 控制器
├── service/        # 業務邏輯
├── repository/     # 數據訪問層
├── model/          # 數據模型
├── handler/        # 消息處理器
└── util/           # 工具類
```

#### 10:00-11:00 基礎配置
1. 複製 `application.yml` 配置文件
2. 複製所有 `config/*.kt` 配置類
3. 測試應用程式啟動：`./gradlew bootRun`

#### 11:00-12:00 數據模型定義
1. 複製 `model/FinewebData.kt`
2. 複製 `model/SearchRequest.kt`  
3. 複製 `model/ExportTask.kt`
4. 運行數據庫初始化腳本

#### 12:00-13:00 Repository 層實現
1. 複製 `repository/r2dbc/FinewebDataRepository.kt`
2. 複製 `repository/elasticsearch/FinewebElasticsearchRepository.kt`
3. 測試數據庫連接

### ⏰ 下午 (14:00-18:00)

#### 14:00-16:00 Elasticsearch 整合
1. 複製 `config/ElasticsearchConfig.kt`
2. 建立 Elasticsearch 索引映射
3. 測試基礎搜索功能
4. 導入測試數據

#### 16:00-17:00 Redis 快取服務
1. 複製 `config/RedisConfig.kt`
2. 實現快取策略
3. 測試快取功能

#### 17:00-18:00 RabbitMQ 配置
1. 複製 `config/RabbitMQConfig.kt`
2. 配置 Queue 和 Exchange
3. 基礎消息發送測試

### 🎯 第一天檢查點
- [x] 應用程式可正常啟動
- [x] 所有依賴服務連接正常
- [x] 基礎數據模型已定義
- [x] Repository 層基本功能運作
- [x] 快取和消息隊列配置完成

## 🎯 第二天 - 業務邏輯與部署 (8小時)

### ⏰ 上午 (09:00-13:00)

#### 09:00-11:00 搜索服務實現
1. 複製 `service/SearchService.kt`
2. 實現混合檢索邏輯
3. 添加分頁和排序功能
4. 測試搜索 API

#### 11:00-12:30 檔案匯出服務
1. 複製 `service/ExportService.kt`
2. 複製 `service/MinioService.kt`
3. 複製 `util/CompressionUtil.kt`
4. 實現 Streaming 壓縮

#### 12:30-13:00 WebFlux Controller
1. 複製 `controller/SearchController.kt`
2. 測試所有 API 端點
3. 添加錯誤處理

### ⏰ 下午 (14:00-18:00)

#### 14:00-15:00 消息處理器
1. 複製 `handler/ExportTaskHandler.kt`
2. 測試異步任務處理
3. 端到端匯出功能測試

#### 15:00-16:00 基礎測試
```bash
# 功能測試
curl -X POST http://localhost:8080/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{"query":"test","page":0,"size":20}'

# 匯出測試
curl -X POST http://localhost:8080/api/v1/export \
  -H "Content-Type: application/json" \
  -d '{"query":"test","page":0,"size":100}'

# 健康檢查
curl http://localhost:8080/actuator/health
```

#### 16:00-17:00 容器化
1. 複製 `Dockerfile`
2. 構建 Docker 鏡像：
```bash
docker build -t hybridsearch/hybridsearch-es:latest .
```
3. 測試容器運行

#### 17:00-18:00 Kubernetes 部署
1. 複製所有 `k8s/*.yaml` 文件
2. 執行部署腳本：
```bash
chmod +x deploy.sh
./deploy.sh
```
3. 驗證部署狀態

### 🎯 第二天檢查點
- [x] 所有 API 端點正常運作
- [x] 搜索功能支援分頁和過濾
- [x] 檔案匯出和下載功能完整
- [x] Docker 容器正常運行
- [x] Kubernetes 部署成功
- [x] 基礎監控指標可查看

## 🛠️ 開發技巧和最佳實踐

### 時間管理策略
1. **嚴格按時間分配**：每個任務都有明確時間限制
2. **優先核心功能**：先實現 MVP，再優化細節
3. **並行開發**：配置和代碼同時進行
4. **快速驗證**：每完成一個模組立即測試

### 常見問題解決
```bash
# 依賴衝突解決
./gradlew dependencies --configuration runtimeClasspath

# 數據庫連接問題
docker logs hybridsearch-postgres

# Elasticsearch 連接問題
curl http://localhost:9200/_cluster/health

# Redis 連接測試
docker exec -it hybridsearch-redis redis-cli ping

# RabbitMQ 管理界面
open http://localhost:15672
```

### 除錯快速指令
```bash
# 查看應用程式日誌
./gradlew bootRun --debug

# 檢查 Docker 服務狀態
docker-compose logs -f [service-name]

# Kubernetes 除錯
kubectl describe pod -n hybridsearch
kubectl logs -f deployment/hybridsearch-app -n hybridsearch

# 監控系統資源
docker stats
kubectl top pods -n hybridsearch
```

## 📊 性能基準測試

### 快速性能驗證
```bash
# 基礎負載測試 (需要安裝 apache2-utils)
ab -n 1000 -c 10 -H "Content-Type: application/json" \
   -p test-search.json http://localhost:8080/api/v1/search

# 記憶體使用檢查
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# 資料庫連接池狀態
curl http://localhost:8080/actuator/metrics/r2dbc.pool.acquired
```

### 預期性能指標
- **搜索 QPS**: 500-1000 (單實例)
- **回應時間**: < 100ms (P95)
- **記憶體使用**: < 1GB (正常負載)
- **檔案匯出**: 支援 50MB 檔案

## 🔍 驗收標準

### 功能驗收
- [x] 支援關鍵字搜索和分頁
- [x] 支援結果匯出為 ZIP 檔案
- [x] 支援異步任務處理
- [x] 提供下載連結生成
- [x] 健康檢查端點正常

### 非功能驗收
- [x] 系統可在 Kubernetes 運行
- [x] 支援水平擴展 (HPA)
- [x] 監控指標可觀測
- [x] 日誌聚合正常
- [x] 基礎安全配置

### 部署驗收
```bash
# 驗證所有服務運行
kubectl get pods -n hybridsearch --show-labels

# 驗證 API 可訪問
curl -k https://api.hybridsearch.example.com/actuator/health

# 驗證監控指標
curl -k https://api.hybridsearch.example.com/actuator/prometheus
```

## 🚀 後續擴展建議

### 第三天可增強功能
1. **向量搜索**：整合 embedding 模型
2. **API 文檔**：Swagger/OpenAPI 規範
3. **安全性**：JWT 認證和授權
4. **限流**：API Rate Limiting

### 第四天優化項目
1. **性能調優**：JVM 參數和連接池
2. **快取策略**：多層快取和預熱
3. **監控告警**：Grafana Dashboard
4. **自動化**：CI/CD Pipeline

### 生產就緒檢查清單
- [ ] 安全掃描 (依賴和容器)
- [ ] 壓力測試 (負載和容量)
- [ ] 災難恢復計劃
- [ ] 數據備份策略
- [ ] 安全配置審查
- [ ] 運營監控手冊

## 📚 參考資料

### 官方文檔
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [Elasticsearch Java Client](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

### 社群資源
- [Spring Boot GitHub Examples](https://github.com/spring-projects/spring-boot/tree/main/spring-boot-samples)
- [Reactive Spring](https://spring.io/reactive)
- [Testcontainers Documentation](https://www.testcontainers.org/)

---

## 🎉 恭喜完成！

如果你嚴格按照這個計劃執行，你現在應該有一個：
- ✅ 功能完整的混合搜索系統
- ✅ 生產就緒的 Kubernetes 部署
- ✅ 可觀測的監控指標
- ✅ 可擴展的架構設計

**準備好展示你的成果了嗎？** 🚀