package com.example.prototype_ocr.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RecordsManager(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("test_records", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val RECORDS_KEY = "records_array"
        private const val NEXT_TEST_NUMBER_GLUCOSE_KEY = "next_test_number_glucose"
        private const val NEXT_TEST_NUMBER_CREATININE_KEY = "next_test_number_creatinine" 
        private const val NEXT_TEST_NUMBER_CHOLESTEROL_KEY = "next_test_number_cholesterol"
    }
    
    /**
     * Get all test records
     */
    fun getAllRecords(): List<TestRecord> {
        val json = sharedPreferences.getString(RECORDS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<TestRecord>>() {}.type
        return gson.fromJson(json, type)
    }
    
    /**
     * Get next test number for specific test type (auto-incrementing per type)
     */
    fun getNextTestNumber(testType: TestType): Int {
        val key = when (testType) {
            TestType.GLUCOSE -> NEXT_TEST_NUMBER_GLUCOSE_KEY
            TestType.CREATININE -> NEXT_TEST_NUMBER_CREATININE_KEY  
            TestType.CHOLESTEROL -> NEXT_TEST_NUMBER_CHOLESTEROL_KEY
        }
        return sharedPreferences.getInt(key, 1)
    }
    
    /**
     * Save a new test record
     */
    fun saveRecord(record: TestRecord): Boolean {
        return try {
            val currentRecords = getAllRecords().toMutableList()
            currentRecords.add(record)
            
            // Save updated records list
            val json = gson.toJson(currentRecords)
            
            // Update test number for the specific test type
            val nextNumberKey = when (record.testType) {
                TestType.GLUCOSE -> NEXT_TEST_NUMBER_GLUCOSE_KEY
                TestType.CREATININE -> NEXT_TEST_NUMBER_CREATININE_KEY
                TestType.CHOLESTEROL -> NEXT_TEST_NUMBER_CHOLESTEROL_KEY
            }
            
            sharedPreferences.edit()
                .putString(RECORDS_KEY, json)
                .putInt(nextNumberKey, record.testNumber + 1)
                .apply()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get record by ID
     */
    fun getRecordById(id: String): TestRecord? {
        return getAllRecords().find { it.id == id }
    }
    
    /**
     * Delete record by ID
     */
    fun deleteRecord(id: String): Boolean {
        return try {
            val currentRecords = getAllRecords().toMutableList()
            val recordToDelete = currentRecords.find { it.id == id }
            
            if (recordToDelete != null) {
                currentRecords.remove(recordToDelete)
                
                // Save updated records list
                val json = gson.toJson(currentRecords)
                sharedPreferences.edit()
                    .putString(RECORDS_KEY, json)
                    .apply()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get records sorted by timestamp (newest first)
     */
    fun getRecordsSortedByTime(): List<TestRecord> {
        return getAllRecords().sortedByDescending { it.validationTimestamp }
    }
    
    /**
     * Get records filtered by test type
     */
    fun getRecordsByType(testType: TestType): List<TestRecord> {
        return getAllRecords().filter { it.testType == testType }
    }
    
    /**
     * Get records organized by month and then by test type
     * Returns a map where key is month string (e.g., "January 2026") 
     * and value is another map organized by TestType
     */
    fun getRecordsGroupedByMonthAndType(): Map<String, Map<TestType, List<TestRecord>>> {
        val records = getRecordsSortedByTime()
        val monthFormat = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
        
        return records.groupBy { record ->
            monthFormat.format(java.util.Date(record.validationTimestamp))
        }.mapValues { (_, monthRecords) ->
            monthRecords.groupBy { it.testType }.mapValues { (_, typeRecords) ->
                typeRecords.sortedBy { it.testNumber }
            }
        }
    }
}