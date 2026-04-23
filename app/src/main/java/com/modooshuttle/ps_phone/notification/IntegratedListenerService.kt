package com.modooshuttle.ps_phone.notification


import android.app.Notification
import android.app.Person
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi

class IntegratedListenerService : NotificationListenerService() {

  private val TAG = "MsgListener"

  // 감시할 패키지 목록
  private val targetPackages = listOf(
    "com.kakao.talk",                 // 카카오톡
    "com.android.mms",                // 삼성/제조사 기본 문자
    "com.google.android.apps.messaging", // 구글 메시지
    "com.samsung.android.messaging"    // 최신 삼성 메시지
  )

  @RequiresApi(Build.VERSION_CODES.P)
  override fun onNotificationPosted(sbn: StatusBarNotification) {
    val packageName = sbn.packageName
    if (!targetPackages.contains(packageName)) return

    val extras = sbn.notification.extras
    val sender = extras.getString(Notification.EXTRA_TITLE) ?: "알 수 없는 발신자"
    val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
    val room = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "개인 메시지"
  var phone = ""
    when (packageName) {
      "com.kakao.talk" -> {
        // 카톡은 보통 EXTRA_TITLE이 보낸 사람 이름입니다.
        // 단, 단톡방은 '방이름 : 이름' 형태로 올 수 있으니 파싱이 필요합니다.
      }
      "com.android.mms", "com.google.android.apps.messaging", "com.samsung.android.messaging" -> {
        // SMS 앱의 경우 EXTRA_MESSAGING_PERSON 확인
        val person = extras.getParcelable<Person>(Notification.EXTRA_MESSAGING_PERSON)
        val phoneNumber = person?.uri  // tel:010...
//        EXTRA_MESSAGING_STYLE_USER  나
        phone = phoneNumber.toString()


        // 번호가 없다면 title에서 숫자만 추출하는 fallback 로직 검토
      }
    }


    val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)

    if (messages != null && messages.isNotEmpty()) {
      // 2. 가장 마지막(최신) 메시지를 확인합니다.
      val lastMessage = messages.last() as Bundle

      // 3. 그 메시지를 보낸 '사람'을 추출합니다.
      val senderPerson = lastMessage.getParcelable<Person>("sender_person")

      if (senderPerson != null) {
        val realSenderName = senderPerson.name
        val realSenderUri = senderPerson.uri // 여기가 진짜 상대방 번호!
        Log.d("SMS_LOG", "진짜 발신자: $realSenderName, 번호: $realSenderUri")
      }
    }


    // 서비스 유형 분류
    val type = if (packageName == "com.kakao.talk") "카카오톡" else "문자메시지"

    val txt = extras.toString()
    Log.d(TAG, "[$type] 발신: $sender, 내용: $content $phone")

    // 슬랙 전송 호출 (유형 정보를 추가로 보냄)
    sendToSlack(type, sender, content, room)
  }

  private fun sendToSlack(type: String, sender: String, message: String, room: String) {
    // 여기에 이전에 작성한 OkHttp 슬랙 전송 로직이 들어갑니다.
    // 슬랙 페이로드에 "text": "[$type] $sender: $message" 식으로 구성하면 구분하기 좋습니다.
  }
}