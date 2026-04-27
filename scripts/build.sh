#!/bin/bash

# 통합 빌드 및 설치 스크립트
# 사용: ./scripts/build.sh [debug|release]

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_TYPE=${1:-debug}

if [ "$BUILD_TYPE" != "debug" ] && [ "$BUILD_TYPE" != "release" ]; then
    echo "사용법: ./scripts/build.sh [debug|release]"
    echo ""
    echo "옵션:"
    echo "  debug   - Debug 빌드 및 설치 (.env.development 사용)"
    echo "  release - Release 빌드 및 설치 (.env.production 사용)"
    exit 1
fi

echo "=========================================="
echo "$BUILD_TYPE 빌드 시작"
echo "=========================================="

# 환경 확인
if [ "$BUILD_TYPE" = "debug" ]; then
    ENV_FILE="$PROJECT_DIR/.env.development"
    SCRIPT="$PROJECT_DIR/scripts/build-and-install-debug.sh"
else
    ENV_FILE="$PROJECT_DIR/.env.production"
    SCRIPT="$PROJECT_DIR/scripts/build-and-install-release.sh"
fi

if [ ! -f "$ENV_FILE" ]; then
    echo "❌ $ENV_FILE 파일이 없습니다"
    exit 1
fi

echo ""
echo "✅ 환경 파일: $ENV_FILE"
echo "✅ 실행: $SCRIPT"
echo ""

# 스크립트 실행
bash "$SCRIPT"
