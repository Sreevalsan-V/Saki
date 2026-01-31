package com.example.prototype_ocr

import android.content.Context
import com.example.prototype_ocr.data.TestRecord
import com.example.prototype_ocr.data.TestType
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

object CombinedPdfGenerator {
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    
    fun createCombinedPdf(
        context: Context,
        monthName: String,
        glucoseRecord: TestRecord?,
        creatinineRecord: TestRecord?,
        cholesterolRecord: TestRecord?
    ): File? {
        return try {
            val pdfDir = File(context.filesDir, "upload_pdfs")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()
            val pdfFile = File(pdfDir, "upload_${timestamp}.pdf")
            val writer = PdfWriter(FileOutputStream(pdfFile))
            val pdfDoc = PdfDocument(writer)
            val document = Document(pdfDoc)
            
            // Add title
            val title = Paragraph("Medical Test Upload Report")
                .setFontSize(22f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
            document.add(title)
            
            val subtitle = Paragraph(monthName)
                .setFontSize(16f)
                .setTextAlignment(TextAlignment.CENTER)
            document.add(subtitle)
            
            document.add(Paragraph(" "))
            
            // Add summary table
            val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f)))
                .setWidth(UnitValue.createPercentValue(100f))
            
            summaryTable.addCell(Cell().add(Paragraph("Upload Date:").setBold()))
            summaryTable.addCell(Cell().add(Paragraph(dateFormat.format(Date()))))
            
            summaryTable.addCell(Cell().add(Paragraph("Tests Included:").setBold()))
            val testsIncluded = buildString {
                val tests = mutableListOf<String>()
                if (glucoseRecord != null) tests.add("Glucose")
                if (creatinineRecord != null) tests.add("Creatinine")
                if (cholesterolRecord != null) tests.add("Cholesterol")
                append(tests.joinToString(", "))
            }
            summaryTable.addCell(Cell().add(Paragraph(testsIncluded)))
            
            document.add(summaryTable)
            document.add(Paragraph(" "))
            
            // Add each test
            val imageDir = File(context.filesDir, "lcd_images")
            
            glucoseRecord?.let {
                addTestSection(document, it, imageDir)
            }
            
            creatinineRecord?.let {
                addTestSection(document, it, imageDir)
            }
            
            cholesterolRecord?.let {
                addTestSection(document, it, imageDir)
            }
            
            document.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun addTestSection(document: Document, record: TestRecord, imageDir: File) {
        // Section divider
        document.add(Paragraph("—".repeat(50))
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10f))
        
        document.add(Paragraph(" "))
        
        // Test header
        val testHeader = Paragraph("${record.testType.displayName} - Test ${record.testNumber}")
            .setFontSize(18f)
            .setBold()
        document.add(testHeader)
        
        // Test details table
        val detailsTable = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
            .setWidth(UnitValue.createPercentValue(100f))
        
        detailsTable.addCell(Cell().add(Paragraph("Date:").setBold()))
        detailsTable.addCell(Cell().add(Paragraph(dateFormat.format(record.validationTimestamp))))
        
        detailsTable.addCell(Cell().add(Paragraph("Result:").setBold()))
        val resultText = if (record.resultValue != null) {
            "${record.resultValue} ${record.testType.unit}"
        } else {
            "No value extracted"
        }
        detailsTable.addCell(Cell().add(Paragraph(resultText)))
        
        record.confidence?.let { confidence ->
            detailsTable.addCell(Cell().add(Paragraph("Confidence:").setBold()))
            detailsTable.addCell(Cell().add(Paragraph("${(confidence * 100).toInt()}%")))
        }
        
        if (record.latitude != null && record.longitude != null) {
            detailsTable.addCell(Cell().add(Paragraph("Location:").setBold()))
            val lat = String.format("%.6f", record.latitude)
            val lon = String.format("%.6f", record.longitude)
            detailsTable.addCell(Cell().add(Paragraph("$lat, $lon")))
        }
        
        document.add(detailsTable)
        document.add(Paragraph(" "))
        
        // Add image
        val imageFile = File(imageDir, record.imageFileName)
        if (imageFile.exists()) {
            try {
                val imageData = ImageDataFactory.create(imageFile.absolutePath)
                val pdfImage = Image(imageData)
                pdfImage.setAutoScale(true)
                pdfImage.setMaxWidth(UnitValue.createPercentValue(70f))
                document.add(pdfImage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        document.add(Paragraph(" "))
    }
}
