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

    private val boxRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        boxRect.set(
            width * 0.1f,
            height * 0.15f,
            width * 0.75f,
            height * 0.85f
        )

        canvas.drawRect(boxRect, paint)
    }

    fun getBoxRect(): RectF {
        return RectF(boxRect)
    }
}

