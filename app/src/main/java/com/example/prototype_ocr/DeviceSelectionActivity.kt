package com.example.prototype_ocr

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.prototype_ocr.data.DeviceType

class DeviceSelectionActivity : AppCompatActivity() {
    
    private lateinit var deviceSpinner: Spinner
    private lateinit var scanDeviceButton: Button
    private lateinit var uploadRecordsButton: Button
    private lateinit var viewRecordsButton: Button
    private var selectedDevice = DeviceType.HORIBA
    
    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val qrCode = result.data?.getStringExtra(QrScannerActivity.EXTRA_QR_CODE)
            if (qrCode != null) {
                // Open upload selection with QR code
                val intent = Intent(this, UploadSelectionActivity::class.java)
                intent.putExtra("device_id", qrCode)
                startActivity(intent)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Lock to portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        setContentView(R.layout.activity_device_selection)
        
        deviceSpinner = findViewById(R.id.deviceSpinner)
        scanDeviceButton = findViewById(R.id.scanDeviceButton)
        uploadRecordsButton = findViewById(R.id.uploadRecordsButton)
        viewRecordsButton = findViewById(R.id.viewRecordsButton)
        
        // Setup device spinner
        val devices = listOf("Horiba", "Robonik")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, devices)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceSpinner.adapter = adapter
        
        deviceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDevice = when (position) {
                    0 -> DeviceType.HORIBA
                    1 -> DeviceType.ROBONIK
                    else -> DeviceType.HORIBA
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedDevice = DeviceType.HORIBA
            }
        }
        
        scanDeviceButton.setOnClickListener {
            openCamera(selectedDevice)
        }
        
        uploadRecordsButton.setOnClickListener {
            // Open QR scanner first
            val intent = Intent(this, QrScannerActivity::class.java)
            qrScannerLauncher.launch(intent)
        }
        
        viewRecordsButton.setOnClickListener {
            val intent = Intent(this, RecordsActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun openCamera(deviceType: DeviceType) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("device_type", deviceType.name)
        startActivity(intent)
    }
}
