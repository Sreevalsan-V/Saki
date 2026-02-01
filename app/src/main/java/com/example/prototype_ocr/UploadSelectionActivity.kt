package com.example.prototype_ocr

import android.Manifest
import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototype_ocr.data.*
import com.example.prototype_ocr.api.AuthRepository
import com.example.prototype_ocr.api.ServerUploadHelper
import com.example.prototype_ocr.api.UserData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class UploadSelectionActivity : AppCompatActivity() {
    
    private lateinit var recordsManager: RecordsManager
    private lateinit var uploadManager: UploadManager
    private lateinit var adapter: UploadSelectionAdapter
    private lateinit var emptyStateText: TextView
    private lateinit var authRepository: AuthRepository
    private lateinit var uploadButton: Button
    private lateinit var currentMonthText: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private var selectedGlucose: TestRecord? = null
    private var selectedCreatinine: TestRecord? = null
    private var selectedCholesterol: TestRecord? = null
    private var currentMonth: String = ""
    private var userData: UserData? = null
    private var panelId: String = ""
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 102
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_upload_selection)
        
        recordsManager = RecordsManager(this)
        uploadManager = UploadManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        authRepository = AuthRepository(this)
        
        // Get panel ID from QR scan
        panelId = intent.getStringExtra("device_id") ?: ""
        if (panelId.isEmpty()) {
            Toast.makeText(this, "No panel ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Get user data from auth repository
        userData = authRepository.getCachedUserData()
        if (userData == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        currentMonthText = findViewById(R.id.currentMonthText)
        emptyStateText = findViewById(R.id.emptyStateText)
        uploadButton = findViewById(R.id.uploadButton)
        
        // Get current month
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        currentMonth = monthFormat.format(Date())
        currentMonthText.text = "Select tests for $currentMonth\nPanel: $panelId | User: ${userData!!.name}"
        
        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.uploadSelectionRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        val imageDir = File(filesDir, "lcd_images")
        adapter = UploadSelectionAdapter(imageDir) { record, testType ->
            onRecordSelected(record, testType)
        }
        recyclerView.adapter = adapter
        
        uploadButton.setOnClickListener {
            getCurrentLocationAndShowDialog()
        }
        
        loadCurrentMonthRecords()
    }
    
    private fun getCurrentLocationAndShowDialog() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            showValidationDialog(location)
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to get location, continuing without GPS", Toast.LENGTH_SHORT).show()
            showValidationDialog(null)
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndShowDialog()
            } else {
                showValidationDialog(null)
            }
        }
    }
    
    private fun loadCurrentMonthRecords() {
        val allGrouped = recordsManager.getRecordsGroupedByMonthAndType()
        val currentMonthRecords = allGrouped[currentMonth]
        
        if (currentMonthRecords == null || currentMonthRecords.isEmpty()) {
            emptyStateText.visibility = View.VISIBLE
            emptyStateText.text = "No test records for $currentMonth"
            uploadButton.isEnabled = false
            return
        }
        
        emptyStateText.visibility = View.GONE
        
        // Convert to list for adapter
        val items = mutableListOf<UploadSelectionItem>()
        
        // Add Glucose section
        currentMonthRecords[TestType.GLUCOSE]?.let { records ->
            items.add(UploadSelectionItem.Header(TestType.GLUCOSE))
            records.forEach { items.add(UploadSelectionItem.RecordItem(it)) }
        }
        
        // Add Creatinine section
        currentMonthRecords[TestType.CREATININE]?.let { records ->
            items.add(UploadSelectionItem.Header(TestType.CREATININE))
            records.forEach { items.add(UploadSelectionItem.RecordItem(it)) }
        }
        
        // Add Cholesterol section
        currentMonthRecords[TestType.CHOLESTEROL]?.let { records ->
            items.add(UploadSelectionItem.Header(TestType.CHOLESTEROL))
            records.forEach { items.add(UploadSelectionItem.RecordItem(it)) }
        }
        
        adapter.updateItems(items)
        updateUploadButtonState()
    }
    
    private fun onRecordSelected(record: TestRecord, testType: TestType) {
        when (testType) {
            TestType.GLUCOSE -> {
                selectedGlucose = if (selectedGlucose?.id == record.id) null else record
            }
            TestType.CREATININE -> {
                selectedCreatinine = if (selectedCreatinine?.id == record.id) null else record
            }
            TestType.CHOLESTEROL -> {
                selectedCholesterol = if (selectedCholesterol?.id == record.id) null else record
            }
        }
        
        adapter.updateSelection(selectedGlucose, selectedCreatinine, selectedCholesterol)
        updateUploadButtonState()
    }
    
    private fun updateUploadButtonState() {
        val hasSelection = selectedGlucose != null || selectedCreatinine != null || selectedCholesterol != null
        uploadButton.isEnabled = hasSelection
    }
    
    private fun showValidationDialog(location: Location?) {
        val message = buildString {
            append("Upload Summary:\n")
            append("Panel: $panelId\n")
            append("User: ${userData?.name}\n\n")
            append("Selected Tests:\n\n")
            
            selectedGlucose?.let {
                append("✓ Glucose: ${it.resultValue ?: "N/A"} ${TestType.GLUCOSE.unit}\n")
            }
            
            selectedCreatinine?.let {
                append("✓ Creatinine: ${it.resultValue ?: "N/A"} ${TestType.CREATININE.unit}\n")
            }
            
            selectedCholesterol?.let {
                append("✓ Cholesterol: ${it.resultValue ?: "N/A"} ${TestType.CHOLESTEROL.unit}\n")
            }
            
            append("\nProceed with upload?")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Confirm Upload")
            .setMessage(message)
            .setPositiveButton("Upload") { _, _ ->
                createUpload(location)
            }
            .setNegativeButton("Go Back", null)
            .show()
    }
    
    private fun createUpload(location: Location?) {
        // Generate PDF
        val pdfFile = CombinedPdfGenerator.createCombinedPdf(
            context = this,
            monthName = currentMonth,
            glucoseRecord = selectedGlucose,
            creatinineRecord = selectedCreatinine,
            cholesterolRecord = selectedCholesterol
        )
        
        if (pdfFile == null) {
            Toast.makeText(this, "Failed to create PDF", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Use live GPS coordinates from current location
        val latitude = location?.latitude
        val longitude = location?.longitude
        
        // Get user details
        val user = userData!!
        
        // Create upload object with panel ID, user details and live GPS data
        val upload = Upload(
            uploadTimestamp = System.currentTimeMillis(),
            monthName = currentMonth,
            panelId = panelId,
            userId = user.id,
            userName = user.name,
            phcName = user.phcName ?: "",
            hubName = user.hubName ?: "",
            blockName = user.blockName ?: "",
            districtName = user.districtName ?: "",
            glucoseRecord = selectedGlucose,
            creatinineRecord = selectedCreatinine,
            cholesterolRecord = selectedCholesterol,
            pdfFileName = pdfFile.name,
            latitude = latitude,
            longitude = longitude
        )
        
        // Save upload locally first
        val saved = uploadManager.saveUpload(upload)
        
        if (!saved) {
            Toast.makeText(this, "Failed to save upload", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get images directory
        val imagesDir = File(filesDir, "lcd_images")
        
        // Show upload dialog with server option
        ServerUploadHelper.showUploadDialog(
            context = this,
            upload = upload,
            imagesDir = imagesDir,
            pdfFile = pdfFile,
            onLocalSaveOnly = {
                Toast.makeText(this, "Upload saved locally for panel $panelId (${userData?.name})", Toast.LENGTH_LONG).show()
                finish()
            },
            onServerUploadSuccess = { response ->
                Toast.makeText(this, "Upload successful! Server ID: ${response.uploadId}", Toast.LENGTH_LONG).show()
                finish()
            }
        )
    }
}

sealed class UploadSelectionItem {
    data class Header(val testType: TestType) : UploadSelectionItem()
    data class RecordItem(val record: TestRecord) : UploadSelectionItem()
}
