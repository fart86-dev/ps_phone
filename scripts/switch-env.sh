#!/bin/bash

# 환경 설정 전환 스크립트
# 사용: ./scripts/switch-env.sh development|production

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV=${1:-development}

if [ "$ENV" != "development" ] && [ "$ENV" != "production" ]; then
    echo "사용법: ./scripts/switch-env.sh [development|production]"
    exit 1
fi

ENV_FILE="$PROJECT_DIR/.env.$ENV"

if [ ! -f "$ENV_FILE" ]; then
    echo "❌ $ENV_FILE 파일이 없습니다"
    exit 1
fi

echo "=========================================="
echo "환경 설정 확인"
echo "=========================================="
echo ""
echo "📌 현재 환경: $ENV"
echo ""
echo "📋 .env.$ENV 구성:"
echo ""

while IFS='=' read -r key value; do
    if [ -z "$key" ] || [[ "$key" == \#* ]]; then
        continue
    fi

    # 값의 일부만 표시 (보안)
    if [ "$key" = "NOTION_API_TOKEN" ] || [ "$key" = "AWS_SECRET_ACCESS_KEY" ]; then
        display_value="${value:0:10}...${value: -5}"
    else
        display_value="$value"
    fi

    echo "   $key: $display_value"
done < "$ENV_FILE"

echo ""
echo "=========================================="
echo "✅ $ENV 환경이 선택되었습니다"
echo ""
echo "다음 단계:"
echo "  • Debug 빌드:   ./scripts/build-and-install-debug.sh"
echo "  • Release 빌드: ./scripts/build-and-install-release.sh"
echo ""
echo "=========================================="
