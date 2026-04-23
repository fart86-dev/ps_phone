package com.modooshuttle.ps_phone.notification

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MessageRepository {

    private val TAG = "MessageRepository"
    private val notionService = NotionService()

    fun saveMessage(message: MessageInfo) {
        Log.d(TAG, "메시지 저장: $message")

        when (message.type) {
            "카카오톡" -> logKakaoMessage(message)
            "문자메시지" -> logSmsMessage(message)
        }

        // Notion에 비동기로 저장
        saveToNotion(message)
    }

    private fun logKakaoMessage(message: MessageInfo) {
        Log.i(
            TAG,
            "카카오톡 수신 | 발신자: ${message.sender} | 방: ${message.room} | 내용: ${message.content}"
        )
    }

    private fun logSmsMessage(message: MessageInfo) {
        Log.i(
            TAG,
            "문자메시지 수신 | 발신자: ${message.sender} | 번호: ${message.phone} | 내용: ${message.content}"
        )
    }

    private fun saveToNotion(message: MessageInfo) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                notionService.saveMessageToNotion(message)
            } catch (e: Exception) {
                Log.e(TAG, "Notion 저장 실패", e)
            }
        }
    }
}
