#!/bin/bash

# 測試配置腳本
echo "Testing Spring Boot configuration files..."

# 測試開發環境
echo "Testing DEV environment..."
cd /e/hybrid-search-es
./gradlew bootRun --args='--spring.profiles.active=dev --spring.main.web-application-type=none' &
DEV_PID=$!
sleep 10
kill $DEV_PID 2>/dev/null

echo "DEV test completed"

# 測試生產環境配置語法
echo "Testing PROD environment configuration syntax..."
./gradlew bootRun --args='--spring.profiles.active=prod --spring.main.web-application-type=none --spring.config.location=classpath:application.yml,classpath:application-prod.yaml' &
PROD_PID=$!
sleep 10
kill $PROD_PID 2>/dev/null

echo "PROD test completed"
echo "Configuration test finished"
