package com.gabow95k.keeply.presentation.botiquin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.gabow95k.keeply.R
import com.gabow95k.keeply.util.ImageCropMath

class ProductPhotoCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null
    private var baseScale = 1f
    private var zoom = 1f
    private var imageCenterX = 0f
    private var imageCenterY = 0f
    private val imageMatrix = Matrix()
    private val cropBounds = RectF()
    private val cropPath = Path()
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2f
        color = context.getColor(R.color.keeply_primary)
    }
    private val cornerRadius = resources.displayMetrics.density * 20f

    var onZoomChanged: ((Float) -> Unit)? = null

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent): Boolean = true

            override fun onScroll(
                firstEvent: MotionEvent?,
                currentEvent: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                imageCenterX -= distanceX
                imageCenterY -= distanceY
                constrainImage()
                invalidate()
                return true
            }
        }
    )

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val previousZoom = zoom
                val nextZoom = (zoom * detector.scaleFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)
                if (nextZoom == previousZoom) return true

                val ratio = nextZoom / previousZoom
                imageCenterX = detector.focusX - (detector.focusX - imageCenterX) * ratio
                imageCenterY = detector.focusY - (detector.focusY - imageCenterY) * ratio
                zoom = nextZoom
                constrainImage()
                onZoomChanged?.invoke(zoom)
                invalidate()
                return true
            }
        }
    )

    init {
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        setBackgroundColor(context.getColor(R.color.keeply_surface_variant))
    }

    fun setBitmap(value: Bitmap) {
        bitmap = value
        resetTransform()
    }

    fun setZoom(value: Float) {
        val nextZoom = value.coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (nextZoom == zoom) return
        val ratio = nextZoom / zoom
        val focusX = width / 2f
        val focusY = height / 2f
        imageCenterX = focusX - (focusX - imageCenterX) * ratio
        imageCenterY = focusY - (focusY - imageCenterY) * ratio
        zoom = nextZoom
        constrainImage()
        invalidate()
    }

    fun resetTransform() {
        val source = bitmap ?: return
        if (width == 0 || height == 0) {
            post(::resetTransform)
            return
        }
        baseScale = ImageCropMath.coverScale(
            width.toFloat(),
            height.toFloat(),
            source.width.toFloat(),
            source.height.toFloat()
        )
        zoom = MIN_ZOOM
        imageCenterX = width / 2f
        imageCenterY = height / 2f
        onZoomChanged?.invoke(zoom)
        invalidate()
    }

    fun renderCrop(targetWidth: Int, targetHeight: Int): Bitmap? {
        val source = bitmap ?: return null
        if (width <= 0 || height <= 0 || targetWidth <= 0 || targetHeight <= 0) return null

        updateImageMatrix(source)
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        canvas.scale(targetWidth / width.toFloat(), targetHeight / height.toFloat())
        canvas.drawBitmap(source, imageMatrix, imagePaint)
        return output
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        cropBounds.set(0f, 0f, width.toFloat(), height.toFloat())
        cropPath.reset()
        cropPath.addRoundRect(cropBounds, cornerRadius, cornerRadius, Path.Direction.CW)
        if (bitmap != null) resetTransform()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val source = bitmap ?: return
        updateImageMatrix(source)

        val checkpoint = canvas.save()
        canvas.clipPath(cropPath)
        canvas.drawBitmap(source, imageMatrix, imagePaint)
        canvas.restoreToCount(checkpoint)
        canvas.drawRoundRect(cropBounds, cornerRadius, cornerRadius, borderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            parent?.requestDisallowInterceptTouchEvent(false)
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun updateImageMatrix(source: Bitmap) {
        val scale = baseScale * zoom
        imageMatrix.reset()
        imageMatrix.postScale(scale, scale)
        imageMatrix.postTranslate(
            imageCenterX - source.width * scale / 2f,
            imageCenterY - source.height * scale / 2f
        )
    }

    private fun constrainImage() {
        val source = bitmap ?: return
        val scale = baseScale * zoom
        imageCenterX = ImageCropMath.clampCenter(
            imageCenterX,
            width.toFloat(),
            source.width * scale
        )
        imageCenterY = ImageCropMath.clampCenter(
            imageCenterY,
            height.toFloat(),
            source.height * scale
        )
    }

    companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 4f
    }
}
