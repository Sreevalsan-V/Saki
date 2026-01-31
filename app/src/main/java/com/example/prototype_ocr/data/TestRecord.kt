package com.example.prototype_ocr.data

import java.text.SimpleDateFormat
import java.util.*

data class TestRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val testNumber: Int, // Per test type numbering (Test 1 Glucose, Test 1 Creatinine, etc.)
    val testType: TestType,
    val resultValue: Double?,
    val rawOcrText: String,
    val confidence: Float?,
    val validationTimestamp: Long,
    val imageFileName: String,
    val isValidResult: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null,
    var testName: String = "" // test-1, test-2, test-3 for upload ordering
) {
    fun getFormattedDateTime(): String {
        val format = SimpleDateFormat("d MMM yyyy 'at' h:mm a", Locale.getDefault())
        return format.format(Date(validationTimestamp))
    }
}

enum class TestType(val displayName: String, val unit: String) {
    GLUCOSE("Glucose", "mg/dL"),
    CREATININE("Creatinine", "mg/dL"),
    CHOLESTEROL("Cholesterol", "mg/dL")
}

data class Upload(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uploadTimestamp: Long,
    val monthName: String,
    val deviceId: String, // QR code scanned ID like DPHS-1, DPHS-2
    val glucoseRecord: TestRecord?,
    val creatinineRecord: TestRecord?,
    val cholesterolRecord: TestRecord?,
    val pdfFileName: String,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    fun getRecordCount(): Int {
        var count = 0
        if (glucoseRecord != null) count++
        if (creatinineRecord != null) count++
        if (cholesterolRecord != null) count++
        return count
    }
    
    fun getAllRecords(): List<TestRecord> {
        val records = mutableListOf<TestRecord>()
        glucoseRecord?.let { records.add(it) }
        creatinineRecord?.let { records.add(it) }
        cholesterolRecord?.let { records.add(it) }
        return records
    }
    
    fun getAllRecordsWithTestNames(): List<TestRecord> {
        val records = mutableListOf<TestRecord>()
        var index = 1
        glucoseRecord?.let { 
            it.testName = "test-$index"
            records.add(it)
            index++
        }
        creatinineRecord?.let { 
            it.testName = "test-$index"
            records.add(it)
            index++
        }
        cholesterolRecord?.let { 
            it.testName = "test-$index"
            records.add(it)
            index++
        }
        return records
    }
    
    fun getFormattedDateTime(): String {
        val format = SimpleDateFormat("d MMM yyyy 'at' h:mm a", Locale.getDefault())
        return format.format(Date(uploadTimestamp))
    }
}