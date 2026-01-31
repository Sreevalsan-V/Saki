package com.example.prototype_ocr

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.prototype_ocr.data.TestType
import com.example.prototype_ocr.data.Upload
import com.example.prototype_ocr.data.UploadManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class UploadDetailActivity : AppCompatActivity() {
    
    private lateinit var uploadManager: UploadManager
    private lateinit var upload: Upload
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_upload_detail)
        
        uploadManager = UploadManager(this)
        
        val uploadId = intent.getStringExtra("upload_id")
        if (uploadId == null) {
            Toast.makeText(this, "Upload not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        val loadedUpload = uploadManager.getUploadById(uploadId)
        if (loadedUpload == null) {
            Toast.makeText(this, "Upload not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        upload = loadedUpload
        setupViews()
    }
    
    private fun setupViews() {
        val monthText = findViewById<TextView>(R.id.uploadMonthText)
        val deviceIdText = findViewById<TextView>(R.id.uploadDeviceIdText)
        val dateText = findViewById<TextView>(R.id.uploadDateText)
        val testCountText = findViewById<TextView>(R.id.uploadTestCountText)
        val locationText = findViewById<TextView>(R.id.uploadLocationText)
        
        val glucoseRecords = findViewById<TextView>(R.id.glucoseRecordsText)
        val creatinineRecords = findViewById<TextView>(R.id.creatinineRecordsText)
        val cholesterolRecords = findViewById<TextView>(R.id.cholesterolRecordsText)
        
        val sharePdfButton = findViewById<Button>(R.id.sharePdfButton)
        val downloadPdfButton = findViewById<Button>(R.id.downloadPdfButton)
        val deleteButton = findViewById<Button>(R.id.deleteUploadButton)
        
        // Set title and info
        monthText.text = upload.monthName
        deviceIdText.text = upload.deviceId
        dateText.text = dateFormat.format(Date(upload.uploadTimestamp))
        testCountText.text = "${upload.getRecordCount()} records"
        
        // Set GPS location
        if (upload.latitude != null && upload.longitude != null) {
            locationText.text = String.format("%.6f, %.6f", upload.latitude, upload.longitude)
        } else {
            locationText.text = "Not Available"
        }
        
        // Get records with test names assigned
        val recordsWithNames = upload.getAllRecordsWithTestNames()
        
        // Glucose record
        val glucoseRecord = upload.glucoseRecord
        if (glucoseRecord != null) {
            val testIndex = recordsWithNames.indexOfFirst { it.id == glucoseRecord.id } + 1
            val gpsInfo = if (glucoseRecord.latitude != null && glucoseRecord.longitude != null) {
                "\nGPS: ${String.format("%.6f, %.6f", glucoseRecord.latitude, glucoseRecord.longitude)}"
            } else {
                ""
            }
            glucoseRecords.text = "${glucoseRecord.testType.displayName} Test - $testIndex\n${glucoseRecord.resultValue ?: "N/A"} ${glucoseRecord.testType.unit}\n${formatRecordDate(glucoseRecord.validationTimestamp)}$gpsInfo"
        } else {
            glucoseRecords.text = "No glucose record"
        }
        
        // Creatinine record
        val creatinineRecord = upload.creatinineRecord
        if (creatinineRecord != null) {
            val testIndex = recordsWithNames.indexOfFirst { it.id == creatinineRecord.id } + 1
            val gpsInfo = if (creatinineRecord.latitude != null && creatinineRecord.longitude != null) {
                "\nGPS: ${String.format("%.6f, %.6f", creatinineRecord.latitude, creatinineRecord.longitude)}"
            } else {
                ""
            }
            creatinineRecords.text = "${creatinineRecord.testType.displayName} Test - $testIndex\n${creatinineRecord.resultValue ?: "N/A"} ${creatinineRecord.testType.unit}\n${formatRecordDate(creatinineRecord.validationTimestamp)}$gpsInfo"
        } else {
            creatinineRecords.text = "No creatinine record"
        }
        
        // Cholesterol record
        val cholesterolRecord = upload.cholesterolRecord
        if (cholesterolRecord != null) {
            val testIndex = recordsWithNames.indexOfFirst { it.id == cholesterolRecord.id } + 1
            val gpsInfo = if (cholesterolRecord.latitude != null && cholesterolRecord.longitude != null) {
                "\nGPS: ${String.format("%.6f, %.6f", cholesterolRecord.latitude, cholesterolRecord.longitude)}"
            } else {
                ""
            }
            cholesterolRecords.text = "${cholesterolRecord.testType.displayName} Test - $testIndex\n${cholesterolRecord.resultValue ?: "N/A"} ${cholesterolRecord.testType.unit}\n${formatRecordDate(cholesterolRecord.validationTimestamp)}$gpsInfo"
        } else {
            cholesterolRecords.text = "No cholesterol record"
        }
        
        // Share button
        sharePdfButton.setOnClickListener {
            sharePdf()
        }
        
        // Download button
        downloadPdfButton.setOnClickListener {
            Toast.makeText(this, "PDF already saved to app storage", Toast.LENGTH_SHORT).show()
        }
        
        // Delete button
        deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }
    }
    
    private fun sharePdf() {
        val pdfDir = File(filesDir, "upload_pdfs")
        val pdfFile = File(pdfDir, upload.pdfFileName)
        
        if (!pdfFile.exists()) {
            Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        val pdfUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            pdfFile
        )
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_TEXT, "Medical Test Upload - ${upload.monthName}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share Upload Report"))
    }
    
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Upload")
            .setMessage("Are you sure you want to delete this upload? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteUpload()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteUpload() {
        // Delete PDF file
        val pdfDir = File(filesDir, "upload_pdfs")
        val pdfFile = File(pdfDir, upload.pdfFileName)
        if (pdfFile.exists()) {
            pdfFile.delete()
        }
        
        // Delete upload record
        val deleted = uploadManager.deleteUpload(upload.id)
        
        if (deleted) {
            Toast.makeText(this, "Upload deleted", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Failed to delete upload", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun formatRecordDate(timestamp: Long): String {
        val recordFormat = SimpleDateFormat("d MMM yyyy 'at' h:mm a", Locale.getDefault())
        return recordFormat.format(Date(timestamp))
    }
}
