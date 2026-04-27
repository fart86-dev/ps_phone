package com.modooshuttle.ps_phone.notification.handler

import android.Manifest
import android.app.Notification
import android.app.Person
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.modooshuttle.ps_phone.notification.MessageInfo

class SmsMessageHandler(private val context: Context) {

    private val TAG = "SmsHandler"
    private val systemNotificationSenders = setOf(
        "기기 페어링"
    )

    fun extractMessage(sbn: StatusBarNotification, packageName: String): MessageInfo? {
        val extras = sbn.notification.extras

        val sender = extras.getString(Notification.EXTRA_TITLE) ?: return null
        if (sender in systemNotificationSenders) {
            Log.d(TAG, "[필터됨] 시스템 알림: $sender")
            return null
        }

        if (!isSmsMessage(extras)) {
            return null
        }

        val content = extractContent(extras) ?: return null

        val phone = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            extractPhoneFromAndroidMessages(extras)
                ?: extractPhoneFromPerson(extras)
        } else {
            null
        } ?: extractPhoneFromTitle(sender)

        val messageType = determineMessageType(extras, content)

        if (sender.contains("제목") || content.contains("제목")) {
            Log.w(TAG, "[경고] 제목 관련 메시지 감지 - sender:[$sender], content:[$content], type:[$messageType], package:[$packageName]")
            logExtrasDebug(extras, packageName)
        }

        if (phone == null) {
            Log.w(TAG, "[전화번호 미추출] [$messageType] [$sender] - android.messages: ${extras.getParcelableArray("android.messages") != null}")
        } else {
            Log.d(TAG, "[$messageType] [$sender] ($phone): ${content.take(50)}")
        }

        return MessageInfo(
            type = messageType,
            sender = sender,
            phone = phone,
            content = content,
            room = "개인",
            packageName = packageName
        )
    }

    private fun isSmsMessage(extras: Bundle): Boolean {
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return false
        val content = extractContent(extras) ?: return false
        return true
    }

    private fun extractContent(extras: Bundle): String? {
        return extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
    }

    private fun determineMessageType(extras: Bundle, content: String): String {
        val hasAndroidMessages = extras.getParcelableArray("android.messages") != null
        val usesBigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT) != null

        return when {
            usesBigText -> "MMS"
            hasAndroidMessages && content.length > 160 -> "LMS"
            hasAndroidMessages -> "SMS"
            else -> "MMS"
        }
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val person = sms.getParcelable<Person>("sender_person")
                        Log.d(TAG, "    sender_person.name: ${person?.name}")
                        Log.d(TAG, "    sender_person.uri: ${person?.uri}")
                        Log.d(TAG, "    sender_person.icon: ${person?.icon}")
                    }
                    Log.d(TAG, "    timestamp: ${sms.getLong("timestamp", -1)}")
                }
            }
        } else {
            Log.d(TAG, "android.messages: 없음")
        }
        Log.d(TAG, "====================")
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun extractPhoneFromAndroidMessages(extras: Bundle): String? {
        val messages = extras.getParcelableArray("android.messages")
        if (messages != null) {
            for (message in messages) {
                val sms = message as? Bundle
                if (sms != null) {
                    val person = sms.getParcelable<Person>("sender_person")
                    if (person != null && person.uri != null) {
                        extractPhoneFromUri(person.uri.toString())?.let { return it }
                    }
                }
            }
        }
        return null
    }

    private fun extractPhoneFromUri(lookupUri: String): String? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CONTACTS 권한 없음")
            return null
        }

        return try {
            val uri = android.net.Uri.parse(lookupUri)
            val contactUri = ContactsContract.Contacts.lookupContact(context.contentResolver, uri)
            if (contactUri != null) {
                queryPhoneNumber(contactUri)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "연락처 조회 실패: $lookupUri", e)
            null
        }
    }

    private fun queryPhoneNumber(contactUri: android.net.Uri): String? {
        return try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactUri.lastPathSegment),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val number = it.getString(0)
                    Log.d(TAG, "연락처에서 조회한 번호: $number")
                    number
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "전화번호 조회 실패", e)
            null
        }
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
