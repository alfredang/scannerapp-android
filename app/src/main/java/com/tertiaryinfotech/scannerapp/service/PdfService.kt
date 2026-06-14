package com.tertiaryinfotech.scannerapp.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.tertiaryinfotech.scannerapp.settings.PdfPageSize
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

/** Builds a PDF from page bitmaps. Ports the iOS PDFService geometry. */
object PdfService {

    fun makePdf(bitmaps: List<Bitmap>, pageSize: PdfPageSize, quality: Float): ByteArray {
        val doc = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        bitmaps.forEachIndexed { index, raw ->
            val img = recompress(raw, quality)
            val (pw, ph) = pageRect(img, pageSize)
            val info = PdfDocument.PageInfo.Builder(pw.roundToInt(), ph.roundToInt(), index + 1).create()
            val page = doc.startPage(info)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            val dest = aspectFit(img.width.toFloat(), img.height.toFloat(), pw, ph)
            canvas.drawBitmap(img, null, dest, paint)
            doc.finishPage(page)
        }
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    /** Page size in points (1pt = 1/72in). Fixed sizes auto-swap to match image orientation. */
    private fun pageRect(img: Bitmap, size: PdfPageSize): Pair<Float, Float> {
        val w = size.widthPt
        val h = size.heightPt
        if (w == null || h == null) return img.width.toFloat() to img.height.toFloat()
        val landscape = img.width > img.height
        return if (landscape) h to w else w to h
    }

    private fun aspectFit(iw: Float, ih: Float, pw: Float, ph: Float): RectF {
        val scale = min(pw / iw, ph / ih)
        val dw = iw * scale
        val dh = ih * scale
        val left = (pw - dw) / 2f
        val top = (ph - dh) / 2f
        return RectF(left, top, left + dw, top + dh)
    }

    private fun recompress(bitmap: Bitmap, quality: Float): Bitmap {
        return try {
            val q = (quality.coerceIn(0.1f, 1f) * 100).roundToInt()
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, q, baos)
            val bytes = baos.toByteArray()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: bitmap
        } catch (_: Throwable) {
            bitmap
        }
    }
}
