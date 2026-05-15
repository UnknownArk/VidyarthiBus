package com.vidyarthibus

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.vidyarthibus.model.RouteStatus

class CrowdMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    var status: RouteStatus = RouteStatus.UNKNOWN
        set(value) {
            field = value
            invalidate()
        }

    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = sp(12)
        isFakeBoldText = true
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF123047.toInt()
        style = Paint.Style.FILL
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000
        style = Paint.Style.FILL
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = dp(82)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize(desiredHeight, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        if (width <= 0f) return

        val barTop = dp(26).toFloat()
        val barHeight = dp(30).toFloat()
        val barBottom = barTop + barHeight
        val radius = dp(16).toFloat()
        val fullBar = RectF(0f, barTop, width, barBottom)
        val segmentWidth = width / 3f

        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(fullBar, radius, radius, Path.Direction.CW) })
        drawSegment(canvas, 0f, segmentWidth, barTop, barBottom, 0xFF2E7D32.toInt(), "Empty")
        drawSegment(canvas, segmentWidth, segmentWidth * 2f, barTop, barBottom, 0xFFF9A825.toInt(), "Seated")
        drawSegment(canvas, segmentWidth * 2f, width, barTop, barBottom, 0xFFC62828.toInt(), "Full")
        canvas.restore()

        val markerX = when (status) {
            RouteStatus.EMPTY -> segmentWidth / 2f
            RouteStatus.SEATED -> segmentWidth * 1.5f
            RouteStatus.FULL -> segmentWidth * 2.5f
            RouteStatus.UNKNOWN -> width / 2f
        }
        drawMarker(canvas, markerX, barTop)
    }

    private fun drawSegment(canvas: Canvas, left: Float, right: Float, top: Float, bottom: Float, color: Int, label: String) {
        segmentPaint.color = color
        canvas.drawRect(left, top, right, bottom, segmentPaint)
        val centerY = top + ((bottom - top) / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(label, (left + right) / 2f, centerY, textPaint)
    }

    private fun drawMarker(canvas: Canvas, centerX: Float, barTop: Float) {
        val circleRadius = dp(8).toFloat()
        canvas.drawCircle(centerX + dp(1), dp(12).toFloat() + dp(1), circleRadius, shadowPaint)
        canvas.drawCircle(centerX, dp(12).toFloat(), circleRadius, markerPaint)

        val triangle = Path().apply {
            moveTo(centerX, barTop - dp(2))
            lineTo(centerX - dp(8), barTop - dp(12))
            lineTo(centerX + dp(8), barTop - dp(12))
            close()
        }
        canvas.drawPath(triangle, markerPaint)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun sp(value: Int): Float = value * resources.displayMetrics.scaledDensity
}
