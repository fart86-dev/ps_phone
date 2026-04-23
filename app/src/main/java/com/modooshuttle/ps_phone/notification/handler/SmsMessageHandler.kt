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
