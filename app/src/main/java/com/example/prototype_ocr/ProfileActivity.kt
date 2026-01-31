package com.example.prototype_ocr

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.prototype_ocr.api.AuthRepository

class ProfileActivity : AppCompatActivity() {
    
    private lateinit var authRepository: AuthRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        
        supportActionBar?.title = "Profile"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        authRepository = AuthRepository(this)
        
        // Get user data
        val userData = authRepository.getCachedUserData()
        
        if (userData == null) {
            // Should not happen, but handle gracefully
            finish()
            return
        }
        
        // Populate UI
        findViewById<TextView>(R.id.profileName).text = userData.name
        findViewById<TextView>(R.id.profileUsername).text = userData.username
        findViewById<TextView>(R.id.profileRole).text = userData.role
        
        // Optional fields
        userData.email?.let {
            findViewById<TextView>(R.id.profileEmail).text = it
        }
        
        userData.phoneNumber?.let {
            findViewById<TextView>(R.id.profilePhone).text = it
        }
        
        userData.healthCenter?.let {
            findViewById<TextView>(R.id.profileHealthCenter).text = it
        }
        
        userData.district?.let {
            findViewById<TextView>(R.id.profileDistrict).text = it
        }
        
        userData.state?.let {
            findViewById<TextView>(R.id.profileState).text = it
        }
        
        // Logout button
        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            showLogoutDialog()
        }
    }
    
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performLogout() {
        authRepository.logout()
        
        // Navigate to login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
