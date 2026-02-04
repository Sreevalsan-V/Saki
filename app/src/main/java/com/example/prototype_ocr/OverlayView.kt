package com.example.prototype_ocr

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Crop region configuration
 * Coordinates are relative to parent bounding box (0.0 to 1.0)
 */
data class CropRegion(
    val name: String,
    var x: Float,        // X position (0.0 = left, 1.0 = right)
    var y: Float,        // Y position (0.0 = top, 1.0 = bottom)
    var width: Float,    // Width (0.0 to 1.0)
    var height: Float,   // Height (0.0 to 1.0)
    var color: Int = Color.GREEN,
    var enabled: Boolean = true
)

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.RED
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }
    
    private val stripPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    
    private val cropPaint = Paint().apply {
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    
    private val labelPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val labelBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        alpha = 180
    }

    private val boxRect = RectF()
    var showStrips = false // Control strip visualization
    var numberOfStrips = 5 // Default number of horizontal strips
    
    // Crop regions for Robonik (editable)
    private val cropRegions = mutableListOf<CropRegion>()
    
    // Default Robonik crop regions
    private val defaultRobonikCrops = listOf(
        CropRegion(
            name = "Test Type",
            x = 0.35f,        // Top-center horizontally
            y = 0.05f,        // Top of parent box
            width = 0.30f,    // 30% width
            height = 0.25f,   // 25% height
            color = Color.CYAN
        ),
        CropRegion(
            name = "mg/dL Value",
            x = 0.05f,        // Left side
            y = 0.35f,        // Middle vertically
            width = 0.40f,    // 40% width
            height = 0.30f,   // 30% height
            color = Color.YELLOW
        )
    )
    
    init {
        // Initialize with default crops (disabled by default)
        cropRegions.addAll(defaultRobonikCrops.map { it.copy(enabled = false) })
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw main bounding box
        boxRect.set(
            width * 0.1f,
            height * 0.15f,
            width * 0.75f,
            height * 0.85f
        )

        canvas.drawRect(boxRect, paint)
        
        // Draw horizontal strip dividers if enabled
        if (showStrips) {
            val stripHeight = boxRect.height() / numberOfStrips
            for (i in 1 until numberOfStrips) {
                val y = boxRect.top + (stripHeight * i)
                canvas.drawLine(boxRect.left, y, boxRect.right, y, stripPaint)
            }
        }
        
        // Draw crop regions if enabled
        for (crop in cropRegions) {
            if (crop.enabled) {
                drawCropRegion(canvas, crop)
            }
        }
    }
    
    private fun drawCropRegion(canvas: Canvas, crop: CropRegion) {
        // Calculate absolute coordinates within parent box
        val cropRect = RectF(
            boxRect.left + (boxRect.width() * crop.x),
            boxRect.top + (boxRect.height() * crop.y),
            boxRect.left + (boxRect.width() * (crop.x + crop.width)),
            boxRect.top + (boxRect.height() * (crop.y + crop.height))
        )
        
        // Draw crop rectangle
        cropPaint.color = crop.color
        canvas.drawRect(cropRect, cropPaint)
        
        // Draw label with background
        val labelText = crop.name
        val textBounds = android.graphics.Rect()
        labelPaint.getTextBounds(labelText, 0, labelText.length, textBounds)
        
        val labelX = cropRect.left + 8f
        val labelY = cropRect.top - 8f
        
        // Draw label background
        labelBackgroundPaint.color = crop.color
        canvas.drawRect(
            labelX - 4f,
            labelY - textBounds.height() - 4f,
            labelX + textBounds.width() + 4f,
            labelY + 4f,
            labelBackgroundPaint
        )
        
        // Draw label text
        labelPaint.color = Color.BLACK
        canvas.drawText(labelText, labelX, labelY, labelPaint)
    }

    fun getBoxRect(): RectF {
        return RectF(boxRect)
    }
    
    fun enableStripVisualization(enabled: Boolean, strips: Int = 5) {
        showStrips = enabled
        numberOfStrips = strips
        invalidate()
    }
    
    /**
     * Enable/disable crop region visualization for Robonik
     */
    fun enableCropRegions(enabled: Boolean) {
        cropRegions.forEach { it.enabled = enabled }
        invalidate()
    }
    
    /**
     * Get all enabled crop regions with absolute coordinates
     * Returns list of maps with pixel coordinates
     */
    fun getCropRegions(): List<Map<String, Any>> {
        return cropRegions.filter { it.enabled }.map { crop ->
            mapOf(
                "name" to crop.name,
                "x" to crop.x,
                "y" to crop.y,
                "width" to crop.width,
                "height" to crop.height
            )
        }
    }
    
    /**
     * Update a specific crop region
     */
    fun updateCropRegion(name: String, x: Float, y: Float, width: Float, height: Float) {
        cropRegions.find { it.name == name }?.apply {
            this.x = x.coerceIn(0f, 1f)
            this.y = y.coerceIn(0f, 1f)
            this.width = width.coerceIn(0.1f, 1f)
            this.height = height.coerceIn(0.1f, 1f)
            invalidate()
        }
    }
    
    /**
     * Add custom crop region
     */
    fun addCropRegion(
        name: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Int = Color.GREEN
    ) {
        cropRegions.add(
            CropRegion(
                name = name,
                x = x.coerceIn(0f, 1f),
                y = y.coerceIn(0f, 1f),
                width = width.coerceIn(0.1f, 1f),
                height = height.coerceIn(0.1f, 1f),
                color = color,
                enabled = true
            )
        )
        invalidate()
    }
    
    /**
     * Reset to default Robonik crops
     */
    fun resetToDefaultCrops() {
        cropRegions.clear()
        cropRegions.addAll(defaultRobonikCrops.map { it.copy() })
        invalidate()
    }
    
    /**
     * Clear all crop regions
     */
    fun clearCropRegions() {
        cropRegions.clear()
        invalidate()
    }
}


