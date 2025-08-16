# HybridSearch-ES Kubernetes 部署配置

## 1. Namespace 配置

```yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: hybridsearch
  labels:
    name: hybridsearch
    environment: production
```

## 2. ConfigMap 配置

```yaml
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: hybridsearch-config
  namespace: hybridsearch
data:
  application.yml: |
    server:
      port: 8080
    spring:
      application:
        name: hybrid-search-es
      r2dbc:
        url: r2dbc:postgresql://postgres-service:5432/hybridsearch
        username: postgres
        password: ${POSTGRES_PASSWORD}
        pool:
          initial-size: 10
          max-size: 50
          max-idle-time: 30m
      elasticsearch:
        uris: http://elasticsearch-service:9200
        username: elastic
        password: ${ELASTICSEARCH_PASSWORD}
      data:
        redis:
          host: redis-service
          port: 6379
          password: ${REDIS_PASSWORD}
          database: 0
      rabbitmq:
        host: rabbitmq-service
        port: 5672
        username: guest
        password: ${RABBITMQ_PASSWORD}
        virtual-host: /
      cache:
        type: redis
        redis:
          time-to-live: 600000
    minio:
      endpoint: http://minio-service:9000
      access-key: ${MINIO_ACCESS_KEY}
      secret-key: ${MINIO_SECRET_KEY}
      bucket-name: hybridsearch-exports
    app:
      search:
        max-results: 10000
        default-page-size: 20
        max-page-size: 100
      export:
        max-file-size: 1GB
        compression-level: 6
        temp-dir: /tmp/exports
    logging:
      level:
        com.hybridsearch: INFO
        org.springframework.data.elasticsearch: WARN
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

## 3. Secret 配置

```yaml
# k8s/secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: hybridsearch-secrets
  namespace: hybridsearch
type: Opaque
data:
  # Base64 encoded passwords
  POSTGRES_PASSWORD: cGFzc3dvcmQ=  # password
  ELASTICSEARCH_PASSWORD: cGFzc3dvcmQ=  # password
  REDIS_PASSWORD: cGFzc3dvcmQ=  # password
  RABBITMQ_PASSWORD: Z3Vlc3Q=  # guest
  MINIO_ACCESS_KEY: bWluaW9hZG1pbg==  # minioadmin
  MINIO_SECRET_KEY: bWluaW9hZG1pbg==  # minioadmin
```

## 4. PostgreSQL 部署

```yaml
# k8s/postgres.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: hybridsearch
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
  storageClassName: fast-ssd  # 根據集群調整

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: hybridsearch
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        env:
        - name: POSTGRES_DB
          value: hybridsearch
        - name: POSTGRES_USER
          value: postgres
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: hybridsearch-secrets
              key: POSTGRES_PASSWORD
        ports:
        - containerPort: 5432
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        - name: init-scripts
          mountPath: /docker-entrypoint-initdb.d
        livenessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - postgres
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - postgres
          initialDelaySeconds: 5
          periodSeconds: 5
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc
      - name: init-scripts
        configMap:
          name: postgres-init-script

---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: hybridsearch
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
  type: ClusterIP

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres-init-script
  namespace: hybridsearch
data:
  init.sql: |
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

    CREATE INDEX IF NOT EXISTS idx_fineweb_data_lang ON fineweb_data(language);
    CREATE INDEX IF NOT EXISTS idx_fineweb_data_date ON fineweb_data(date);
    CREATE INDEX IF NOT EXISTS idx_fineweb_data_metadata ON fineweb_data USING gin (metadata);
    CREATE INDEX IF NOT EXISTS idx_fineweb_data_text ON fineweb_data USING gin (to_tsvector('english', text));
```

## 5. Elasticsearch 部署

```yaml
# k8s/elasticsearch.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: elasticsearch-pvc
  namespace: hybridsearch
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 20Gi
  storageClassName: fast-ssd

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: elasticsearch
  namespace: hybridsearch
spec:
  replicas: 1
  selector:
    matchLabels:
      app: elasticsearch
  template:
    metadata:
      labels:
        app: elasticsearch
    spec:
      initContainers:
      - name: configure-sysctl
        image: busybox:1.35
        command: ['sh', '-c', 'sysctl -w vm.max_map_count=262144']
        securityContext:
          privileged: true
      containers:
      - name: elasticsearch
        image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
        env:
        - name: discovery.type
          value: single-node
        - name: xpack.security.enabled
          value: "false"
        - name: ES_JAVA_OPTS
          value: "-Xms2g -Xmx2g"
        ports:
        - containerPort: 9200
        - containerPort: 9300
        volumeMounts:
        - name: elasticsearch-storage
          mountPath: /usr/share/elasticsearch/data
        livenessProbe:
          httpGet:
            path: /_cluster/health
            port: 9200
          initialDelaySeconds: 120
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /_cluster/health
            port: 9200
          initialDelaySeconds: 60
          periodSeconds: 10
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
      volumes:
      - name: elasticsearch-storage
        persistentVolumeClaim:
          claimName: elasticsearch-pvc

---
apiVersion: v1
kind: Service
metadata:
  name: elasticsearch-service
  namespace: hybridsearch
spec:
  selector:
    app: elasticsearch
  ports:
  - name: http
    port: 9200
    targetPort: 9200
  - name: transport
    port: 9300
    targetPort: 9300
  type: ClusterIP
```

## 6. Redis 部署

```yaml
# k8s/redis.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: redis-pvc
  namespace: hybridsearch
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
  storageClassName: fast-ssd

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: hybridsearch
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        command:
        - redis-server
        - --requirepass
        - $(REDIS_PASSWORD)
        - --appendonly
        - "yes"
        env:
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: hybridsearch-secrets
              key: REDIS_PASSWORD
        ports:
        - containerPort: 6379
        volumeMounts:
        - name: redis-storage
          mountPath: /data
        livenessProbe:
          exec:
            command:
            - redis-cli
            - ping
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - redis-cli
            - ping
          initialDelaySeconds: 5
          periodSeconds: 5
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
      volumes:
      - name: redis-storage
        persistentVolumeClaim:
          claimName: redis-pvc

---
apiVersion: v1
kind: Service
metadata:
  name: redis-service
  namespace: hybridsearch
spec:
  selector:
    app: redis
  ports:
  - port: 6379
    targetPort: 6379
  type: ClusterIP
```

## 7. RabbitMQ 部署

```yaml
# k8s/rabbitmq.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: rabbitmq-pvc
  namespace: hybridsearch
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
  storageClassName: fast-ssd

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rabbitmq
  namespace: hybridsearch
spec:
  replicas: 1
  selector:
    matchLabels:
      app: rabbitmq
  template:
    metadata:
      labels:
        app: rabbitmq
    spec:
      containers:
      - name: rabbitmq
        image: rabbitmq:3-management-alpine
        env:
        - name: RABBITMQ_DEFAULT_USER
          value: guest
        - name: RABBITMQ_DEFAULT_PASS
          valueFrom:
            secretKeyRef:
              name: hybridsearch-secrets
              key: RABBITMQ_PASSWORD
        ports:
        - containerPort: 5672
        - containerPort: 15672
        volumeMounts:
        - name: rabbitmq-storage
          mountPath: /var/lib/rabbitmq
        livenessProbe:
          exec:
            command:
            - rabbitmq-diagnostics
            - ping
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          exec:
            command:
            - rabbitmq-diagnostics
            - ping
          initialDelaySeconds: 20
          periodSeconds: 10
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
      volumes:
      - name: rabbitmq-storage
        persistentVolumeClaim:
          claimName: rabbitmq-pvc

---
apiVersion: v1
kind: Service
metadata:
  name: rabbitmq-service
  namespace: hybridsearch
spec:
  selector:
    app: rabbitmq
  ports:
  - name: amqp
    port: 5672
    targetPort: 5672
  - name: management
    port: 15672
    targetPort: 15672
  type: ClusterIP
```

## 8. MinIO 部署

```yaml
# k8s/minio.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: minio-pvc
  namespace: hybridsearch
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 50Gi
  storageClassName: fast-ssd

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: minio
  namespace: hybridsearch
spec:
  replicas: 1
  selector:
    matchLabels:
      app: minio
  template:
    metadata:
      labels:
        app: minio
    spec:
      containers:
      - name: minio
        image: minio/minio:latest
        command:
        - /bin/bash
        - -c
        args:
        - minio server /data --console-address :9001
        env:
        - name: MINIO_ROOT_USER
          valueFrom:
            secretKeyRef:
              name: hybridsearch-secrets
              key: MINIO_ACCESS_KEY
        - name: MINIO_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: hybridsearch-secrets
              key: MINIO_SECRET_KEY
        ports:
        - containerPort: 9000
        - containerPort: 9001
        volumeMounts:
        - name: minio-storage
          mountPath: /data
        livenessProbe:
          httpGet:
            path: /minio/health/live
            port: 9000
          initialDelaySeconds: 120
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /minio/health/ready
            port: 9000
          initialDelaySeconds: 60
          periodSeconds: 10
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
      volumes:
      - name: minio-storage
        persistentVolumeClaim:
          claimName: minio-pvc

---
apiVersion: v1
kind: Service
metadata:
  name: minio-service
  namespace: hybridsearch
spec:
  selector:
    app: minio
  ports:
  - name: api
    port: 9000
    targetPort: 9000
  - name: console
    port: 9001
    targetPort: 9001
  type: ClusterIP
```

## 9. 應用程式部署

```yaml
# k8s/app.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: hybridsearch-app
  namespace: hybridsearch
  labels:
    app: hybridsearch-app
    version: v1
spec:
  replicas: 3
  selector:
    matchLabels:
      app: hybridsearch-app
  template:
    metadata:
      labels:
        app: hybridsearch-app
        version: v1
    spec:
      containers:
      - name: hybridsearch-app
        image: hybridsearch/hybridsearch-es:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: kubernetes
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: hybridsearch-secrets
              key: POSTGRES_PASSWORD
        - name: ELASTICSEARCH_PASSWORD
          valueFrom:
            secretKeyRef:
              name: hybridsearch-secrets
              key: ELASTICSEARCH_PASSWORD
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: hybridsearch-secrets
              key: REDIS_PASSWORD
        - name: RABBITMQ_PASSWORD
          valueFrom:
            secretKeyRef:
              name: hybridsearch-secrets
              key: RABBITMQ_PASSWORD
        - name: MINIO_ACCESS_KEY
          valueFrom:
            secretKeyRef:
              name: hybridsearch-secrets
              key: MINIO_ACCESS_KEY
        - name: MINIO_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: hybridsearch-secrets
              key: MINIO_SECRET_KEY
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
        - name: temp-storage
          mountPath: /tmp/exports
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 120
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
      volumes:
      - name: config-volume
        configMap:
          name: hybridsearch-config
      - name: temp-storage
        emptyDir:
          sizeLimit: 10Gi
      initContainers:
      - name: wait-for-postgres
        image: busybox:1.35
        command: ['sh', '-c', 'until nc -z postgres-service 5432; do echo waiting for postgres; sleep 2; done;']
      - name: wait-for-elasticsearch
        image: busybox:1.35
        command: ['sh', '-c', 'until nc -z elasticsearch-service 9200; do echo waiting for elasticsearch; sleep 2; done;']
      - name: wait-for-redis
        image: busybox:1.35
        command: ['sh', '-c', 'until nc -z redis-service 6379; do echo waiting for redis; sleep 2; done;']
      - name: wait-for-rabbitmq
        image: busybox:1.35
        command: ['sh', '-c', 'until nc -z rabbitmq-service 5672; do echo waiting for rabbitmq; sleep 2; done;']

---
apiVersion: v1
kind: Service
metadata:
  name: hybridsearch-service
  namespace: hybridsearch
  labels:
    app: hybridsearch-app
spec:
  selector:
    app: hybridsearch-app
  ports:
  - name: http
    port: 80
    targetPort: 8080
  type: ClusterIP

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: hybridsearch-hpa
  namespace: hybridsearch
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: hybridsearch-app
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

## 10. Ingress 配置

```yaml
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: hybridsearch-ingress
  namespace: hybridsearch
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "100m"
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - api.hybridsearch.example.com
    secretName: hybridsearch-tls
  rules:
  - host: api.hybridsearch.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: hybridsearch-service
            port:
              number: 80
```

## 11. 監控配置

```yaml
# k8s/monitoring.yaml
apiVersion: v1
kind: ServiceMonitor
metadata:
  name: hybridsearch-metrics
  namespace: hybridsearch
  labels:
    app: hybridsearch-app
spec:
  selector:
    matchLabels:
      app: hybridsearch-app
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s

---
apiVersion: v1
kind: Service
metadata:
  name: hybridsearch-metrics
  namespace: hybridsearch
  labels:
    app: hybridsearch-app
spec:
  selector:
    app: hybridsearch-app
  ports:
  - name: metrics
    port: 8080
    targetPort: 8080
  type: ClusterIP
```

## 12. 部署腳本

```bash
#!/bin/bash
# deploy.sh

set -e

echo "開始部署 HybridSearch-ES 到 Kubernetes..."

# 創建 namespace
kubectl apply -f k8s/namespace.yaml

# 創建 secrets 和 configmaps
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/configmap.yaml

# 部署數據庫和中間件
echo "部署 PostgreSQL..."
kubectl apply -f k8s/postgres.yaml

echo "部署 Elasticsearch..."
kubectl apply -f k8s/elasticsearch.yaml

echo "部署 Redis..."
kubectl apply -f k8s/redis.yaml

echo "部署 RabbitMQ..."
kubectl apply -f k8s/rabbitmq.yaml

echo "部署 MinIO..."
kubectl apply -f k8s/minio.yaml

# 等待服務啟動
echo "等待服務啟動..."
kubectl wait --for=condition=ready pod -l app=postgres -n hybridsearch --timeout=300s
kubectl wait --for=condition=ready pod -l app=elasticsearch -n hybridsearch --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n hybridsearch --timeout=300s
kubectl wait --for=condition=ready pod -l app=rabbitmq -n hybridsearch --timeout=300s
kubectl wait --for=condition=ready pod -l app=minio -n hybridsearch --timeout=300s

# 部署應用程式
echo "部署應用程式..."
kubectl apply -f k8s/app.yaml

# 設置 ingress
echo "設置 Ingress..."
kubectl apply -f k8s/ingress.yaml

# 設置監控
echo "設置監控..."
kubectl apply -f k8s/monitoring.yaml

echo "部署完成！"
echo "檢查部署狀態："
kubectl get pods -n hybridsearch
kubectl get services -n hybridsearch
kubectl get ingress -n hybridsearch

echo "應用程式 URL: https://api.hybridsearch.example.com"
echo "健康檢查: https://api.hybridsearch.example.com/actuator/health"
```

這個 Kubernetes 配置提供了：

1. **完整的基礎設施** - PostgreSQL, Elasticsearch, Redis, RabbitMQ, MinIO
2. **高可用性** - 多副本部署和自動擴縮容
3. **存儲持久化** - 所有有狀態服務都配置了 PVC
4. **健康檢查** - Liveness 和 Readiness Probes
5. **安全配置** - Secrets 管理和 TLS 支援
6. **監控整合** - Prometheus metrics 導出
7. **負載平衡** - Ingress 和 HPA 配置
8. **初始化容器** - 確保依賴服務就緒
9. **資源限制** - CPU 和記憶體配置
10. **一鍵部署** - 自動化部署腳本