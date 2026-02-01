package com.example.prototype_ocr

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.prototype_ocr.data.Upload
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PastUploadsAdapter(
    private val onUploadClick: (Upload) -> Unit
) : RecyclerView.Adapter<PastUploadsAdapter.UploadViewHolder>() {

    private var uploads: List<Upload> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UploadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_past_upload, parent, false)
        return UploadViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: UploadViewHolder, position: Int) {
        holder.bind(uploads[position])
    }
    
    override fun getItemCount() = uploads.size
    
    fun updateUploads(newUploads: List<Upload>) {
        uploads = newUploads
        notifyDataSetChanged()
    }
    
    inner class UploadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val monthNameText: TextView = itemView.findViewById(R.id.uploadMonthText)
        private val userNameText: TextView = itemView.findViewById(R.id.uploadDeviceIdText)  // Reusing ID for user name
        private val dateText: TextView = itemView.findViewById(R.id.uploadDateText)
        private val testCountText: TextView = itemView.findViewById(R.id.uploadTestCountText)
        private val summaryText: TextView = itemView.findViewById(R.id.uploadSummaryText)
        private val shareButton: Button = itemView.findViewById(R.id.shareButton)
        
        fun bind(upload: Upload) {
            monthNameText.text = "${upload.panelId} - ${upload.monthName}"
            userNameText.text = upload.userName
            dateText.text = "Uploaded on ${dateFormat.format(Date(upload.uploadTimestamp))}"
            testCountText.text = "${upload.getRecordCount()} test records"
            
            // Build summary with counts
            val summary = buildString {
                val counts = mutableListOf<String>()
                val glucoseCount = if (upload.glucoseRecord != null) 1 else 0
                val creatinineCount = if (upload.creatinineRecord != null) 1 else 0
                val cholesterolCount = if (upload.cholesterolRecord != null) 1 else 0
                
                if (glucoseCount > 0) counts.add("Glucose ($glucoseCount)")
                if (creatinineCount > 0) counts.add("Creatinine ($creatinineCount)")
                if (cholesterolCount > 0) counts.add("Cholesterol ($cholesterolCount)")
                append(counts.joinToString(" • "))
            }
            summaryText.text = summary
            
            itemView.setOnClickListener {
                onUploadClick(upload)
            }
            
            shareButton.setOnClickListener {
                shareToWhatsApp(upload)
            }
        }
        
        private fun shareToWhatsApp(upload: Upload) {
            val context = itemView.context
            val pdfDir = File(context.filesDir, "upload_pdfs")
            val pdfFile = File(pdfDir, upload.pdfFileName)
            
            if (!pdfFile.exists()) {
                Toast.makeText(context, "PDF file not found", Toast.LENGTH_SHORT).show()
                return
            }
            
            val pdfUri = FileProvider.getUriForFile(
                context,
                "${context.applicationContext.packageName}.fileprovider",
                pdfFile
            )
            
            val whatsappIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "application/pdf"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                putExtra(Intent.EXTRA_TEXT, "Medical Test Upload - Panel: ${upload.panelId} | ${upload.monthName} (${upload.userName})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            try {
                context.startActivity(whatsappIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
