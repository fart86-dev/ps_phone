package com.modooshuttle.ps_phone.notification.handler

import android.app.Notification
import android.service.notification.StatusBarNotification
import android.util.Log
import com.modooshuttle.ps_phone.notification.MessageInfo

class SmsMessageHandler {

    private val TAG = "SmsHandler"

    fun extractMessage(sbn: StatusBarNotification, packageName: String): MessageInfo? {
        val extras = sbn.notification.extras

        val sender = extras.getString(Notification.EXTRA_TITLE) ?: return null
        val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return null

        // title에서 전화번호 추출 (예: "010-1234-5678" 또는 "김철수 010-1234-5678")
        val phone = extractPhone(sender)

        Log.d(TAG, "[$sender] (${phone ?: "번호 없음"}): $content")

        return MessageInfo(
            type = "문자메시지",
            sender = sender,
            phone = phone,
            content = content,
            room = "개인",
            packageName = packageName
        )
    }

    private fun extractPhone(title: String): String? {
        // "010-1234-5678" 또는 "01012345678" 형태 추출
        val phoneRegex = Regex("""(\d{3}[-.]?\d{3,4}[-.]?\d{4})""")
        return phoneRegex.find(title)?.value
    }
}
