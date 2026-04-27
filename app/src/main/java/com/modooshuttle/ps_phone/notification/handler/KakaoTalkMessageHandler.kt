package com.modooshuttle.ps_phone.notification.handler

import android.app.Notification
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi
import com.modooshuttle.ps_phone.notification.MessageInfo

class KakaoTalkMessageHandler {

    private val TAG = "KakaoTalkHandler"

    @RequiresApi(Build.VERSION_CODES.P)
    fun extractMessage(sbn: StatusBarNotification): MessageInfo? {
        val extras = sbn.notification.extras

        val sender = extras.getString(Notification.EXTRA_TITLE) ?: return null
        val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return null

        logKakaoExtras(extras, sender)

        val isGroupConversation = extras.getBoolean("android.isGroupConversation", false)
        val room = if (isGroupConversation) {
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "그룹"
        } else {
            "개인"
        }

        Log.d(TAG, "[$sender] @ [$room]: $content")

        return MessageInfo(
            type = "카카오톡",
            sender = sender,
            phone = null,
            content = content,
            room = room,
            packageName = sbn.packageName
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun logKakaoExtras(extras: android.os.Bundle, sender: String) {
        Log.d(TAG, "=== Kakao Extras Debug ===")
        Log.d(TAG, "EXTRA_TITLE: $sender")
        Log.d(TAG, "isGroupConversation: ${extras.getBoolean("android.isGroupConversation")}")
        Log.d(TAG, "All extras keys: ${extras.keySet()}")
        for (key in extras.keySet()) {
            val value = extras.get(key)
            if (value != null && value !is ByteArray && !key.startsWith("android.people")) {
                Log.d(TAG, "  $key: $value")
            }
        }

        val messagingStyleUser = extras.getBundle("android.messagingStyleUser")
        if (messagingStyleUser != null) {
            Log.d(TAG, "messagingStyleUser keys: ${messagingStyleUser.keySet()}")
            Log.d(TAG, "  name: ${messagingStyleUser.getString("name")}")
            Log.d(TAG, "  uri: ${messagingStyleUser.getString("uri")}")
        }

        val messages = extras.getParcelableArray("android.messages")
        if (messages != null) {
            Log.d(TAG, "android.messages count: ${messages.size}")
            for ((i, msg) in messages.withIndex()) {
                val bundle = msg as? android.os.Bundle
                if (bundle != null) {
                    Log.d(TAG, "  Message[$i]:")
                    Log.d(TAG, "    text: ${bundle.getString("text")}")
                    Log.d(TAG, "    sender: ${bundle.getString("sender")}")
                    val person = bundle.getParcelable<android.app.Person>("sender_person")
                    if (person != null) {
                        Log.d(TAG, "    sender_person.name: ${person.name}")
                        Log.d(TAG, "    sender_person.uri: ${person.uri}")
                    }
                    Log.d(TAG, "    message keys: ${bundle.keySet()}")
                    val msgExtras = bundle.getBundle("extras")
                    if (msgExtras != null) {
                        Log.d(TAG, "    message extras keys: ${msgExtras.keySet()}")
                        for (key in msgExtras.keySet()) {
                            Log.d(TAG, "      $key: ${msgExtras.get(key)}")
                        }
                    }
                }
            }
        }

        Log.d(TAG, "=======================")
    }

}
