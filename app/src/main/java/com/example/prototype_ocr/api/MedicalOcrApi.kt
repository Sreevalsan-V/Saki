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
     * Get current user data (refresh profile)
     */
    @GET("/api/auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<AuthApiResponse<UserData>>
    
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
        @Query("userId") userId: String? = null,
        @Query("panelId") panelId: String? = null,
        @Query("month") month: String? = null,
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
        @Query("userId") userId: String? = null,
        @Query("panelId") panelId: String? = null
    ): Response<ApiResponse<Map<String, Any>>>
    
    // ========== SERVER OCR ENDPOINTS (Optional Module) ==========
    
    /**
     * Check if OCR service is available
     */
    @GET("/api/ocr/health")
    suspend fun checkOcrHealth(): Response<ApiResponse<Map<String, Any>>>
    
    /**
     * Process full image OCR on server
     */
    @POST("/api/ocr/process")
    suspend fun processServerOcr(
        @Header("Authorization") token: String,
        @Body request: ServerOcrRequest
    ): Response<ApiResponse<ServerOcrResult>>
    
    /**
     * Process cropped regions OCR on server
     */
    @POST("/api/ocr/crop-and-process")
    suspend fun processServerCroppedOcr(
        @Header("Authorization") token: String,
        @Body request: ServerCroppedOcrRequest
    ): Response<ApiResponse<ServerCroppedOcrResult>>
}
