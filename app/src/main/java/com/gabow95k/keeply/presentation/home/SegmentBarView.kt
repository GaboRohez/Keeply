package com.gabow95k.keeply.presentation.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.gabow95k.keeply.R
import kotlin.math.max

/**
 * Horizontal stacked bar for monthly movement mix (consume / adjust / add).
 */
class SegmentBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.keeply_surface_variant)
    }
    private val consumePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.keeply_primary)
    }
    private val adjustPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.keeply_warning)
    }
    private val addPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.keeply_success)
    }
    private val rect = RectF()

    private var consumeShare = 0f
    private var adjustShare = 0f
    private var addShare = 0f

    fun setShares(consume: Float, adjust: Float, add: Float) {
        consumeShare = consume.coerceIn(0f, 1f)
        adjustShare = adjust.coerceIn(0f, 1f)
        addShare = add.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = height / 2f
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, trackPaint)

        val total = max(consumeShare + adjustShare + addShare, 0.0001f)
        var start = 0f
        fun drawSegment(share: Float, paint: Paint) {
            if (share <= 0f) return
            val segmentWidth = width * (share / total)
            rect.set(start, 0f, start + segmentWidth, height.toFloat())
            canvas.drawRoundRect(rect, radius, radius, paint)
            start += segmentWidth
        }
        drawSegment(consumeShare, consumePaint)
        drawSegment(adjustShare, adjustPaint)
        drawSegment(addShare, addPaint)
    }
}
