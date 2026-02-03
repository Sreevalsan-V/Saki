package com.example.prototype_ocr.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val api = ApiClient.api
    
    companion object {
        private const val TAG = "AuthRepository"
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
            Log.d(TAG, "Sending login request for: $username")
            
            val response = api.login(request)
            
            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Response successful: ${response.isSuccessful}")
            Log.d(TAG, "Response body: ${response.body()}")
            
            if (response.isSuccessful && response.body()?.success == true) {
                val loginResponse = response.body()?.data
                if (loginResponse != null) {
                    val user = loginResponse.user
                    
                    // Log exactly what backend sent
                    Log.d(TAG, "=== BACKEND RESPONSE DATA ===")
                    Log.d(TAG, "Raw JSON: ${gson.toJson(user)}")
                    Log.d(TAG, "User ID: '${user.id}'")
                    Log.d(TAG, "User Name: '${user.name}'")
                    Log.d(TAG, "PHC Name from backend: '${user.phcName}'")
                    Log.d(TAG, "Hub Name from backend: '${user.hubName}'")
                    Log.d(TAG, "Block Name from backend: '${user.blockName}'")
                    Log.d(TAG, "District Name from backend: '${user.districtName}'")
                    Log.d(TAG, "State from backend: '${user.state}'")
                    Log.d(TAG, "===========================")
                    
                    // Save user data and credentials
                    saveUserData(user)
                    saveToken(loginResponse.token)
                    saveCredentials(username, password)
                    
                    Log.d(TAG, "User data saved successfully")
                    Result.success<UserData>(user)
                } else {
                    Log.e(TAG, "Login response data is null")
                    Result.failure<UserData>(Exception("Invalid response from server"))
                }
            } else {
                val errorMsg = response.body()?.message ?: "Login failed"
                Log.e(TAG, "Login failed: $errorMsg")
                Result.failure<UserData>(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login exception", e)
            Result.failure<UserData>(Exception("Network error: ${e.message}"))
        }
    }
    
    /**
     * Offline login - verify cached credentials
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
        Log.d(TAG, "Saving user data to SharedPreferences:")
        Log.d(TAG, "JSON being saved: $userJson")
        prefs.edit()
            .putString(KEY_USER_DATA, userJson)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
        Log.d(TAG, "User data saved to SharedPreferences")
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
     * Refresh user data from server (silent background update)
     */
    suspend fun refreshUserData(): Result<UserData> {
        return withContext(Dispatchers.IO) {
            try {
                val token = getToken()
                if (token == null) {
                    Log.w(TAG, "No token available for refresh")
                    return@withContext Result.failure<UserData>(Exception("Not logged in"))
                }
                
                Log.d(TAG, "Refreshing user data from server...")
                
                val response = api.getCurrentUser("Bearer $token")
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val userData = response.body()?.data
                    if (userData != null) {
                        Log.d(TAG, "User data refreshed successfully")
                        Log.d(TAG, "PHC: '${userData.phcName}', Hub: '${userData.hubName}', Block: '${userData.blockName}', District: '${userData.districtName}'")
                        
                        // Update cached data
                        saveUserData(userData)
                        
                        Result.success(userData)
                    } else {
                        Result.failure<UserData>(Exception("No data in response"))
                    }
                } else {
                    val errorMsg = response.body()?.message ?: "Failed to refresh"
                    Log.e(TAG, "Refresh failed: $errorMsg")
                    
                    // Return cached data as fallback
                    val cached = getCachedUserData()
                    if (cached != null) {
                        Result.success(cached)
                    } else {
                        Result.failure<UserData>(Exception(errorMsg))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing user data", e)
                
                // Return cached data as fallback
                val cached = getCachedUserData()
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.failure<UserData>(e)
                }
            }
        }
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
        val userJson = prefs.getString(KEY_USER_DATA, null)
        Log.d(TAG, "Retrieving cached user data")
        Log.d(TAG, "Cached JSON: $userJson")
        
        if (userJson == null) {
            Log.w(TAG, "No cached user data found")
            return null
        }
        
        return try {
            val userData = gson.fromJson(userJson, UserData::class.java)
            Log.d(TAG, "=== CACHED USER DATA ===")
            Log.d(TAG, "PHC Name from cache: '${userData.phcName}'")
            Log.d(TAG, "Hub Name from cache: '${userData.hubName}'")
            Log.d(TAG, "Block Name from cache: '${userData.blockName}'")
            Log.d(TAG, "District Name from cache: '${userData.districtName}'")
            Log.d(TAG, "=======================")
            userData
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing cached user data", e)
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
