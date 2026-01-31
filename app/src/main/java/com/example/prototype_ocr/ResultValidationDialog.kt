package com.example.prototype_ocr

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Button
import android.widget.Spinner
import android.widget.ArrayAdapter
import com.example.prototype_ocr.data.TestType

object ResultValidationDialog {
    
    fun show(
        context: Context,
        ocrResult: OcrResult?,
        allDetectedText: String,
        croppedBitmap: Bitmap,
        detectedTestType: TestType, // Pre-select detected test type
        onSave: (OcrResult?, TestType) -> Unit,
        onDiscard: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_result_validation, null)
        
        val testTypeSpinner = dialogView.findViewById<Spinner>(R.id.testTypeSpinner)
        val imagePreview = dialogView.findViewById<ImageView>(R.id.imagePreview)
        val resultText = dialogView.findViewById<TextView>(R.id.resultText)
        val confidenceText = dialogView.findViewById<TextView>(R.id.confidenceText)
        val rawTextView = dialogView.findViewById<TextView>(R.id.rawText)
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)
        val discardButton = dialogView.findViewById<Button>(R.id.discardButton)
        
        // Setup test type spinner
        val testTypes = TestType.values()
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            testTypes.map { it.displayName }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        testTypeSpinner.adapter = adapter
        
        // Pre-select detected test type
        val detectedIndex = testTypes.indexOf(detectedTestType)
        if (detectedIndex >= 0) {
            testTypeSpinner.setSelection(detectedIndex)
        }
        
        // Set data
        imagePreview.setImageBitmap(croppedBitmap)
        
        if (ocrResult != null) {
            // mg/dL value detected
            resultText.text = "${ocrResult.value} mg/dL"
            confidenceText.text = "Confidence: ${(ocrResult.confidence * 100).toInt()}%"
            confidenceText.visibility = android.view.View.VISIBLE
        } else {
            // No mg/dL value detected
            resultText.text = "No mg/dL value detected"
            confidenceText.visibility = android.view.View.GONE
        }
        
        // Always show all detected text
        rawTextView.text = "Detected text:\n$allDetectedText"
        
        // Update button text based on whether mg/dL value was detected
        if (ocrResult != null) {
            saveButton.text = "Save Result"
        } else {
            saveButton.text = "Save OCR Text"
        }
        
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true) // Allow back button to dismiss
            .create()
        
        // Handle back button press
        dialog.setOnCancelListener {
            onDiscard() // Treat cancel as discard
        }
        
        saveButton.setOnClickListener {
            val selectedTestType = testTypes[testTypeSpinner.selectedItemPosition]
            onSave(ocrResult, selectedTestType) // Pass both OcrResult and TestType
            dialog.dismiss()
        }
        
        discardButton.setOnClickListener {
            onDiscard()
            dialog.dismiss()
        }
        
        dialog.show()
    }
}