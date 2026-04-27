package com.modooshuttle.ps_phone.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.database.Cursor
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class CallRecordingService : Service() {

    private val TAG = "CallRecordingService"
    private val CHANNEL_ID = "call_recording_channel"
    private lateinit var recordingObserver: RecordingObserver

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CallRecordingService 생성")
        createNotificationChannel()
        recordingObserver = RecordingObserver(Handler(mainLooper))
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            recordingObserver
        )
        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "CallRecordingService 시작")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "CallRecordingService 종료")
        try {
            contentResolver.unregisterContentObserver(recordingObserver)
        } catch (e: Exception) {
            Log.e(TAG, "ContentObserver 등록 해제 실패", e)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "통화 녹음 감시",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForeground() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("통화 녹음 감시 중")
            .setContentText("녹음 파일을 감시하고 있습니다")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                1002,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(1002, notification)
        }
    }

    private inner class RecordingObserver(handler: Handler) : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            Log.d(TAG, "미디어 DB 변경 감지")
            checkNewRecordings()
        }

        private fun checkNewRecordings() {
            try {
                val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.DURATION
                )
                val selection = "${MediaStore.Audio.Media.DATE_ADDED} > ?"
                val cutoffTime = (System.currentTimeMillis() / 1000) - 5
                val selectionArgs = arrayOf(cutoffTime.toString())
                val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC LIMIT 1"

                val cursor: Cursor? = contentResolver.query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )

                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                        val dataIndex = it.getColumnIndex(MediaStore.Audio.Media.DATA)
                        val dateIndex = it.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                        val durationIndex = it.getColumnIndex(MediaStore.Audio.Media.DURATION)

                        val fileName = it.getString(nameIndex)
                        val filePath = it.getString(dataIndex)
                        val dateAdded = it.getLong(dateIndex)
                        val duration = it.getLong(durationIndex)

                        if (isCallRecording(filePath)) {
                            Log.d(TAG, "[녹음 감지] 파일명: $fileName")
                            Log.d(TAG, "[녹음 감지] 경로: $filePath")
                            Log.d(TAG, "[녹음 감지] 시간: $dateAdded (${dateAdded * 1000})")
                            Log.d(TAG, "[녹음 감지] 지속시간: ${duration}ms")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "녹음 파일 조회 실패", e)
            }
        }

        private fun isCallRecording(filePath: String?): Boolean {
            if (filePath == null) return false
            return filePath.contains("Call", ignoreCase = true) ||
                    filePath.contains("Recording", ignoreCase = true) ||
                    filePath.contains("Voice Memo", ignoreCase = true)
        }
    }
}
