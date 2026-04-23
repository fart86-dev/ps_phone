package com.modooshuttle.ps_phone.notification.handler

import android.app.Notification
import android.service.notification.StatusBarNotification
import android.util.Log
import com.modooshuttle.ps_phone.notification.MessageInfo

class KakaoTalkMessageHandler {

    private val TAG = "KakaoTalkHandler"

    fun extractMessage(sbn: StatusBarNotification): MessageInfo? {
        val extras = sbn.notification.extras

        val sender = extras.getString(Notification.EXTRA_TITLE) ?: return null
        val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return null

        // 단톡방 처리: "방이름 : 이름" 형태
        val (room, actualSender) = parseTitle(sender)

        Log.d(TAG, "[$actualSender] @ [$room]: $content")

        return MessageInfo(
            type = "카카오톡",
            sender = actualSender,
            phone = null,
            content = content,
            room = room,
            packageName = sbn.packageName
        )
    }

    private fun parseTitle(title: String): Pair<String, String> {
        // "방이름 : 이름" 형태 체크
        return if (title.contains(" : ")) {
            val parts = title.split(" : ", limit = 2)
            Pair(parts[0], parts[1])
        } else {
            // 개인 메시지
            Pair("개인", title)
        }
    }
}
