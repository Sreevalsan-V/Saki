package com.example.prototype_ocr.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    
    // Server Configuration
    // For emulator: http://10.0.2.2:3000
    // For real device (same network): http://192.168.1.103:3000 (your computer's local IP)
    // For production: Your deployed server URL
    private const val BASE_URL = "http://192.168.1.103:3000"  // ← Updated for real device
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val api: MedicalOcrApi = retrofit.create(MedicalOcrApi::class.java)
    
    /**
     * Update base URL at runtime if needed
     */
    fun createApiWithBaseUrl(baseUrl: String): MedicalOcrApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MedicalOcrApi::class.java)
    }
}
