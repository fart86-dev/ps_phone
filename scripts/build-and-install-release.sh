#!/bin/bash

# Release APK 빌드 및 설치 스크립트
# 사용: ./scripts/build-and-install-release.sh

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"
PACKAGE_NAME="com.modooshuttle.ps_phone"

echo "=========================================="
echo "Release APK 빌드 및 설치"
echo "=========================================="

# 1. 환경 변수 확인
echo ""
echo "📋 환경 변수 확인:"
if [ -f "$PROJECT_DIR/.env.production" ]; then
    echo "✅ .env.production 파일 발견"
    echo "   NOTION_API_TOKEN: $(grep NOTION_API_TOKEN $PROJECT_DIR/.env.production | cut -d'=' -f2 | head -c 20)..."
else
    echo "⚠️  .env.production 파일이 없습니다"
    exit 1
fi

# 2. Release 빌드
echo ""
echo "🔨 Release 빌드 진행 중..."
cd "$PROJECT_DIR"
./gradlew clean assembleRelease

if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK 생성 실패"
    exit 1
fi

APK_SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
echo "✅ APK 생성 완료: $APK_SIZE"

# 3. ADB 확인
echo ""
echo "📱 연결된 기기 확인..."
if ! command -v adb &> /dev/null; then
    echo "❌ adb를 찾을 수 없습니다"
    echo "   Android SDK가 설치되어 있는지 확인하세요"
    exit 1
fi

DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)
if [ $DEVICES -eq 0 ]; then
    echo "❌ 연결된 기기가 없습니다"
    echo "   USB 디버깅이 활성화된 Android 기기를 연결해주세요"
    exit 1
fi

echo "✅ 기기 발견: $DEVICES개"
adb devices | grep -v "List"

# 4. APK 설치
echo ""
echo "📦 APK 설치 진행 중..."
adb install -r "$APK_PATH"

# 5. 설치 확인
echo ""
echo "✅ 설치 완료!"
echo ""
echo "다음 명령으로 앱을 실행할 수 있습니다:"
echo "  adb shell am start -n $PACKAGE_NAME/.MainActivity"
echo ""
echo "또는 기기의 앱 목록에서 '법인폰 메시지 수집' 을 찾아 실행하세요"
echo ""
echo "=========================================="
