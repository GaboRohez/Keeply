package com.gabow95k.keeply.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageCropMathTest {

    @Test
    fun coverScale_fillsViewportWithoutEmptyEdges() {
        assertEquals(2f, ImageCropMath.coverScale(400f, 200f, 200f, 200f), 0f)
        assertEquals(2f, ImageCropMath.coverScale(200f, 400f, 200f, 200f), 0f)
        assertEquals(1f, ImageCropMath.coverScale(400f, 200f, 800f, 200f), 0f)
    }

    @Test
    fun clampCenter_keepsScaledImageCoveringViewport() {
        assertEquals(300f, ImageCropMath.clampCenter(999f, 400f, 600f), 0f)
        assertEquals(100f, ImageCropMath.clampCenter(-999f, 400f, 600f), 0f)
        assertEquals(200f, ImageCropMath.clampCenter(250f, 400f, 300f), 0f)
    }
}
