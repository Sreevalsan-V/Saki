package com.example.prototype_ocr.data

data class TestRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val testNumber: Int, // Per test type numbering (Test 1 Glucose, Test 1 Creatinine, etc.)
    val testType: TestType,
    val resultValue: Double?,
    val rawOcrText: String,
    val confidence: Float?,
    val validationTimestamp: Long,
    val imageFileName: String,
    val isValidResult: Boolean
)

enum class TestType(val displayName: String, val unit: String) {
    GLUCOSE("Glucose", "mg/dL"),
    CREATININE("Creatinine", "mg/dL"),
    CHOLESTEROL("Cholesterol", "mg/dL")
}