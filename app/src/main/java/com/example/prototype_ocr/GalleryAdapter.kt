package com.example.prototype_ocr

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class GalleryAdapter(
    private var images: MutableList<File>,
    private val onClick: (File) -> Unit,
    private val onDelete: (File) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val deleteButton: ImageView = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val file = images[position]
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        holder.imageView.setImageBitmap(bitmap)

        holder.imageView.setOnClickListener {
            onClick(file)
        }

        holder.deleteButton.setOnClickListener {
            onDelete(file)
        }
    }

    override fun getItemCount() = images.size

    fun removeImage(file: File) {
        val position = images.indexOf(file)
        if (position != -1) {
            images.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
