# FCM 푸시 알림 설정 가이드

## 1. Firebase 프로젝트 설정

1. [Firebase Console](https://console.firebase.google.com)에 접속
2. 프로젝트 생성 또는 기존 프로젝트 선택
3. 프로젝트 설정 → "서비스 계정" 탭
4. `google-services.json` 다운로드
5. `app/google-services.json`으로 저장 (기존 파일 덮어쓰기)

## 2. 코드 변경 사항

### build.gradle.kts (프로젝트 레벨)
- Google Services 플러그인 추가

### app/build.gradle.kts
- Firebase 의존성 추가
- Google Services 플러그인 적용

### AndroidManifest.xml
- 푸시 알림 권한 추가
- PushNotificationService 등록

### PushNotificationService.kt (NEW)
- FCM 메시지 수신 처리
- 알림 표시 기능
- 토큰 관리

## 3. 현재 TODO 항목

`PushNotificationService.kt`에서:

```kotlin
// TODO: 서버에 토큰 전송
// FCM 토큰을 서버에 저장하는 로직 추가

// TODO: 메시지 데이터 처리 (서버에 저장 등)
// 수신한 메시지를 서버에 저장하는 로직 추가
```

## 4. 테스트

### Firebase Console에서 테스트
1. Cloud Messaging → "새 캠페인"
2. FCM 토큰 입력 (Logcat에서 확인 가능)
3. 메시지 작성 후 전송

### Logcat 확인
```
adb logcat | grep PushNotification
```

## 5. 주의사항

- `google-services.json`은 절대 커밋하면 안 됨 (`.gitignore` 확인)
- Firebase 토큰은 앱 설치 시마다 달라질 수 있음
- 백그라운드에서도 메시지 수신 가능
