package com.example.prototype_ocr

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

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

    private val boxRect = RectF()
    var showStrips = false // Control strip visualization
    var numberOfStrips = 5 // Default number of horizontal strips

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

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
    }

    fun getBoxRect(): RectF {
        return RectF(boxRect)
    }
    
    fun enableStripVisualization(enabled: Boolean, strips: Int = 5) {
        showStrips = enabled
        numberOfStrips = strips
        invalidate()
    }
}

