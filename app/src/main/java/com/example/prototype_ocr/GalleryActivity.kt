package com.example.prototype_ocr

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import java.io.File

class GalleryActivity : AppCompatActivity() {

    private lateinit var adapter: GalleryAdapter
    private lateinit var images: MutableList<File>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        val imageDir = File(filesDir, "lcd_images")
        images = imageDir.listFiles()?.toMutableList() ?: mutableListOf()

        adapter = GalleryAdapter(
            images,
            onClick = { file ->
                val intent = Intent(this, ImageViewActivity::class.java)
                intent.putExtra("image_path", file.absolutePath)
                startActivity(intent)
            },
            onDelete = { file ->
                deleteImage(file)
            }
        )

        recyclerView.adapter = adapter

    }

    private fun deleteImage(file: File) {
        if (file.exists()) {
            file.delete()
        }
        adapter.removeImage(file)
    }
}
