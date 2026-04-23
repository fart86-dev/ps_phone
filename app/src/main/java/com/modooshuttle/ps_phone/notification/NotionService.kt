package com.modooshuttle.ps_phone.notification

import android.util.Log
import com.modooshuttle.ps_phone.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class NotionService {

    private val TAG = "NotionService"
    private val client = OkHttpClient()

    private val notionApiUrl = "https://api.notion.com/v1/pages"
    private val databaseId = BuildConfig.NOTION_DATABASE_ID
    private val notionToken = BuildConfig.NOTION_API_TOKEN

    fun saveMessageToNotion(message: MessageInfo) {
        try {
            val payload = createPayload(message)
            val request = createRequest(payload)

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Log.d(TAG, "Notion 저장 성공")
            } else {
                Log.e(TAG, "Notion 저장 실패: ${response.code} - ${response.body?.string()}")
            }
            response.close()
        } catch (e: Exception) {
            Log.e(TAG, "Notion 저장 중 오류", e)
        }
    }

    private fun createPayload(message: MessageInfo): JSONObject {
        val properties = JSONObject()

        // 이름 (발신자)
        properties.put("이름", JSONObject().apply {
            val titleArray = org.json.JSONArray()
            titleArray.put(JSONObject().put("text", JSONObject().put("content", message.sender)))
            put("title", titleArray)
        })

        // 타입
        properties.put("타입", JSONObject().apply {
            val richTextArray = org.json.JSONArray()
            richTextArray.put(JSONObject().put("text", JSONObject().put("content", message.type)))
            put("rich_text", richTextArray)
        })

        // 전화 (문자메시지만 해당)
        if (message.phone != null) {
            properties.put("전화", JSONObject().apply {
                val richTextArray = org.json.JSONArray()
                richTextArray.put(JSONObject().put("text", JSONObject().put("content", message.phone)))
                put("rich_text", richTextArray)
            })
        }

        // 내용
        properties.put("내용", JSONObject().apply {
            val richTextArray = org.json.JSONArray()
            richTextArray.put(JSONObject().put("text", JSONObject().put("content", message.content)))
            put("rich_text", richTextArray)
        })

        // 날짜 (KST - 타임존 포함)
        val kstDate = convertToKST(message.timestamp)
        properties.put("날짜", JSONObject().apply {
            put("date", JSONObject().put("start", "$kstDate+09:00"))
        })

        return JSONObject().apply {
            put("parent", JSONObject().put("database_id", databaseId))
            put("properties", properties)
        }
    }

    private fun createRequest(payload: JSONObject): Request {
        val mediaType = "application/json".toMediaType()
        val body = payload.toString().toRequestBody(mediaType)

        return Request.Builder()
            .url(notionApiUrl)
            .addHeader("Authorization", "Bearer $notionToken")
            .addHeader("Notion-Version", "2022-06-28")
            .post(body)
            .build()
    }

    private fun convertToKST(timestamp: Long): String {
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").apply {
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }
        val result = sdf.format(date)
        Log.d(TAG, "원본 timestamp: $timestamp, 변환 결과: $result")
        return result
    }
}
