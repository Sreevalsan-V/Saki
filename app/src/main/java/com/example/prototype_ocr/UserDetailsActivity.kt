package com.example.prototype_ocr

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class UserDetailsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_details)
        
        val iqcButton = findViewById<Button>(R.id.iqcButton)
        val eqasButton = findViewById<Button>(R.id.eqasButton)
        
        // IQC button - does nothing for now
        iqcButton.setOnClickListener {
            // TODO: Implement IQC functionality
        }
        
        // EQAS button - goes to device selection page
        eqasButton.setOnClickListener {
            val intent = Intent(this, DeviceSelectionActivity::class.java)
            startActivity(intent)
        }
    }
}
