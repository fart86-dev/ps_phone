package com.modooshuttle.ps_phone.notification

import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.PutObjectRequest
import com.modooshuttle.ps_phone.BuildConfig
import java.io.File

class S3Service {

    private val TAG = "S3Service"
    private val s3Client: AmazonS3Client by lazy {
        val credentials = BasicAWSCredentials(
            BuildConfig.AWS_ACCESS_KEY_ID,
            BuildConfig.AWS_SECRET_ACCESS_KEY
        )
        AmazonS3Client(credentials).apply {
            setRegion(Region.getRegion(Regions.fromName(BuildConfig.AWS_S3_REGION)))
        }
    }

    fun uploadFileToS3(filePath: String, fileName: String, metadata: Map<String, String> = emptyMap()): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "파일이 존재하지 않음: $filePath")
                return false
            }

            Log.d(TAG, "S3 업로드 시작: $fileName (크기: ${file.length()} bytes)")

            val s3Key = "call_recordings/${System.currentTimeMillis()}_$fileName"
            val putRequest = PutObjectRequest(
                BuildConfig.AWS_S3_BUCKET_NAME,
                s3Key,
                file
            )

            if (metadata.isNotEmpty()) {
                val userMetadata = metadata.toMutableMap()
                putRequest.withMetadata(com.amazonaws.services.s3.model.ObjectMetadata().apply {
                    userMetadata.forEach { (key, value) ->
                        addUserMetadata(key, value)
                    }
                })
            }

            s3Client.putObject(putRequest)

            Log.d(TAG, "S3 업로드 완료: s3://${BuildConfig.AWS_S3_BUCKET_NAME}/$s3Key")
            true
        } catch (e: Exception) {
            Log.e(TAG, "S3 업로드 실패", e)
            false
        }
    }

    fun uploadRecordingWithMetadata(
        filePath: String,
        fileName: String,
        sender: String = "",
        duration: Long = 0,
        timestamp: Long = System.currentTimeMillis()
    ): Boolean {
        val metadata = mutableMapOf(
            "original_filename" to fileName,
            "timestamp" to timestamp.toString()
        )
        if (sender.isNotEmpty()) {
            metadata["sender"] = sender
        }
        if (duration > 0) {
            metadata["duration_ms"] to duration.toString()
        }

        return uploadFileToS3(filePath, fileName, metadata)
    }

    fun isS3ConfiguredProperly(): Boolean {
        val keyId = BuildConfig.AWS_ACCESS_KEY_ID
        val secretKey = BuildConfig.AWS_SECRET_ACCESS_KEY
        val bucketName = BuildConfig.AWS_S3_BUCKET_NAME
        val region = BuildConfig.AWS_S3_REGION

        return keyId.isNotEmpty() &&
                keyId != "YOUR_ACCESS_KEY_ID" &&
                secretKey.isNotEmpty() &&
                secretKey != "YOUR_SECRET_ACCESS_KEY" &&
                bucketName.isNotEmpty() &&
                region.isNotEmpty()
    }
}
