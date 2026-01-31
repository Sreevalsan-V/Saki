package com.example.prototype_ocr

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prototype_ocr.data.RecordListItem
import com.example.prototype_ocr.data.TestRecord
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.BitmapFactory
import java.io.File

class GroupedRecordsAdapter(
    private var items: List<RecordListItem>,
    private val imageDir: File,
    private val onItemClick: (TestRecord) -> Unit,
    private val onMonthHeaderClick: (String) -> Unit,
    private val onTestTypeHeaderClick: (String, String) -> Unit // monthName, testTypeName
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_MONTH_HEADER = 0
        private const val TYPE_TEST_TYPE_HEADER = 1
        private const val TYPE_RECORD = 2
    }

    private val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is RecordListItem.MonthHeader -> TYPE_MONTH_HEADER
            is RecordListItem.TestTypeHeader -> TYPE_TEST_TYPE_HEADER
            is RecordListItem.TestRecordItem -> TYPE_RECORD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_MONTH_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_month_header, parent, false)
                MonthHeaderViewHolder(view)
            }
            TYPE_TEST_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_test_type_header, parent, false)
                TestTypeHeaderViewHolder(view)
            }
            TYPE_RECORD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_test_record, parent, false)
                RecordViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is RecordListItem.MonthHeader -> {
                (holder as MonthHeaderViewHolder).bind(item.monthName, item.isExpanded)
            }
            is RecordListItem.TestTypeHeader -> {
                (holder as TestTypeHeaderViewHolder).bind(item.testType.displayName, item.monthName, item.isExpanded)
            }
            is RecordListItem.TestRecordItem -> {
                (holder as RecordViewHolder).bind(item.record)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<RecordListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    // Month Header ViewHolder
    inner class MonthHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val monthHeaderText: TextView = itemView.findViewById(R.id.monthHeaderText)
        private val monthExpandIcon: ImageView = itemView.findViewById(R.id.monthExpandIcon)

        fun bind(monthName: String, isExpanded: Boolean) {
            monthHeaderText.text = monthName
            
            // Set expand/collapse icon
            monthExpandIcon.setImageResource(
                if (isExpanded) android.R.drawable.arrow_up_float
                else android.R.drawable.arrow_down_float
            )
            
            // Set click listener
            itemView.setOnClickListener {
                onMonthHeaderClick(monthName)
            }
        }
    }

    // Test Type Header ViewHolder  
    inner class TestTypeHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val testTypeHeaderText: TextView = itemView.findViewById(R.id.testTypeHeaderText)
        private val testTypeExpandIcon: ImageView = itemView.findViewById(R.id.testTypeExpandIcon)

        fun bind(testTypeName: String, monthName: String, isExpanded: Boolean) {
            testTypeHeaderText.text = testTypeName
            
            // Set expand/collapse icon
            testTypeExpandIcon.setImageResource(
                if (isExpanded) android.R.drawable.arrow_up_float
                else android.R.drawable.arrow_down_float
            )
            
            // Set click listener
            itemView.setOnClickListener {
                onTestTypeHeaderClick(monthName, testTypeName)
            }
        }
    }

    // Record ViewHolder (reusing existing logic)
    inner class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailImageView: ImageView = itemView.findViewById(R.id.thumbnailImageView)
        private val testNumberText: TextView = itemView.findViewById(R.id.testNumberText)
        private val testTypeText: TextView = itemView.findViewById(R.id.testTypeText)
        private val resultText: TextView = itemView.findViewById(R.id.resultText)
        private val dateText: TextView = itemView.findViewById(R.id.dateText)
        private val confidenceText: TextView = itemView.findViewById(R.id.confidenceText)

        fun bind(record: TestRecord) {
            // Set text data
            testNumberText.text = "Test ${record.testNumber}"
            testTypeText.text = record.testType.displayName
            dateText.text = dateFormat.format(Date(record.validationTimestamp))
            
            // Set result text and confidence
            if (record.isValidResult && record.resultValue != null) {
                resultText.text = "${record.resultValue} mg/dL"
                resultText.setTextColor(
                    itemView.context.getColor(android.R.color.holo_green_dark)
                )
                val confidence = record.confidence
                confidenceText.text = if (confidence != null) {
                    "${(confidence * 100).toInt()}% confidence"
                } else {
                    ""
                }
                confidenceText.visibility = View.VISIBLE
            } else {
                resultText.text = "Generic OCR"
                resultText.setTextColor(
                    itemView.context.getColor(android.R.color.darker_gray)
                )
                confidenceText.visibility = View.GONE
            }
            
            // Load thumbnail image
            val imageFile = File(imageDir, record.imageFileName)
            if (imageFile.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    thumbnailImageView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    thumbnailImageView.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } else {
                thumbnailImageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            
            // Set click listener
            itemView.setOnClickListener {
                onItemClick(record)
            }
        }
    }
}