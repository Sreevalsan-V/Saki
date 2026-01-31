package com.example.prototype_ocr

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.prototype_ocr.data.RecordsManager
import com.example.prototype_ocr.data.TestRecord
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class RecordDetailActivity : AppCompatActivity() {
    
    private lateinit var recordsManager: RecordsManager
    private lateinit var testRecord: TestRecord
    private lateinit var sharePdfButton: Button
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Lock to portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        setContentView(R.layout.activity_record_detail)
        
        // Initialize components
        recordsManager = RecordsManager(this)
        
        // Get record ID from intent
        val recordId = intent.getStringExtra("record_id")
        if (recordId == null) {
            Toast.makeText(this, "Record not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Load record
        val record = recordsManager.getRecordById(recordId)
        if (record == null) {
            Toast.makeText(this, "Record not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        testRecord = record
        setupViews()
    }
    
    private fun setupViews() {
        val testTitleText = findViewById<TextView>(R.id.testTitleText)
        val fullImageView = findViewById<ImageView>(R.id.fullImageView)
        val resultValueText = findViewById<TextView>(R.id.resultValueText)
        val testTypeDetailText = findViewById<TextView>(R.id.testTypeDetailText)
        val dateDetailText = findViewById<TextView>(R.id.dateDetailText)
        val confidenceLayout = findViewById<LinearLayout>(R.id.confidenceLayout)
        val confidenceDetailText = findViewById<TextView>(R.id.confidenceDetailText)
        val rawOcrText = findViewById<TextView>(R.id.rawOcrText)
        val deleteButton = findViewById<Button>(R.id.deleteButton)
        
        // Set title
        testTitleText.text = "Test ${testRecord.testNumber} - ${testRecord.testType.displayName}"
        
        // Load full image
        val imageDir = File(filesDir, "lcd_images")
        val imageFile = File(imageDir, testRecord.imageFileName)
        if (imageFile.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                fullImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                fullImageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } else {
            fullImageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        
        // Set result value
        if (testRecord.isValidResult && testRecord.resultValue != null) {
            resultValueText.text = "${testRecord.resultValue} mg/dL"
            resultValueText.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            resultValueText.text = "Generic OCR (no mg/dL value detected)"
            resultValueText.setTextColor(getColor(android.R.color.darker_gray))
        }
        
        // Set other details
        testTypeDetailText.text = testRecord.testType.displayName
        dateDetailText.text = dateFormat.format(Date(testRecord.validationTimestamp))
        
        // Set confidence (hide if not available)
        val confidence = testRecord.confidence
        if (confidence != null) {
            confidenceDetailText.text = "${(confidence * 100).toInt()}%"
            confidenceLayout.visibility = View.VISIBLE
        } else {
            confidenceLayout.visibility = View.GONE
        }
        
        // Set raw OCR text
        rawOcrText.text = testRecord.rawOcrText
        
        // Setup delete button
        deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }
        
        // Setup PDF share button
        sharePdfButton = findViewById(R.id.sharePdfButton)
        sharePdfButton.setOnClickListener {
            createAndSharePdf()
        }
    }
    
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Record")
            .setMessage("Are you sure you want to delete Test ${testRecord.testNumber}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteRecord()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteRecord() {
        // Delete image file
        val imageDir = File(filesDir, "lcd_images")
        val imageFile = File(imageDir, testRecord.imageFileName)
        if (imageFile.exists()) {
            imageFile.delete()
        }
        
        // Delete record from storage
        val deleted = recordsManager.deleteRecord(testRecord.id)
        
        if (deleted) {
            Toast.makeText(this, "Test ${testRecord.testNumber} deleted", Toast.LENGTH_SHORT).show()
            finish() // Close the detail view
        } else {
            Toast.makeText(this, "Failed to delete record", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createAndSharePdf() {
        try {
            val pdfFile = createPdfReport()
            shareToWhatsApp(pdfFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to create PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createPdfReport(): File {
        // Create PDF file
        val pdfDir = File(filesDir, "pdf_reports")
        if (!pdfDir.exists()) {
            pdfDir.mkdirs()
        }
        
        val pdfFile = File(pdfDir, "test_${testRecord.testNumber}_${testRecord.id}.pdf")
        val writer = PdfWriter(FileOutputStream(pdfFile))
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc)
        
        // Add title
        val title = Paragraph("Medical Test Report")
            .setFontSize(20f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
        document.add(title)
        
        // Add spacing
        document.add(Paragraph(" "))
        
        // Add test information table
        val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
            .setWidth(UnitValue.createPercentValue(100f))
        
        infoTable.addCell(Cell().add(Paragraph("Test Number:").setBold()))
        infoTable.addCell(Cell().add(Paragraph(testRecord.testNumber.toString())))
        
        infoTable.addCell(Cell().add(Paragraph("Test Type:").setBold()))
        infoTable.addCell(Cell().add(Paragraph(testRecord.testType.displayName)))
        
        infoTable.addCell(Cell().add(Paragraph("Date:").setBold()))
        infoTable.addCell(Cell().add(Paragraph(dateFormat.format(testRecord.validationTimestamp))))
        
        infoTable.addCell(Cell().add(Paragraph("Result:").setBold()))
        val resultText = if (testRecord.resultValue != null) {
            "${testRecord.resultValue} ${testRecord.testType.unit}"
        } else {
            "No value extracted"
        }
        infoTable.addCell(Cell().add(Paragraph(resultText)))
        
        testRecord.confidence?.let { confidence ->
            infoTable.addCell(Cell().add(Paragraph("Confidence:").setBold()))
            infoTable.addCell(Cell().add(Paragraph("${(confidence * 100).toInt()}%")))
        }
        
        document.add(infoTable)
        
        // Add spacing
        document.add(Paragraph(" "))
        
        // Add image if exists
        val imageDir = File(filesDir, "lcd_images")
        val imageFile = File(imageDir, testRecord.imageFileName)
        if (imageFile.exists()) {
            document.add(Paragraph("Test Image:").setBold())
            val imageData = ImageDataFactory.create(imageFile.absolutePath)
            val pdfImage = Image(imageData)
            pdfImage.setAutoScale(true)
            pdfImage.setMaxWidth(UnitValue.createPercentValue(80f))
            document.add(pdfImage)
        }
        
        // Add raw OCR text
        document.add(Paragraph(" "))
        document.add(Paragraph("Raw OCR Text:").setBold())
        document.add(Paragraph(testRecord.rawOcrText))
        
        document.close()
        return pdfFile
    }
    
    private fun shareToWhatsApp(pdfFile: File) {
        val pdfUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            pdfFile
        )
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_TEXT, "Medical Test Report - Test ${testRecord.testNumber}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp") // Specifically target WhatsApp
        }
        
        try {
            startActivity(shareIntent)
        } catch (e: Exception) {
            // WhatsApp not installed, show general share dialog
            shareIntent.setPackage(null)
            val chooser = Intent.createChooser(shareIntent, "Share Test Report")
            startActivity(chooser)
        }
    }
}