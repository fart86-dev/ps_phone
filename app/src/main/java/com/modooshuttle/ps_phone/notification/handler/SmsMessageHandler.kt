package com.modooshuttle.ps_phone.notification.handler

import android.app.Notification
import android.app.Person
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi
import com.modooshuttle.ps_phone.notification.MessageInfo

class SmsMessageHandler {

    private val TAG = "SmsHandler"

    fun extractMessage(sbn: StatusBarNotification, packageName: String): MessageInfo? {
        val extras = sbn.notification.extras

        // 실제 SMS만 처리 - android.messages 필드 필수
        if (!isRealSmsMessage(extras)) {
            return null
        }

        val sender = extras.getString(Notification.EXTRA_TITLE) ?: return null
        val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return null

        val phone = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            extractPhoneFromPerson(extras)
        } else {
            null
        } ?: extractPhoneFromTitle(sender)

        if (sender.contains("제목") || content.contains("제목")) {
            Log.w(TAG, "[경고] 제목 관련 메시지 감지 - sender:[$sender], content:[$content], package:[$packageName]")
            logExtrasDebug(extras, packageName)
        }

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

    private fun logExtrasDebug(extras: Bundle, packageName: String) {
        Log.d(TAG, "=== Extras Debug Info ===")
        Log.d(TAG, "Package: $packageName")
        Log.d(TAG, "EXTRA_TITLE: ${extras.getString(Notification.EXTRA_TITLE)}")
        Log.d(TAG, "EXTRA_TEXT: ${extras.getCharSequence(Notification.EXTRA_TEXT)}")
        Log.d(TAG, "EXTRA_SUB_TEXT: ${extras.getCharSequence(Notification.EXTRA_SUB_TEXT)}")
        Log.d(TAG, "EXTRA_BIG_TEXT: ${extras.getCharSequence(Notification.EXTRA_BIG_TEXT)}")

        val messages = extras.getParcelableArray("android.messages")
        if (messages != null) {
            Log.d(TAG, "android.messages count: ${messages.size}")
            for ((i, message) in messages.withIndex()) {
                val sms = message as? Bundle
                if (sms != null) {
                    Log.d(TAG, "  Message[$i]:")
                    Log.d(TAG, "    text: ${sms.getString("text")}")
                    Log.d(TAG, "    sender_person: ${sms.getParcelable<Person>("sender_person")?.name}")
                    Log.d(TAG, "    timestamp: ${sms.getLong("timestamp", -1)}")
                }
            }
        }
        Log.d(TAG, "====================")
    }

    private fun isRealSmsMessage(extras: Bundle): Boolean {
        // 실제 SMS는 android.messages 배열을 가짐
        val messages = extras.getParcelableArray("android.messages")
        if (messages == null || messages.isEmpty()) {
            return false
        }

        // 각 메시지가 실제 SMS인지 확인
        for (message in messages) {
            val sms = message as? Bundle
            if (sms != null) {
                // 실제 SMS는 "sender_person" 또는 "text" 필드를 가짐
                val hasSenderPerson = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    sms.getParcelable<Person>("sender_person") != null
                val hasText = sms.getString("text") != null

                if (hasSenderPerson || hasText) {
                    return true
                }
            }
        }

        return false
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun extractPhoneFromPerson(extras: Bundle): String? {
        val messages = extras.getParcelableArray("android.messages")
        if (messages != null) {
            for (message in messages) {
                val sms = message as? Bundle
                val person = sms?.getParcelable<Person>("sender_person")
                if (person != null && person.uri != null) {
                    val uri = person.uri.toString()
                    val phonePattern = Regex("""(\d{3}[-.]?\d{3,4}[-.]?\d{4})""")
                    val match = phonePattern.find(uri)
                    if (match != null) {
                        return match.value
                    }
                }
            }
        }
        return null
    }

    private fun extractPhoneFromTitle(title: String): String? {
        val phoneRegex = Regex("""(\d{3}[-.]?\d{3,4}[-.]?\d{4})""")
        return phoneRegex.find(title)?.value
    }
}
