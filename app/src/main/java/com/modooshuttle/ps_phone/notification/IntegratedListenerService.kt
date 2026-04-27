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
    val type: String,              // "카카오톡" / "SMS" / "LMS" / "MMS"
    val sender: String,            // 발신자 이름
    val phone: String?,            // 전화번호 (SMS/LMS/MMS만 해당)
    val content: String,           // 메시지 내용
    val room: String,              // 방 이름 (카톡 단톡방이면 방 이름, 아니면 "개인")
    val packageName: String,       // 원본 앱 패키지명
    val timestamp: Long = System.currentTimeMillis(),
    val handler: String = ""       // 담당자 이름
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
    private lateinit var smsHandler: SmsMessageHandler

    private var lastMessageKey: String = ""
    private var lastMessageTime: Long = 0
    private val DUPLICATE_CHECK_WINDOW_MS = 1000L

    override fun onCreate() {
        super.onCreate()
        smsHandler = SmsMessageHandler(this)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val isKakaoEnabled = prefs.getBoolean("kakao_enabled", true)
        val isSmsEnabled = prefs.getBoolean("sms_enabled", true)

        val messageInfo = when {
            packageName == kakaotalkPackage && isKakaoEnabled -> kakaoHandler.extractMessage(sbn)
            packageName in smsPackages && isSmsEnabled -> smsHandler.extractMessage(sbn, packageName)
            else -> return
        } ?: return

        val handlerName = prefs.getString("handler_name", "") ?: ""
        val finalMessageInfo = messageInfo.copy(handler = handlerName)

        // 중복 메시지 필터링
        val currentKey = "${finalMessageInfo.type}|${finalMessageInfo.sender}|${finalMessageInfo.content}"
        val currentTime = System.currentTimeMillis()

        if (currentKey == lastMessageKey && (currentTime - lastMessageTime) < DUPLICATE_CHECK_WINDOW_MS) {
            Log.d(TAG, "[중복] 같은 메시지가 1초 내에 감지됨: ${finalMessageInfo.sender}")
            return
        }

        lastMessageKey = currentKey
        lastMessageTime = currentTime

        Log.d(TAG, "[${finalMessageInfo.type}] ${finalMessageInfo.sender}: ${finalMessageInfo.content}")

        repository.saveMessage(finalMessageInfo)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "알림 리스너 연결됨")
        startMessageTransportService()
        startCallRecordingService()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "알림 리스너 연결 해제")
        stopMessageTransportService()
        stopCallRecordingService()
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

    private fun startCallRecordingService() {
        // TODO: WorkManager 기반 S3 업로드 시스템 구현 후 활성화
        Log.d(TAG, "통화 녹음 감시는 아직 구현 중입니다. (WorkManager 대기)")
        return

        // val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        // val isCallRecordingEnabled = prefs.getBoolean("call_recording_enabled", false)
        //
        // if (!isCallRecordingEnabled) {
        //     Log.d(TAG, "통화 녹음 감시가 비활성화됨")
        //     return
        // }
        //
        // val intent = Intent(this, CallRecordingService::class.java)
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        //     startForegroundService(intent)
        // } else {
        //     startService(intent)
        // }
    }

    private fun stopCallRecordingService() {
        val intent = Intent(this, CallRecordingService::class.java)
        stopService(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 알림이 제거되었을 때 처리
    }
}
