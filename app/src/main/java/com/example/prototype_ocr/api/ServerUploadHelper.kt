package com.example.prototype_ocr.api

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.example.prototype_ocr.data.Upload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object ServerUploadHelper {
    
    private const val TAG = "ServerUploadHelper"
    
    /**
     * Upload to server with progress dialog
     * 
     * @param context Activity context
     * @param upload The upload object to send
     * @param imagesDir Directory containing LCD images
     * @param pdfFile PDF file to upload
     * @param onSuccess Callback on successful upload
     * @param onFailure Callback on failure
     */
    fun uploadToServer(
        context: Context,
        upload: Upload,
        imagesDir: File,
        pdfFile: File,
        onSuccess: (UploadResponse) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val progressDialog = ProgressDialog(context).apply {
            setMessage("Uploading to server...")
            setCancelable(false)
            show()
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Load images for all test records
                val testImages = withContext(Dispatchers.IO) {
                    val recordsWithNames = upload.getAllRecordsWithTestNames()
                    val imageMap = mutableMapOf<com.example.prototype_ocr.data.TestRecord, android.graphics.Bitmap>()
                    
                    for (record in recordsWithNames) {
                        val imageFile = File(imagesDir, record.imageFileName)
                        if (imageFile.exists()) {
                            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                            if (bitmap != null) {
                                imageMap[record] = bitmap
                            } else {
                                Log.w(TAG, "Failed to decode image: ${record.imageFileName}")
                            }
                        } else {
                            Log.w(TAG, "Image file not found: ${record.imageFileName}")
                        }
                    }
                    imageMap
                }
                
                // Check if we have images for all tests
                val recordsCount = upload.getAllRecordsWithTestNames().size
                if (testImages.size != recordsCount) {
                    progressDialog.dismiss()
                    onFailure("Missing images: found ${testImages.size} of $recordsCount")
                    return@launch
                }
                
                // Upload to server
                val repository = UploadRepository()
                val result = repository.uploadToServer(upload, testImages, pdfFile)
                
                progressDialog.dismiss()
                
                result.onSuccess { response ->
                    onSuccess(response)
                }.onFailure { error ->
                    onFailure(error.message ?: "Upload failed")
                }
                
            } catch (e: Exception) {
                progressDialog.dismiss()
                Log.e(TAG, "Upload error", e)
                onFailure("Upload error: ${e.message}")
            }
        }
    }
    
    /**
     * Show upload confirmation dialog with option to upload to server
     */
    fun showUploadDialog(
        context: Context,
        upload: Upload,
        imagesDir: File,
        pdfFile: File,
        onLocalSaveOnly: () -> Unit,
        onServerUploadSuccess: (UploadResponse) -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("Upload Options")
            .setMessage("Do you want to upload this data to the server?\n\nDevice: ${upload.deviceId}\nTests: ${upload.getRecordCount()}")
            .setPositiveButton("Upload to Server") { _, _ ->
                uploadToServer(
                    context = context,
                    upload = upload,
                    imagesDir = imagesDir,
                    pdfFile = pdfFile,
                    onSuccess = { response ->
                        AlertDialog.Builder(context)
                            .setTitle("Upload Successful")
                            .setMessage("Upload ID: ${response.uploadId}\nDevice: ${response.deviceId}\nTests: ${response.testsCount}\n\nData saved locally and on server.")
                            .setPositiveButton("OK") { _, _ ->
                                onServerUploadSuccess(response)
                            }
                            .show()
                    },
                    onFailure = { error ->
                        AlertDialog.Builder(context)
                            .setTitle("Upload Failed")
                            .setMessage("Could not upload to server:\n\n$error\n\nData has been saved locally only.")
                            .setPositiveButton("OK") { _, _ ->
                                onLocalSaveOnly()
                            }
                            .show()
                    }
                )
            }
            .setNegativeButton("Save Locally Only") { _, _ ->
                onLocalSaveOnly()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }
}
