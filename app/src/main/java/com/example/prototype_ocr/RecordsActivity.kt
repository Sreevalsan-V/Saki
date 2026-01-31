package com.example.prototype_ocr

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototype_ocr.data.RecordsManager
import com.example.prototype_ocr.data.RecordListItem
import com.example.prototype_ocr.data.TestType
import com.example.prototype_ocr.data.TestRecord
import java.io.File

class RecordsActivity : AppCompatActivity() {
    
    private lateinit var recordsManager: RecordsManager
    private lateinit var recordsAdapter: GroupedRecordsAdapter
    private lateinit var recordsRecyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    
    // State management for expand/collapse
    private val expandedMonths = mutableSetOf<String>()
    private val expandedTestTypes = mutableSetOf<String>() // "monthName_testTypeName"
    private var isInitialLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_records)
        
        // Initialize components
        recordsManager = RecordsManager(this)
        recordsRecyclerView = findViewById(R.id.recordsRecyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)
        
        // Setup RecyclerView
        val imageDir = File(filesDir, "lcd_images")
        recordsAdapter = GroupedRecordsAdapter(
            items = emptyList(),
            imageDir = imageDir,
            onItemClick = { testRecord ->
                // Open detail view
                val intent = Intent(this, RecordDetailActivity::class.java)
                intent.putExtra("record_id", testRecord.id)
                startActivity(intent)
            },
            onMonthHeaderClick = { monthName ->
                toggleMonthExpansion(monthName)
            },
            onTestTypeHeaderClick = { monthName, testTypeName ->
                toggleTestTypeExpansion(monthName, testTypeName)
            }
        )
        
        recordsRecyclerView.layoutManager = LinearLayoutManager(this)
        recordsRecyclerView.adapter = recordsAdapter
        
        // Load records
        loadRecords()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh records when returning to this activity
        loadRecords()
    }
    
    private fun loadRecords() {
        val groupedRecords = recordsManager.getRecordsGroupedByMonthAndType()
        
        if (groupedRecords.isEmpty()) {
            // Show empty state
            recordsRecyclerView.visibility = View.GONE
            emptyStateText.visibility = View.VISIBLE
        } else {
            // Show records
            recordsRecyclerView.visibility = View.VISIBLE
            emptyStateText.visibility = View.GONE
            
            // Initialize all months as expanded on first load
            if (isInitialLoad) {
                expandedMonths.addAll(groupedRecords.keys)
                // Initialize all test types as expanded
                groupedRecords.forEach { (monthName, testTypeMap) ->
                    testTypeMap.keys.forEach { testType ->
                        expandedTestTypes.add("${monthName}_${testType.displayName}")
                    }
                }
                isInitialLoad = false
            }
            
            // Convert grouped data to flat list for RecyclerView
            val listItems = buildListItems(groupedRecords)
            recordsAdapter.updateItems(listItems)
        }
    }
    
    private fun buildListItems(groupedRecords: Map<String, Map<TestType, List<TestRecord>>>): List<RecordListItem> {
        val listItems = mutableListOf<RecordListItem>()
        
        groupedRecords.forEach { (monthName, testTypeMap) ->
            val isMonthExpanded = expandedMonths.contains(monthName)
            
            // Add month header
            listItems.add(RecordListItem.MonthHeader(monthName, isMonthExpanded))
            
            // Only add content if month is expanded
            if (isMonthExpanded) {
                // Add test type sections in order: Glucose, Creatinine, Cholesterol
                val orderedTestTypes = listOf(TestType.GLUCOSE, TestType.CREATININE, TestType.CHOLESTEROL)
                
                orderedTestTypes.forEach { testType ->
                    val records: List<TestRecord>? = testTypeMap[testType]
                    if (!records.isNullOrEmpty()) {
                        val testTypeKey = "${monthName}_${testType.displayName}"
                        val isTestTypeExpanded = expandedTestTypes.contains(testTypeKey)
                        
                        // Add test type header
                        listItems.add(RecordListItem.TestTypeHeader(testType, monthName, isTestTypeExpanded))
                        
                        // Only add records if test type is expanded
                        if (isTestTypeExpanded) {
                            records.forEach { record ->
                                listItems.add(RecordListItem.TestRecordItem(record))
                            }
                        }
                    }
                }
            }
        }
        
        return listItems
    }
    
    private fun toggleMonthExpansion(monthName: String) {
        if (expandedMonths.contains(monthName)) {
            expandedMonths.remove(monthName)
        } else {
            expandedMonths.add(monthName)
        }
        loadRecords() // Refresh the list
    }
    
    private fun toggleTestTypeExpansion(monthName: String, testTypeName: String) {
        val key = "${monthName}_${testTypeName}"
        if (expandedTestTypes.contains(key)) {
            expandedTestTypes.remove(key)
        } else {
            expandedTestTypes.add(key)
        }
        loadRecords() // Refresh the list
    }
}