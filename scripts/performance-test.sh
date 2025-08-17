#!/bin/bash
# scripts/performance-test.sh

# 基礎搜索性能測試
echo "Testing search performance..."

# 使用 ab (Apache Bench) 進行簡單負載測試
ab -n 1000 -c 10 -H "Content-Type: application/json" \
   -p test-data/search-request.json \
   http://localhost:8080/api/v1/search

# 使用 curl 測試各個端點
echo "Testing health endpoint..."
curl -w "@curl-format.txt" -s -o /dev/null http://localhost:8080/api/v1/health

echo "Testing search endpoint..."
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"query":"test","page":0,"size":20}' \
  -w "@curl-format.txt" \
  -s -o /dev/null \
  http://localhost:8080/api/v1/search

echo "Performance tests completed!"