package com.example.prototype_ocr.api

import android.content.Context
import android.content.SharedPreferences

/**
 * OCR Configuration Manager
 * Allows switching between on-device ML Kit and server-side EasyOCR
 */
object OcrConfig {
    
    enum class OcrMode {
        ML_KIT,        // On-device ML Kit OCR (default)
        SERVER_FULL,   // Server-side OCR for full image
        SERVER_CROPS   // Server-side OCR for cropped regions
    }
    
    private const val PREFS_NAME = "ocr_config"
    private const val KEY_OCR_MODE = "ocr_mode"
    private const val KEY_USE_CROP_REGIONS = "use_crop_regions"
    private const val KEY_TEST_TYPE_CROP_ENABLED = "test_type_crop_enabled"
    private const val KEY_VALUE_CROP_ENABLED = "value_crop_enabled"
    
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Get current OCR mode
     */
    var ocrMode: OcrMode
        get() {
            val mode = prefs.getString(KEY_OCR_MODE, OcrMode.ML_KIT.name) ?: OcrMode.ML_KIT.name
            return try {
                OcrMode.valueOf(mode)
            } catch (e: IllegalArgumentException) {
                OcrMode.ML_KIT
            }
        }
        set(value) {
            prefs.edit().putString(KEY_OCR_MODE, value.name).apply()
        }
    
    /**
     * Whether to use crop regions for Robonik
     */
    var useCropRegions: Boolean
        get() = prefs.getBoolean(KEY_USE_CROP_REGIONS, false)
        set(value) {
            prefs.edit().putBoolean(KEY_USE_CROP_REGIONS, value).apply()
        }
    
    /**
     * Whether test type crop region is enabled
     */
    var testTypeCropEnabled: Boolean
        get() = prefs.getBoolean(KEY_TEST_TYPE_CROP_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_TEST_TYPE_CROP_ENABLED, value).apply()
        }
    
    /**
     * Whether mg/dL value crop region is enabled
     */
    var valueCropEnabled: Boolean
        get() = prefs.getBoolean(KEY_VALUE_CROP_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_VALUE_CROP_ENABLED, value).apply()
        }
    
    /**
     * Check if using server OCR
     */
    val isServerOcr: Boolean
        get() = ocrMode != OcrMode.ML_KIT
    
    /**
     * Check if using cropped server OCR
     */
    val isServerCroppedOcr: Boolean
        get() = ocrMode == OcrMode.SERVER_CROPS
}
