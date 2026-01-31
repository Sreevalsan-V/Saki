package com.example.prototype_ocr

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prototype_ocr.data.TestRecord
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.BitmapFactory
import java.io.File

class RecordsAdapter(
    private var records: List<TestRecord>,
    private val imageDir: File,
    private val onItemClick: (TestRecord) -> Unit
) : RecyclerView.Adapter<RecordsAdapter.RecordViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnailImageView: ImageView = itemView.findViewById(R.id.thumbnailImageView)
        val testNumberText: TextView = itemView.findViewById(R.id.testNumberText)
        val testTypeText: TextView = itemView.findViewById(R.id.testTypeText)
        val resultText: TextView = itemView.findViewById(R.id.resultText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val confidenceText: TextView = itemView.findViewById(R.id.confidenceText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_test_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val record = records[position]
        
        // Set text data
        holder.testNumberText.text = "Test ${record.testNumber}"
        holder.testTypeText.text = record.testType.displayName
        holder.dateText.text = dateFormat.format(Date(record.validationTimestamp))
        
        // Set result text and confidence
        if (record.isValidResult && record.resultValue != null) {
            holder.resultText.text = "${record.resultValue} mg/dL"
            holder.resultText.setTextColor(
                holder.itemView.context.getColor(android.R.color.holo_green_dark)
            )
            holder.confidenceText.text = if (record.confidence != null) {
                "${(record.confidence * 100).toInt()}% confidence"
            } else {
                ""
            }
            holder.confidenceText.visibility = View.VISIBLE
        } else {
            holder.resultText.text = "Generic OCR"
            holder.resultText.setTextColor(
                holder.itemView.context.getColor(android.R.color.darker_gray)
            )
            holder.confidenceText.visibility = View.GONE
        }
        
        // Load thumbnail image
        val imageFile = File(imageDir, record.imageFileName)
        if (imageFile.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                holder.thumbnailImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.thumbnailImageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } else {
            holder.thumbnailImageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(record)
        }
    }

    override fun getItemCount(): Int = records.size

    fun updateRecords(newRecords: List<TestRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }
}