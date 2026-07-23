#!/usr/bin/env bash

LOADTEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "========================================================="
echo "🚀 loadtest 폴더 내 모든 k6 부하 테스트를 일괄 실행합니다."
echo "🎯 타겟 URL: ${BASE_URL}"
echo "========================================================="

for script in "${LOADTEST_DIR}"/*.js; do
    if [ -f "$script" ]; then
        FILENAME=$(basename "$script")
        echo ""
        echo "▶️ [시작] ${FILENAME} 실행 중..."
        echo "---------------------------------------------------------"
        k6 run -e BASE_URL="${BASE_URL}" "$script"
        echo "---------------------------------------------------------"
        sleep 2
    fi
done

echo ""
echo "========================================================="
echo "✅ 모든 부하 테스트 실행이 완료되었습니다."
echo "========================================================="
