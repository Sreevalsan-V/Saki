package com.example.prototype_ocr

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.prototype_ocr.api.AuthRepository
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var authRepository: AuthRepository
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authRepository = AuthRepository(this)
        
        // Check if already logged in
        if (authRepository.isLoggedIn()) {
            navigateToHome()
            return
        }
        
        setContentView(R.layout.activity_login)
        
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        progressBar = findViewById(R.id.progressBar)
        
        loginButton.setOnClickListener {
            performLogin()
        }
    }
    
    private fun performLogin() {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        
        // Validation
        if (username.isEmpty()) {
            usernameEditText.error = "Username required"
            usernameEditText.requestFocus()
            return
        }
        
        if (password.isEmpty()) {
            passwordEditText.error = "Password required"
            passwordEditText.requestFocus()
            return
        }
        
        // Show loading
        setLoading(true)
        
        lifecycleScope.launch {
            // Check if this is first time login for this user
            val isFirstLogin = !authRepository.isUserDataCached(username)
            
            // Try login
            val result = authRepository.login(username, password, forceOnline = isFirstLogin)
            
            result.onSuccess { userData ->
                setLoading(false)
                if (isFirstLogin) {
                    showFirstLoginDialog(userData.name)
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Welcome back, ${userData.name}!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToHome()
                }
            }
            
            result.onFailure { error ->
                setLoading(false)
                showErrorDialog(error.message ?: "Login failed")
            }
        }
    }
    
    private fun showFirstLoginDialog(userName: String) {
        AlertDialog.Builder(this)
            .setTitle("Account Initialized")
            .setMessage("Welcome, $userName!\n\nYour account has been set up successfully. You can now use the app offline for creating test records. Internet connection is required only for uploading data.")
            .setPositiveButton("Get Started") { _, _ ->
                navigateToHome()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Login Failed")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun setLoading(loading: Boolean) {
        progressBar.isVisible = loading
        loginButton.isEnabled = !loading
        usernameEditText.isEnabled = !loading
        passwordEditText.isEnabled = !loading
    }
    
    private fun navigateToHome() {
        val intent = Intent(this, UserDetailsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
