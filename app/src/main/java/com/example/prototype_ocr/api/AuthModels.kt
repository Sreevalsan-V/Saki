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
    val healthCenter: String?,
    val district: String?,
    val state: String?
)

// Auth API Response wrapper
data class AuthApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String,
    val timestamp: Long
)
