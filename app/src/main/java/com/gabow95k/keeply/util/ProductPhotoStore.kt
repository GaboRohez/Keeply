package com.gabow95k.keeply.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

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

    fun deleteIfOwned(context: Context, path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        val photosDir = File(context.filesDir, DIR_NAME).canonicalFile
        val canonical = runCatching { file.canonicalFile }.getOrNull() ?: return
        if (canonical.path.startsWith(photosDir.path) && canonical.exists()) {
            canonical.delete()
        }
    }
}
