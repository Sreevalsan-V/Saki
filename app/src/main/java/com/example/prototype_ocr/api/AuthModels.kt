package com.example.prototype_ocr.api

import com.google.gson.annotations.SerializedName

// Login Request
data class LoginRequest(
    val username: String,
    val password: String
)

// Login Response
data class LoginResponse(
    val token: String?,
    val user: UserData
)

// User Data
data class UserData(
    val id: String,
    val username: String,
    val name: String,
    val role: String,
    val email: String?,
    val phoneNumber: String?,
    @SerializedName("phcName") val phcName: String?,
    @SerializedName("hubName") val hubName: String?,
    @SerializedName("blockName") val blockName: String?,
    @SerializedName("districtName") val districtName: String?,
    val state: String?,
    // Legacy fields from backend for backward compatibility
    @SerializedName("healthCenter") val healthCenter: String?,
    val district: String?
)

// Auth API Response wrapper
data class AuthApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String,
    val timestamp: Long
)
