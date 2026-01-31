package com.example.prototype_ocr.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UploadManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("uploads_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveUpload(upload: Upload): Boolean {
        return try {
            val uploads = getAllUploads().toMutableList()
            uploads.add(upload)
            val json = gson.toJson(uploads)
            prefs.edit().putString("uploads", json).apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun getAllUploads(): List<Upload> {
        return try {
            val json = prefs.getString("uploads", null) ?: return emptyList()
            val type = object : TypeToken<List<Upload>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun getUploadsSortedByTime(): List<Upload> {
        return getAllUploads().sortedByDescending { it.uploadTimestamp }
    }
    
    fun getUploadById(id: String): Upload? {
        return getAllUploads().firstOrNull { it.id == id }
    }
    
    fun deleteUpload(id: String): Boolean {
        return try {
            val uploads = getAllUploads().toMutableList()
            uploads.removeAll { it.id == id }
            val json = gson.toJson(uploads)
            prefs.edit().putString("uploads", json).apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
