package com.tertiaryinfotech.scannerapp.service

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Writes export artifacts (JPG/PDF) to cache, saves images to the gallery, and builds share intents. */
object ExportService {

    private fun authority(context: Context) = "${context.packageName}.fileprovider"

    fun writeJpgs(context: Context, bitmaps: List<Bitmap>, baseName: String, quality: Int = 92): List<File> {
        val dir = File(context.cacheDir, "exports/export-${UUID.randomUUID()}").apply { mkdirs() }
        return bitmaps.mapIndexed { i, bmp ->
            val f = File(dir, "$baseName-${i + 1}.jpg")
            BitmapIo.writeJpeg(bmp, f, quality)
            f
        }
    }

    fun writePdf(context: Context, bytes: ByteArray, baseName: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val f = File(dir, "${sanitize(baseName)}.pdf")
        f.writeBytes(bytes)
        return f
    }

    /** Save images to the device gallery (Pictures/Tertiary Scanner). Returns count saved. */
    fun saveToGallery(context: Context, bitmaps: List<Bitmap>, baseName: String): Int {
        val resolver = context.contentResolver
        var saved = 0
        bitmaps.forEachIndexed { i, bmp ->
            val name = "$baseName-${i + 1}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/Tertiary Scanner")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val uri = resolver.insert(collection, values) ?: return@forEachIndexed
            resolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            saved++
        }
        return saved
    }

    fun shareFiles(context: Context, files: List<File>, mimeType: String): Intent {
        val uris = files.map { contentUri(context, it) }
        return if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    fun shareText(text: String): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

    fun contentUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, authority(context), file)

    private fun sanitize(name: String) = name.replace("/", "-").ifBlank { "Document" }
}
