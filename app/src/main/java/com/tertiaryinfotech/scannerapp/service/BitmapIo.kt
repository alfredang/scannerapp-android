package com.tertiaryinfotech.scannerapp.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/** Helpers for decoding captured images and writing JPEGs to disk. */
object BitmapIo {

    private const val MAX_EDGE = 2600
    const val STORE_QUALITY = 85

    fun loadBitmaps(context: Context, uris: List<Uri>): List<Bitmap> =
        uris.mapNotNull { runCatching { loadBitmap(context, it) }.getOrNull() }

    fun loadBitmap(context: Context, uri: Uri): Bitmap {
        val resolver = context.contentResolver
        // Decode bounds first to compute a sample size.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sample = 1
        while (longest / sample > MAX_EDGE) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: error("Could not decode $uri")
        val rotation = resolver.openInputStream(uri)?.use { exifRotation(it) } ?: 0
        return if (rotation == 0) decoded else {
            val m = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
        }
    }

    private fun exifRotation(stream: java.io.InputStream): Int = try {
        when (ExifInterface(stream).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (_: Throwable) { 0 }

    fun writeJpeg(bitmap: Bitmap, file: File, quality: Int = STORE_QUALITY) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
    }

    fun decodeFile(file: File): Bitmap? =
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
}
