package com.example.prototype_ocr

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prototype_ocr.data.TestRecord
import com.example.prototype_ocr.data.TestType
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class UploadSelectionAdapter(
    private val imageDir: File,
    private val onSelectionChange: (TestRecord, TestType) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<UploadSelectionItem> = emptyList()
    private var selectedGlucose: TestRecord? = null
    private var selectedCreatinine: TestRecord? = null
    private var selectedCholesterol: TestRecord? = null
    
    private val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
    
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_RECORD = 1
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is UploadSelectionItem.Header -> TYPE_HEADER
            is UploadSelectionItem.RecordItem -> TYPE_RECORD
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_upload_selection_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_upload_selection_record, parent, false)
                RecordViewHolder(view)
            }
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is UploadSelectionItem.Header -> {
                (holder as HeaderViewHolder).bind(item.testType)
            }
            is UploadSelectionItem.RecordItem -> {
                (holder as RecordViewHolder).bind(item.record)
            }
        }
    }
    
    override fun getItemCount() = items.size
    
    fun updateItems(newItems: List<UploadSelectionItem>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    fun updateSelection(glucose: TestRecord?, creatinine: TestRecord?, cholesterol: TestRecord?) {
        selectedGlucose = glucose
        selectedCreatinine = creatinine
        selectedCholesterol = cholesterol
        notifyDataSetChanged()
    }
    
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerText: TextView = itemView.findViewById(R.id.sectionHeaderText)
        
        fun bind(testType: TestType) {
            headerText.text = testType.displayName
        }
    }
    
    inner class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val radioButton: RadioButton = itemView.findViewById(R.id.recordRadioButton)
        private val testNumberText: TextView = itemView.findViewById(R.id.testNumberText)
        private val resultValueText: TextView = itemView.findViewById(R.id.resultValueText)
        private val dateText: TextView = itemView.findViewById(R.id.dateText)
        private val thumbnailImage: ImageView = itemView.findViewById(R.id.thumbnailImage)
        
        fun bind(record: TestRecord) {
            testNumberText.text = "Test ${record.testNumber}"
            
            if (record.resultValue != null) {
                resultValueText.text = "${record.resultValue} ${record.testType.unit}"
                resultValueText.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
            } else {
                resultValueText.text = "No value"
                resultValueText.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
            }
            
            dateText.text = dateFormat.format(Date(record.validationTimestamp))
            
            // Load thumbnail
            val imageFile = File(imageDir, record.imageFileName)
            if (imageFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                thumbnailImage.setImageBitmap(bitmap)
            }
            
            // Set radio button state
            val isSelected = when (record.testType) {
                TestType.GLUCOSE -> selectedGlucose?.id == record.id
                TestType.CREATININE -> selectedCreatinine?.id == record.id
                TestType.CHOLESTEROL -> selectedCholesterol?.id == record.id
            }
            radioButton.isChecked = isSelected
            
            // Handle clicks
            itemView.setOnClickListener {
                onSelectionChange(record, record.testType)
            }
            
            radioButton.setOnClickListener {
                onSelectionChange(record, record.testType)
            }
        }
    }
}
