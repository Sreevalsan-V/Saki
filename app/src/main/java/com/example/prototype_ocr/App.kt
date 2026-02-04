package com.example.prototype_ocr

import android.app.Application
import com.example.prototype_ocr.api.OcrConfig

/**
 * Application class - Initialize OCR configuration
 * 
 * Add to AndroidManifest.xml:
 * <application
 *     android:name=".App"
 *     ...>
 */
class App : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize OcrConfig
        OcrConfig.init(this)
        
        // Optional: Set default OCR mode
        // OcrConfig.ocrMode = OcrConfig.OcrMode.ML_KIT  // Default
        // OcrConfig.ocrMode = OcrConfig.OcrMode.SERVER_CROPS  // Production
    }
}
