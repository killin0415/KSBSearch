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