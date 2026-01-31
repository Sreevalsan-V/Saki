package com.example.prototype_ocr

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prototype_ocr.data.UploadManager

class PastUploadsActivity : AppCompatActivity() {
    
    private lateinit var uploadManager: UploadManager
    private lateinit var adapter: PastUploadsAdapter
    private lateinit var emptyStateText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_past_uploads)
        
        uploadManager = UploadManager(this)
        emptyStateText = findViewById(R.id.emptyStateText)
        
        val recyclerView = findViewById<RecyclerView>(R.id.uploadsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = PastUploadsAdapter { upload ->
            val intent = Intent(this, UploadDetailActivity::class.java)
            intent.putExtra("upload_id", upload.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        
        loadUploads()
    }
    
    override fun onResume() {
        super.onResume()
        loadUploads()
    }
    
    private fun loadUploads() {
        val uploads = uploadManager.getUploadsSortedByTime()
        
        if (uploads.isEmpty()) {
            emptyStateText.visibility = View.VISIBLE
            emptyStateText.text = "No uploads yet"
        } else {
            emptyStateText.visibility = View.GONE
            adapter.updateUploads(uploads)
        }
    }
}
