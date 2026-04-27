# 법인폰 메시지 수집 앱 (PS Phone)

회사 내부용 Android 앱으로, KakaoTalk, SMS/LMS/MMS 메시지를 자동으로 감지하여 Notion 데이터베이스에 저장합니다.

## 🎯 주요 기능

- ✅ **KakaoTalk 메시지** - 그룹톡/개인톡 자동 감지
- ✅ **SMS/LMS/MMS 메시지** - 문자메시지 자동 분류 및 저장
- ✅ **담당자 관리** - 메시지별 담당자 정보 저장
- ✅ **Notion 연동** - 모든 메시지를 Notion DB에 자동 저장
- ⏳ **통화 녹음 감시** - 녹음 파일 감지 (WorkManager 구현 대기)

## 📋 요구사항

### 개발 환경
- **Android Studio** 2024.1 이상
- **Android SDK** API 24 이상
- **Java 11** 이상
- **Gradle** 8.13.2 이상

### 배포 환경
- **Android 6.0** (API 24) 이상
- **4GB RAM** 이상

### 필수 권한
```xml
- INTERNET (네트워크 통신)
- READ_CONTACTS (연락처 조회)
- READ_EXTERNAL_STORAGE (파일 접근)
- READ_MEDIA_AUDIO (오디오 파일 접근)
- POST_NOTIFICATIONS (알림)
- FOREGROUND_SERVICE (백그라운드 서비스)
```

## 🔧 설정

### 1. 환경 변수 설정

#### Development 환경 (.env.development)
```bash
NOTION_API_TOKEN=your_dev_notion_token
NOTION_DATABASE_ID=your_dev_database_id
AWS_ACCESS_KEY_ID=your_dev_aws_key
AWS_SECRET_ACCESS_KEY=your_dev_aws_secret
AWS_S3_BUCKET_NAME=ps-phone-recordings-dev
AWS_S3_REGION=ap-northeast-2
```

#### Production 환경 (.env.production)
```bash
NOTION_API_TOKEN=your_prod_notion_token
NOTION_DATABASE_ID=your_prod_database_id
AWS_ACCESS_KEY_ID=your_prod_aws_key
AWS_SECRET_ACCESS_KEY=your_prod_aws_secret
AWS_S3_BUCKET_NAME=ps-phone-recordings
AWS_S3_REGION=ap-northeast-2
```

### 2. 참고 사항
- `.env.development`, `.env.production`, `.env.backup`은 git에 추적되지 않습니다 (.gitignore)
- `.env.example`을 참고하여 새로운 환경 파일을 만드세요
- Notion API Token: https://www.notion.so/my-integrations
- AWS S3: AWS IAM 콘솔에서 자격증명 발급

## 🚀 빌드 및 설치

### 빠른 시작 (스크립트 사용)

#### Debug 빌드 (개발/테스트)
```bash
./scripts/build.sh debug
# 또는
./scripts/build-and-install-debug.sh
```

#### Release 빌드 (배포)
```bash
./scripts/build.sh release
# 또는
./scripts/build-and-install-release.sh
```

#### 환경 확인
```bash
./scripts/switch-env.sh development
./scripts/switch-env.sh production
```

### 수동 빌드

#### Debug 빌드
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### Release 빌드
```bash
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release-unsigned.apk
```

### 기기 연결 확인
```bash
adb devices
```

## 📱 앱 실행

### 방법 1: 스크립트 (권장)
```bash
# 자동으로 설치 후 실행 안내
./scripts/build.sh debug
```

### 방법 2: ADB
```bash
adb shell am start -n com.modooshuttle.ps_phone/.MainActivity
```

### 방법 3: 기기에서 직접
앱 목록에서 "법인폰 메시지 수집"을 찾아 실행

## 📊 앱 설정

앱 실행 후 설정 화면에서:
- **카카오톡**: ON/OFF (수신 메시지 저장 여부)
- **문자메시지**: ON/OFF (SMS/LMS/MMS 저장 여부)
- **담당자 이름**: 메시지에 포함될 담당자 정보 입력
- **저장**: 설정 저장

## 🔍 로그 확인

### Logcat 실시간 모니터링
```bash
# 모든 로그
adb logcat

# 특정 태그만 필터링
adb logcat | grep -E "IntegratedListener|KakaoTalk|SMS|Notion"

# 앱 재시작 후 로그 보기
adb logcat --clear
adb logcat | grep "ps_phone"
```

### 주요 로그 태그
- `IntegratedListener` - 알림 리스너 상태
- `KakaoTalkHandler` - 카카오톡 메시지 처리
- `SmsMessageHandler` - SMS/LMS/MMS 처리
- `NotionService` - Notion API 저장
- `CallRecordingService` - 통화 녹음 감지

## 📂 프로젝트 구조

```
ps_phone/
├── app/
│   ├── src/main/
│   │   ├── java/com/modooshuttle/ps_phone/
│   │   │   ├── MainActivity.kt              # 설정 UI
│   │   │   ├── notification/
│   │   │   │   ├── IntegratedListenerService.kt   # 알림 수신
│   │   │   │   ├── CallRecordingService.kt        # 녹음 파일 감시
│   │   │   │   ├── MessageTransportService.kt     # 메시지 전송
│   │   │   │   ├── MessageRepository.kt           # 데이터 관리
│   │   │   │   ├── NotionService.kt               # Notion API
│   │   │   │   ├── S3Service.kt                   # S3 업로드
│   │   │   │   └── handler/
│   │   │   │       ├── KakaoTalkMessageHandler.kt
│   │   │   │       └── SmsMessageHandler.kt
│   │   │   └── ui/theme/
│   │   ├── AndroidManifest.xml
│   │   └── res/
│   ├── build.gradle.kts
│   └── ...
├── gradle/
│   └── libs.versions.toml
├── scripts/
│   ├── build.sh                      # 통합 빌드 스크립트
│   ├── build-and-install-debug.sh    # Debug 빌드 및 설치
│   ├── build-and-install-release.sh  # Release 빌드 및 설치
│   └── switch-env.sh                 # 환경 설정 확인
├── .env.development                  # 개발 환경 변수 (git 제외)
├── .env.production                   # 배포 환경 변수 (git 제외)
├── .env.example                      # 환경 변수 템플릿
├── .env.backup                       # 이전 값 보존 (git 제외)
├── gradle.properties                 # Gradle 설정
└── README.md                         # 이 문서
```

## 📡 동작 흐름

### 메시지 수신 및 저장
```
NotificationListenerService
    ↓
onNotificationPosted() 수신
    ↓
KakaoTalkMessageHandler / SmsMessageHandler (메시지 파싱)
    ↓
MessageRepository (로컬 저장)
    ↓
NotionService (Notion API 저장)
```

### 통화 녹음 감시 (구현 대기)
```
IntegratedListenerService
    ↓
CallRecordingService (MediaStore 감시)
    ↓
파일 생성 감지
    ↓
WorkManager (안정적 업로드)
    ↓
S3Service (S3 업로드)
```

## 🐛 트러블슈팅

### ADB 연결 안 됨
```bash
# ADB 서버 재시작
adb kill-server
adb start-server

# 기기 확인
adb devices
```

### 메시지가 저장되지 않음
```bash
# 1. 알림 리스너 권한 확인
# 설정 → 앱 → 특수 앱 접근 권한 → 알림 접근 권한 → 법인폰 메시지 수집 ON

# 2. 각 메시지 타입의 권한 확인
# 설정 → 앱 → 법인폰 메시지 수집 → 권한 → 필요 권한 모두 활성화

# 3. Notion 자격증명 확인
adb logcat | grep Notion
```

### Notion 저장 실패
```bash
# 로그 확인
adb logcat | grep "NotionService"

# 확인 사항:
# - NOTION_API_TOKEN이 유효한가?
# - NOTION_DATABASE_ID가 맞는가?
# - 네트워크 연결이 정상인가?
```

### APK 설치 오류
```bash
# 기존 앱 제거 후 재설치
adb uninstall com.modooshuttle.ps_phone
./scripts/build.sh debug
```

## 📚 기술 스택

| 분야 | 스택 |
|------|------|
| **언어** | Kotlin |
| **UI** | Jetpack Compose |
| **백그라운드** | NotificationListenerService, ContentObserver, Foreground Service |
| **HTTP** | OkHttp3 |
| **JSON** | org.json |
| **AWS** | AWS SDK for Android (S3) |
| **빌드** | Gradle 8.13.2 |
| **대상** | Android 6.0+ (API 24+) |

## 📝 주요 커밋 이력

- `260424_1` - 초기 구성
- `260424_2` - KakaoTalk, SMS/LMS/MMS 분류 및 중복 필터링
- `260424_3` - ON/OFF 설정 및 담당자 관리
- `260424_4` - 통화 녹음 설정 및 라이브러리 업그레이드
- `260424_5` - S3 업로드 기능 구현 (미연결)
- `260424_6` - 환경변수 기반 설정 관리로 전환
- `260424_7` - .env 파일 기반 환경별 관리
- `260424_8` - 통화 녹음 기능 일시 비활성화

## ⏳ 진행 중인 작업

- 🔨 **WorkManager 구현** - S3 업로드 안정화 (대용량 파일, 네트워크 오류 대응)
- 🔨 **통화 녹음 S3 연결** - WorkManager 완료 후 활성화

## 📞 문의 및 피드백

버그 리포트 또는 기능 요청은 프로젝트 이슈에 남겨주세요.

---

**마지막 업데이트**: 2026-04-27  
**버전**: 1.0 (개발 중)
