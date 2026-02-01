package com.example.prototype_ocr.api

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ApiUtils {
    
    /**
     * Convert Bitmap to Base64 JPEG string
     * @param bitmap The bitmap to convert
     * @param quality JPEG compression quality (0-100), default 85
     * @return Base64 encoded string WITHOUT data URI prefix
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
    
    /**
     * Convert PDF bytes to Base64 string
     * @param pdfBytes The PDF file bytes
     * @return Base64 encoded string WITHOUT data URI prefix
     */
    fun pdfToBase64(pdfBytes: ByteArray): String {
        return Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
    }
    
    /**
     * Get month name in "January 2026" format from timestamp
     * @param timestamp Milliseconds since epoch
     * @return Formatted month name like "January 2026"
     */
    fun getMonthName(timestamp: Long): String {
        val format = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        return format.format(Date(timestamp))
    }
    
    /**
     * Validate user details for upload
     * @return true if all required fields are non-blank
     */
    fun validateUserDetails(
        userId: String,
        userName: String,
        phcName: String,
        hubName: String,
        blockName: String,
        districtName: String
    ): Boolean {
        return userId.isNotBlank() &&
               userName.isNotBlank() &&
               phcName.isNotBlank() &&
               hubName.isNotBlank() &&
               blockName.isNotBlank() &&
               districtName.isNotBlank()
    }
    
    /**
     * Validate panel ID (from QR scan)
     */
    fun validatePanelId(panelId: String): Boolean {
        return panelId.isNotBlank()
    }
    
    /**
     * Generate UUID string
     */
    fun generateUUID(): String {
        return UUID.randomUUID().toString()
    }
    
    /**
     * Get formatted date time string
     * @param timestamp Milliseconds since epoch
     * @return Formatted string like "31 Jan 2026 at 2:30 PM"
     */
    fun getFormattedDateTime(timestamp: Long): String {
        val format = SimpleDateFormat("d MMM yyyy 'at' h:mm a", Locale.ENGLISH)
        return format.format(Date(timestamp))
    }
    
    /**
     * Validate that we have at most one test of each type
     */
    fun validateTestTypes(tests: List<TestInfo>): Boolean {
        val types = tests.map { it.type }
        return types.size == types.distinct().size
    }
    
    /**
     * Read PDF file to byte array
     */
    fun readPdfFile(pdfFile: java.io.File): ByteArray {
        return pdfFile.readBytes()
    }
}
