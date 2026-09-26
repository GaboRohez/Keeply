package com.gabow95k.keeply.util

import kotlin.math.max

object ImageCropMath {

    fun coverScale(
        viewportWidth: Float,
        viewportHeight: Float,
        imageWidth: Float,
        imageHeight: Float
    ): Float {
        if (viewportWidth <= 0f || viewportHeight <= 0f || imageWidth <= 0f || imageHeight <= 0f) {
            return 1f
        }
        return max(viewportWidth / imageWidth, viewportHeight / imageHeight)
    }

    fun clampCenter(center: Float, viewportSize: Float, scaledImageSize: Float): Float {
        if (viewportSize <= 0f || scaledImageSize <= viewportSize) return viewportSize / 2f
        val halfImage = scaledImageSize / 2f
        return center.coerceIn(viewportSize - halfImage, halfImage)
    }
}
