package com.example.prototype_ocr.api

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Server OCR Repository
 * Handles server-side OCR processing as removable module
 * Falls back to ML Kit if server unavailable
 */
class ServerOcrRepository(private val api: MedicalOcrApi) {
    
    companion object {
        private const val TAG = "ServerOcrRepository"
    }
    
    /**
     * Process full image with server OCR
     */
    suspend fun processFullImage(
        bitmap: Bitmap,
        token: String
    ): Result<ServerOcrResult> = withContext(Dispatchers.IO) {
        try {
            val imageBase64 = ApiUtils.bitmapToBase64(bitmap)
            
            val request = ServerOcrRequest(
                imageBase64 = imageBase64
            )
            
            val response = api.processServerOcr("Bearer $token", request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    Log.d(TAG, "Server OCR successful: ${data.text}")
                    Result.success(data)
                } else {
                    Result.failure(Exception("Empty server response"))
                }
            } else {
                val errorMsg = response.body()?.message ?: "Server OCR failed"
                Log.e(TAG, "Server OCR error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Server OCR exception", e)
            Result.failure(e)
        }
    }
    
    /**
     * Process cropped regions with server OCR
     */
    suspend fun processCroppedRegions(
        bitmap: Bitmap,
        cropRegions: List<CropRegionRequest>,
        token: String
    ): Result<ServerCroppedOcrResult> = withContext(Dispatchers.IO) {
        try {
            val imageBase64 = ApiUtils.bitmapToBase64(bitmap)
            
            val request = ServerCroppedOcrRequest(
                imageBase64 = imageBase64,
                cropRegions = cropRegions
            )
            
            val response = api.processServerCroppedOcr("Bearer $token", request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    Log.d(TAG, "Server cropped OCR successful: ${data.regions.size} regions")
                    Result.success(data)
                } else {
                    Result.failure(Exception("Empty server response"))
                }
            } else {
                val errorMsg = response.body()?.message ?: "Server cropped OCR failed"
                Log.e(TAG, "Server cropped OCR error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Server cropped OCR exception", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if server OCR is available
     */
    suspend fun checkServerOcrHealth(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = api.checkOcrHealth()
            
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                val isAvailable = data?.get("easyOcrAvailable") as? Boolean ?: false
                Result.success(isAvailable)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Server OCR health check failed", e)
            Result.success(false)
        }
    }
}

/**
 * Server OCR Request Models
 */
data class ServerOcrRequest(
    val imageBase64: String
)

data class ServerCroppedOcrRequest(
    val imageBase64: String,
    val cropRegions: List<CropRegionRequest>
)

data class CropRegionRequest(
    val name: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

/**
 * Server OCR Response Models
 */
data class ServerOcrResult(
    val success: Boolean,
    val text: String,
    val confidence: Float,
    val detections: List<OcrDetection>,
    val totalDetections: Int,
    val error: String? = null
)

data class ServerCroppedOcrResult(
    val regions: List<RegionOcrResult>
)

data class RegionOcrResult(
    val name: String,
    val region: RegionCoordinates,
    val success: Boolean,
    val text: String,
    val confidence: Float,
    val detections: List<OcrDetection>,
    val error: String? = null
)

data class RegionCoordinates(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

data class OcrDetection(
    val bbox: List<List<Float>>,
    val text: String,
    val confidence: Float
)
