#!/bin/bash

# Dev 환경 Debug 설치 스크립트
# 사용: ./scripts/install-dev-debug.sh

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/dev/debug/app-dev-debug.apk"
PACKAGE_NAME="com.modooshuttle.ps_phone.dev"

echo "=========================================="
echo "Dev 환경 Debug APK 설치"
echo "=========================================="
echo ""

# 빌드
echo "🔨 빌드 진행 중..."
cd "$PROJECT_DIR"
./gradlew assembleDevDebug

if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK 생성 실패"
    exit 1
fi

APK_SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
echo "✅ APK 생성 완료: $APK_SIZE"

# ADB 확인
echo ""
echo "📱 연결된 기기 확인..."
if ! command -v adb &> /dev/null; then
    echo "❌ adb를 찾을 수 없습니다"
    exit 1
fi

DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)
if [ $DEVICES -eq 0 ]; then
    echo "❌ 연결된 기기가 없습니다"
    exit 1
fi

echo "✅ 기기 발견: $DEVICES개"

# 설치
echo ""
echo "📦 APK 설치 진행 중..."
adb install -r "$APK_PATH"

echo ""
echo "✅ 설치 완료!"
echo ""
echo "앱 실행:"
echo "  adb shell am start -n $PACKAGE_NAME/.MainActivity"
echo ""
echo "로그 확인:"
echo "  adb logcat | grep 'IntegratedListener\\|KakaoTalk\\|SMS'"
echo ""
echo "=========================================="
