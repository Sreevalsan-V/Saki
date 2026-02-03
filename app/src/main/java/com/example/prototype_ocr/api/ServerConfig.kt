package com.example.prototype_ocr.api

/**
 * Server Configuration
 * Easy way to switch between different server URLs
 * 
 * Instructions:
 * 1. Change `currentEnvironment` to switch servers
 * 2. Update NGROK_URL when you restart ngrok
 */
object ServerConfig {
    
    enum class Environment {
        LOCAL_EMULATOR,    // Use when running on Android Emulator
        LOCAL_DEVICE,      // Use when testing on real device (same WiFi)
        NGROK,             // Use for remote testing with ngrok
        PRODUCTION         // Use for deployed backend
    }
    
    // ====== CHANGE THIS TO SWITCH ENVIRONMENTS ======
    private val currentEnvironment = Environment.NGROK
    // ================================================
    
    // Server URLs
    private const val EMULATOR_URL = "http://10.0.2.2:3000"
    private const val LOCAL_DEVICE_URL = "http://192.168.1.103:3000"
    
    // ⚠️ UPDATE THIS EVERY TIME YOU RESTART NGROK ⚠️
    private const val NGROK_URL = "https://dinkly-nonchurched-kelle.ngrok-free.dev/"
    
    private const val PRODUCTION_URL = "https://your-production-server.com"
    
    /**
     * Get the current base URL based on environment
     */
    val baseUrl: String
        get() = when (currentEnvironment) {
            Environment.LOCAL_EMULATOR -> EMULATOR_URL
            Environment.LOCAL_DEVICE -> LOCAL_DEVICE_URL
            Environment.NGROK -> NGROK_URL
            Environment.PRODUCTION -> PRODUCTION_URL
        }
    
    /**
     * Check if using local server (for debugging)
     */
    val isLocalServer: Boolean
        get() = currentEnvironment in listOf(
            Environment.LOCAL_EMULATOR,
            Environment.LOCAL_DEVICE
        )
    
    /**
     * Get environment name for logging
     */
    val environmentName: String
        get() = currentEnvironment.name
}
