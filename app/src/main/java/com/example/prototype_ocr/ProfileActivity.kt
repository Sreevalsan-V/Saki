package com.example.prototype_ocr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.prototype_ocr.api.AuthRepository

class ProfileActivity : AppCompatActivity() {
    
    private lateinit var authRepository: AuthRepository
    
    companion object {
        private const val TAG = "ProfileActivity"
    }
    
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
            Log.e(TAG, "No cached user data found!")
            finish()
            return
        }
        
        // Log all user data for debugging
        Log.d(TAG, "=== CACHED USER DATA ===")
        Log.d(TAG, "ID: '${userData.id}'")
        Log.d(TAG, "Username: '${userData.username}'")
        Log.d(TAG, "Name: '${userData.name}'")
        Log.d(TAG, "Role: '${userData.role}'")
        Log.d(TAG, "Email: '${userData.email}'")
        Log.d(TAG, "Phone: '${userData.phoneNumber}'")
        Log.d(TAG, "PHC Name: '${userData.phcName}' (null=${userData.phcName == null}, blank=${userData.phcName?.isBlank()})")
        Log.d(TAG, "Hub Name: '${userData.hubName}' (null=${userData.hubName == null}, blank=${userData.hubName?.isBlank()})")
        Log.d(TAG, "Block Name: '${userData.blockName}' (null=${userData.blockName == null}, blank=${userData.blockName?.isBlank()})")
        Log.d(TAG, "District: '${userData.districtName}' (null=${userData.districtName == null}, blank=${userData.districtName?.isBlank()})")
        Log.d(TAG, "State: '${userData.state}'")
        Log.d(TAG, "=======================")
        
        // Populate UI
        findViewById<TextView>(R.id.profileName).text = userData.name
        findViewById<TextView>(R.id.profileUsername).text = userData.username
        findViewById<TextView>(R.id.profileRole).text = userData.role
        
        // Optional fields - show "Not Set" if missing
        findViewById<TextView>(R.id.profileEmail).text = 
            if (userData.email.isNullOrBlank()) "Not Set" else userData.email
        
        findViewById<TextView>(R.id.profilePhone).text = 
            if (userData.phoneNumber.isNullOrBlank()) "Not Set" else userData.phoneNumber
        
        findViewById<TextView>(R.id.profileHealthCenter).text = 
            if (userData.phcName.isNullOrBlank()) "⚠️ NOT SET (Required for uploads)" else userData.phcName
        
        findViewById<TextView>(R.id.profileHub).text = 
            if (userData.hubName.isNullOrBlank()) "⚠️ NOT SET (Required for uploads)" else userData.hubName
        
        findViewById<TextView>(R.id.profileBlock).text = 
            if (userData.blockName.isNullOrBlank()) "⚠️ NOT SET (Required for uploads)" else userData.blockName
        
        findViewById<TextView>(R.id.profileDistrict).text = 
            if (userData.districtName.isNullOrBlank()) "⚠️ NOT SET (Required for uploads)" else userData.districtName
        
        findViewById<TextView>(R.id.profileState).text = 
            if (userData.state.isNullOrBlank()) "Not Set" else userData.state
        
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
