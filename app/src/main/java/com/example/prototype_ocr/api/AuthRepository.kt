package com.example.prototype_ocr.api

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val api = ApiClient.api
    
    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_DATA = "user_data"
        private const val KEY_TOKEN = "token"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD_HASH = "password_hash"
    }
    
    /**
     * Login user - first checks offline credentials, then tries online if needed
     */
    suspend fun login(username: String, password: String, forceOnline: Boolean = false): Result<UserData> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if this is first login or force online
                if (forceOnline || !isUserDataCached(username)) {
                    // Online login - fetch from server
                    loginOnline(username, password)
                } else {
                    // Offline login - verify cached credentials
                    loginOffline(username, password)
                }
            } catch (e: Exception) {
                Result.failure<UserData>(e)
            }
        }
    }
    
    /**
     * Online login - authenticate with server and cache data
     */
    private suspend fun loginOnline(username: String, password: String): Result<UserData> {
        return try {
            val request = LoginRequest(username, password)
            val response = api.login(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val loginResponse = response.body()?.data
                if (loginResponse != null) {
                    // Save user data and credentials
                    saveUserData(loginResponse.user)
                    saveToken(loginResponse.token)
                    saveCredentials(username, password)
                    Result.success<UserData>(loginResponse.user)
                } else {
                    Result.failure<UserData>(Exception("Invalid response from server"))
                }
            } else {
                val errorMsg = response.body()?.message ?: "Login failed"
                Result.failure<UserData>(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure<UserData>(Exception("Network error: ${e.message}"))
        }
    }
    
    /**
     * Offline login - verify against cached credentials
     */
    private fun loginOffline(username: String, password: String): Result<UserData> {
        val savedUsername = prefs.getString(KEY_USERNAME, null)
        val savedPasswordHash = prefs.getString(KEY_PASSWORD_HASH, null)
        
        if (savedUsername == username && savedPasswordHash == hashPassword(password)) {
            val userData = getCachedUserData()
            return if (userData != null) {
                prefs.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply()
                Result.success<UserData>(userData)
            } else {
                Result.failure<UserData>(Exception("User data not found. Please login with internet."))
            }
        } else {
            return Result.failure<UserData>(Exception("Invalid credentials. Please check your username and password."))
        }
    }
    
    /**
     * Save user data to SharedPreferences
     */
    private fun saveUserData(userData: UserData) {
        val userJson = gson.toJson(userData)
        prefs.edit()
            .putString(KEY_USER_DATA, userJson)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }
    
    /**
     * Save authentication token
     */
    private fun saveToken(token: String?) {
        if (token != null) {
            prefs.edit().putString(KEY_TOKEN, token).apply()
        }
    }
    
    /**
     * Save credentials for offline login
     */
    private fun saveCredentials(username: String, password: String) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD_HASH, hashPassword(password))
            .apply()
    }
    
    /**
     * Simple password hashing (use BCrypt or similar in production)
     */
    private fun hashPassword(password: String): String {
        return password.hashCode().toString()
    }
    
    /**
     * Check if user data is cached for username - public for LoginActivity
     */
    fun isUserDataCached(username: String): Boolean {
        val savedUsername = prefs.getString(KEY_USERNAME, null)
        val userData = prefs.getString(KEY_USER_DATA, null)
        return savedUsername == username && userData != null
    }
    
    /**
     * Get cached user data
     */
    fun getCachedUserData(): UserData? {
        val userJson = prefs.getString(KEY_USER_DATA, null) ?: return null
        return try {
            gson.fromJson(userJson, UserData::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get authentication token
     */
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    /**
     * Logout user
     */
    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_TOKEN)
            .apply()
    }
    
    /**
     * Clear all user data (for complete logout)
     */
    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}
