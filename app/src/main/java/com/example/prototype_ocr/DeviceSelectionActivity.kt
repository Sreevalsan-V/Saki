package com.example.prototype_ocr

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.prototype_ocr.data.DeviceType

class DeviceSelectionActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Lock to portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        setContentView(R.layout.activity_device_selection)
        
        val horibaButton: Button = findViewById(R.id.horibaButton)
        val robonikButton: Button = findViewById(R.id.robonikButton)
        val viewRecordsButton: Button = findViewById(R.id.viewRecordsButton)
        
        horibaButton.setOnClickListener {
            openCamera(DeviceType.HORIBA)
        }
        
        robonikButton.setOnClickListener {
            openCamera(DeviceType.ROBONIK)
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
