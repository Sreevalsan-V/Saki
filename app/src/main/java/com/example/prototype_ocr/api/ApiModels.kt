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
    // Panel/Device ID from QR scan
    val panelId: String,
    // User details from login
    val userId: String,
    val userName: String,
    val phcName: String,
    val hubName: String,
    val blockName: String,
    val districtName: String,
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
    val panelId: String,
    val userId: String,
    val userName: String,
    val phcName: String,
    @SerializedName("hubName") val hubName: String?,
    @SerializedName("blockName") val blockName: String?,
    @SerializedName("districtName") val districtName: String?,
    val uploadTime: String,
    @SerializedName("uploadTimestamp") val uploadTimestamp: Long?,
    @SerializedName("month") val month: String?,
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
    @SerializedName("testTimestamp") val testTimestamp: Long?,
    val confidence: Float?,
    @SerializedName("rawText") val rawText: String?,
    val imageUrl: String,
    val testLocation: LocationData?
)

data class LocationData(
    val latitude: Double,
    val longitude: Double
)

// ========== SERVER OCR MODELS ==========
// Note: Server OCR models are defined in ServerOcrRepository.kt

/**
 * Response from server OCR (legacy - kept for compatibility)
 */
data class ServerOcrResponse(
    val rawText: String,
    val value: Double?,
    val confidence: Float,
    val detections: Int
)

/**
 * Response from cropped OCR
 */
data class CroppedOcrResponse(
    val crops: List<CropOcrResult>
)

data class CropOcrResult(
    val name: String,
    val text: String,
    val value: Any?,     // Can be Double (mg/dL) or String (test type)
    val confidence: Float
)

/**
 * OCR service health check response
 */
data class OcrHealthResponse(
    val status: String,
    val service: String,
    val version: String
)
