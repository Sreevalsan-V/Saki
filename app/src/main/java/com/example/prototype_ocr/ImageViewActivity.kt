package com.example.prototype_ocr


import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class ImageViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)

        val imagePath = intent.getStringExtra("image_path") ?: return

        val bitmap = BitmapFactory.decodeFile(imagePath)
        findViewById<ImageView>(R.id.fullImageView).setImageBitmap(bitmap)
    }
}
