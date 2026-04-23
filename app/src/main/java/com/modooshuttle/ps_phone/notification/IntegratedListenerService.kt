package com.modooshuttle.ps_phone.notification

import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi
import com.modooshuttle.ps_phone.notification.handler.KakaoTalkMessageHandler
import com.modooshuttle.ps_phone.notification.handler.SmsMessageHandler

data class MessageInfo(
    val type: String,              // "카카오톡" or "문자메시지"
    val sender: String,            // 발신자 이름
    val phone: String?,            // 전화번호 (문자메시지만 해당)
    val content: String,           // 메시지 내용
    val room: String,              // 방 이름 (카톡 단톡방이면 방 이름, 아니면 "개인")
    val packageName: String,       // 원본 앱 패키지명
    val timestamp: Long = System.currentTimeMillis()
)

class IntegratedListenerService : NotificationListenerService() {

    private val TAG = "IntegratedListener"
    private val repository = MessageRepository()

    private val kakaotalkPackage = "com.kakao.talk"
    private val smsPackages = listOf(
        "com.android.mms",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging"
    )

    private val kakaoHandler = KakaoTalkMessageHandler()
    private val smsHandler = SmsMessageHandler()

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        val messageInfo = when {
            packageName == kakaotalkPackage -> kakaoHandler.extractMessage(sbn)
            packageName in smsPackages -> smsHandler.extractMessage(sbn, packageName)
            else -> return
        } ?: return

        Log.d(TAG, "[${messageInfo.type}] ${messageInfo.sender}: ${messageInfo.content}")

        repository.saveMessage(messageInfo)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "알림 리스너 연결됨")
        startMessageTransportService()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "알림 리스너 연결 해제")
        stopMessageTransportService()
    }

    private fun startMessageTransportService() {
        val intent = Intent(this, MessageTransportService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopMessageTransportService() {
        val intent = Intent(this, MessageTransportService::class.java)
        stopService(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 알림이 제거되었을 때 처리
    }
}
