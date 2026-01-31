package com.example.prototype_ocr.data

// Sealed class to represent different types of items in the RecyclerView
sealed class RecordListItem {
    data class MonthHeader(
        val monthName: String, 
        val isExpanded: Boolean = true
    ) : RecordListItem()
    
    data class TestTypeHeader(
        val testType: TestType, 
        val monthName: String,
        val isExpanded: Boolean = true
    ) : RecordListItem()
    
    data class TestRecordItem(val record: TestRecord) : RecordListItem()
}