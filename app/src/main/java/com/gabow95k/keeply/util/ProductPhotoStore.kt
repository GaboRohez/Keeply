package com.gabow95k.keeply.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object ProductPhotoStore {

    private const val DIR_NAME = "product_photos"
    private const val AUTHORITY_SUFFIX = ".fileprovider"

    fun authority(context: Context): String = "${context.packageName}$AUTHORITY_SUFFIX"

    fun createPhotoFile(context: Context): File {
        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        return File(dir, "product_${System.currentTimeMillis()}.jpg")
    }

    fun uriFor(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, authority(context), file)
    }

    fun decodeForCrop(context: Context, uri: Uri): Bitmap {
        val source = if (uri.scheme == "file") {
            val path = requireNotNull(uri.path)
            ImageDecoder.createSource(File(path))
        } else {
            ImageDecoder.createSource(context.contentResolver, uri)
        }
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val width = info.size.width
            val height = info.size.height
            val longestEdge = maxOf(width, height)
            if (longestEdge > MAX_DECODE_DIMENSION) {
                val ratio = MAX_DECODE_DIMENSION / longestEdge.toFloat()
                decoder.setTargetSize(
                    (width * ratio).roundToInt().coerceAtLeast(1),
                    (height * ratio).roundToInt().coerceAtLeast(1)
                )
            }
        }
    }

    fun saveCroppedBitmap(context: Context, bitmap: Bitmap): File {
        val file = createPhotoFile(context)
        try {
            FileOutputStream(file).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output))
            }
            return file
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
    }

    fun deleteIfOwned(context: Context, path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        val photosDir = File(context.filesDir, DIR_NAME).canonicalFile
        val canonical = runCatching { file.canonicalFile }.getOrNull() ?: return
        if (canonical.path.startsWith(photosDir.path) && canonical.exists()) {
            canonical.delete()
        }
    }

    private const val MAX_DECODE_DIMENSION = 4096
    private const val JPEG_QUALITY = 92
}
