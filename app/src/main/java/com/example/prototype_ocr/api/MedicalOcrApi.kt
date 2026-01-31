package com.example.prototype_ocr.api

import retrofit2.Response
import retrofit2.http.*

interface MedicalOcrApi {
    
    /**
     * Login - Authenticate user
     */
    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthApiResponse<LoginResponse>>
    
    /**
     * Get user profile
     */
    @GET("/api/auth/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<AuthApiResponse<UserData>>
    
    /**
     * Upload test records with images and PDF to server
     */
    @POST("/api/upload")
    suspend fun uploadTestRecords(
        @Body request: UploadRequest
    ): Response<ApiResponse<UploadResponse>>
    
    /**
     * Get all uploads (optional - for future use)
     */
    @GET("/api/uploads")
    suspend fun getUploads(
        @Query("deviceId") deviceId: String? = null,
        @Query("startDate") startDate: Long? = null,
        @Query("endDate") endDate: Long? = null
    ): Response<ApiResponse<List<UploadResponse>>>
    
    /**
     * Get specific upload by ID (optional - for future use)
     */
    @GET("/api/upload/{id}")
    suspend fun getUploadById(
        @Path("id") uploadId: String
    ): Response<ApiResponse<UploadResponse>>
    
    /**
     * Delete upload (optional - for future use)
     */
    @DELETE("/api/upload/{id}")
    suspend fun deleteUpload(
        @Path("id") uploadId: String
    ): Response<ApiResponse<Unit>>
    
    /**
     * Get statistics (optional - for future use)
     */
    @GET("/api/stats")
    suspend fun getStatistics(
        @Query("deviceId") deviceId: String? = null,
        @Query("startDate") startDate: Long? = null,
        @Query("endDate") endDate: Long? = null
    ): Response<ApiResponse<Map<String, Any>>>
}
