package com.example.prototype_ocr.api

import com.google.gson.annotations.SerializedName

/**
 * Request models for server upload
 */
data class UploadRequest(
    val upload: UploadInfo,
    val tests: List<TestInfo>,
    val pdfBase64: String
)

data class UploadInfo(
    val id: String,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val deviceId: String,
    val monthName: String
)

data class TestInfo(
    val id: String,
    val type: String, // "GLUCOSE", "CREATININE", "CHOLESTEROL"
    val value: Double?,
    val unit: String,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val confidence: Float?,
    val rawText: String,
    val imageBase64: String,
    val imageType: String // "jpeg" or "png"
)

/**
 * Response models from server
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val timestamp: Long
)

data class UploadResponse(
    val uploadId: String,
    val deviceId: String,
    val uploadTime: String,
    val uploadLocation: LocationData?,
    val pdfUrl: String,
    val testsCount: Int,
    val tests: List<TestResponse>
)

data class TestResponse(
    val id: String,
    val type: String,
    val displayName: String,
    val value: Double?,
    val unit: String,
    val testTime: String,
    val confidence: Float?,
    val imageUrl: String,
    val testLocation: LocationData?
)

data class LocationData(
    val latitude: Double,
    val longitude: Double
)
