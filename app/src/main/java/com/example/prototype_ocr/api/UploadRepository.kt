package com.example.prototype_ocr.api

import android.graphics.Bitmap
import android.util.Log
import com.example.prototype_ocr.data.TestRecord
import com.example.prototype_ocr.data.Upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UploadRepository(private val api: MedicalOcrApi = ApiClient.api) {
    
    companion object {
        private const val TAG = "UploadRepository"
    }
    
    /**
     * Upload test records to server
     * 
     * @param upload The Upload object containing all metadata
     * @param testImages Map of TestRecord to their Bitmap images
     * @param pdfFile The PDF file to upload
     * @return Result with UploadResponse on success or Exception on failure
     */
    suspend fun uploadToServer(
        upload: Upload,
        testImages: Map<TestRecord, Bitmap>,
        pdfFile: File
    ): Result<UploadResponse> = withContext(Dispatchers.IO) {
        try {
            // Validate panel ID and user details
            if (!ApiUtils.validatePanelId(upload.panelId)) {
                Log.e(TAG, "Invalid panel ID: ${upload.panelId}")
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid panel ID from QR scan")
                )
            }
            
            // Log user details for debugging
            Log.d(TAG, "Upload validation - Panel: ${upload.panelId}")
            Log.d(TAG, "User ID: '${upload.userId}'")
            Log.d(TAG, "User Name: '${upload.userName}'")
            Log.d(TAG, "PHC Name: '${upload.phcName}'")
            Log.d(TAG, "Hub Name: '${upload.hubName}'")
            Log.d(TAG, "Block Name: '${upload.blockName}'")
            Log.d(TAG, "District Name: '${upload.districtName}'")
            
            if (!ApiUtils.validateUserDetails(
                    upload.userId,
                    upload.userName,
                    upload.phcName,
                    upload.hubName,
                    upload.blockName,
                    upload.districtName
                )) {
                // Build detailed error message
                val missing = mutableListOf<String>()
                if (upload.userId.isBlank()) missing.add("User ID")
                if (upload.userName.isBlank()) missing.add("User Name")
                if (upload.phcName.isBlank()) missing.add("PHC Name")
                if (upload.hubName.isBlank()) missing.add("Hub Name")
                if (upload.blockName.isBlank()) missing.add("Block Name")
                if (upload.districtName.isBlank()) missing.add("District Name")
                
                val errorMsg = "Missing fields: ${missing.joinToString(", ")}"
                Log.e(TAG, errorMsg)
                return@withContext Result.failure(
                    IllegalArgumentException("Missing user details. Please ensure your profile is complete. $errorMsg")
                )
            }
            
            // Get all test records with test names assigned
            val allRecords = upload.getAllRecordsWithTestNames()
            
            // Validate we have 1-3 tests
            if (allRecords.isEmpty() || allRecords.size > 3) {
                return@withContext Result.failure(
                    IllegalArgumentException("Must have 1-3 test records, got ${allRecords.size}")
                )
            }
            
            // Build upload info
            val uploadInfo = UploadInfo(
                id = upload.id,
                timestamp = upload.uploadTimestamp,
                latitude = upload.latitude,
                longitude = upload.longitude,
                panelId = upload.panelId,
                userId = upload.userId,
                userName = upload.userName,
                phcName = upload.phcName,
                hubName = upload.hubName,
                blockName = upload.blockName,
                districtName = upload.districtName,
                monthName = upload.monthName
            )
            
            // Build test info list
            val testInfoList = allRecords.mapNotNull { testRecord ->
                // Get the bitmap for this test
                val bitmap = testImages[testRecord]
                if (bitmap == null) {
                    Log.w(TAG, "No image found for test ${testRecord.id}")
                    return@mapNotNull null
                }
                
                // Convert bitmap to base64
                val imageBase64 = ApiUtils.bitmapToBase64(bitmap, quality = 85)
                
                TestInfo(
                    id = testRecord.id,
                    type = testRecord.testType.name, // "GLUCOSE", "CREATININE", "CHOLESTEROL"
                    value = testRecord.resultValue,
                    unit = testRecord.testType.unit,
                    timestamp = testRecord.validationTimestamp,
                    latitude = testRecord.latitude,
                    longitude = testRecord.longitude,
                    confidence = testRecord.confidence,
                    rawText = testRecord.rawOcrText,
                    imageBase64 = imageBase64,
                    imageType = "jpeg"
                )
            }
            
            // Validate test types (no duplicates)
            if (!ApiUtils.validateTestTypes(testInfoList)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Cannot have duplicate test types in one upload")
                )
            }
            
            // Read and encode PDF
            if (!pdfFile.exists()) {
                return@withContext Result.failure(
                    IllegalArgumentException("PDF file not found: ${pdfFile.absolutePath}")
                )
            }
            
            val pdfBytes = pdfFile.readBytes()
            val pdfBase64 = ApiUtils.pdfToBase64(pdfBytes)
            
            // Create request
            val request = UploadRequest(
                upload = uploadInfo,
                tests = testInfoList,
                pdfBase64 = pdfBase64
            )
            
            Log.d(TAG, "Uploading to server: ${testInfoList.size} tests, user ${upload.userName}")
            
            // Make API call
            val response = api.uploadTestRecords(request)
            
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse != null && apiResponse.success && apiResponse.data != null) {
                    Log.d(TAG, "Upload successful: ${apiResponse.data.uploadId}")
                    Result.success(apiResponse.data)
                } else {
                    val errorMsg = apiResponse?.message ?: "Unknown error"
                    Log.e(TAG, "Upload failed: $errorMsg")
                    Result.failure(Exception("Server error: $errorMsg"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Log.e(TAG, "HTTP ${response.code()}: $errorMsg")
                Result.failure(Exception("HTTP ${response.code()}: $errorMsg"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception", e)
            Result.failure(e)
        }
    }
    
    /**
     * Simplified upload function that takes individual parameters
     */
    suspend fun uploadTestRecords(
        panelId: String,
        userId: String,
        userName: String,
        phcName: String,
        hubName: String,
        blockName: String,
        districtName: String,
        currentLatitude: Double?,
        currentLongitude: Double?,
        testRecords: List<TestRecord>,
        testImages: Map<TestRecord, Bitmap>,
        pdfBytes: ByteArray,
        monthName: String = ApiUtils.getMonthName(System.currentTimeMillis())
    ): Result<UploadResponse> = withContext(Dispatchers.IO) {
        try {
            // Create temporary upload object
            val uploadTimestamp = System.currentTimeMillis()
            
            val upload = Upload(
                id = ApiUtils.generateUUID(),
                uploadTimestamp = uploadTimestamp,
                monthName = monthName,
                panelId = panelId,
                userId = userId,
                userName = userName,
                phcName = phcName,
                hubName = hubName,
                blockName = blockName,
                districtName = districtName,
                glucoseRecord = testRecords.find { it.testType.name == "GLUCOSE" },
                creatinineRecord = testRecords.find { it.testType.name == "CREATININE" },
                cholesterolRecord = testRecords.find { it.testType.name == "CHOLESTEROL" },
                pdfFileName = "upload_$uploadTimestamp.pdf",
                latitude = currentLatitude,
                longitude = currentLongitude
            )
            
            // Create temporary PDF file
            val tempPdfFile = File.createTempFile("upload_", ".pdf")
            tempPdfFile.writeBytes(pdfBytes)
            
            val result = uploadToServer(upload, testImages, tempPdfFile)
            
            // Clean up temp file
            tempPdfFile.delete()
            
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Upload preparation failed", e)
            Result.failure(e)
        }
    }
}
