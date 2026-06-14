package com.tertiaryinfotech.scannerapp.imaging

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.tertiaryinfotech.scannerapp.model.FilterType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * On-device image enhancement. Ports the iOS Core Image filter graphs to Android.
 * Color operations use ColorMatrix (matching CIColorControls / CIExposureAdjust semantics);
 * sharpening uses a 3x3 unsharp convolution; "auto" emulates CIImage.autoAdjustment with a
 * histogram-based levels stretch plus a gentle saturation lift.
 */
object ImageProcessor {

    /** Full pipeline: apply the filter, then rotate clockwise. */
    fun process(source: Bitmap, filter: FilterType, rotationDegrees: Int): Bitmap {
        val filtered = apply(filter, source)
        return rotate(filtered, rotationDegrees)
    }

    fun apply(filter: FilterType, src: Bitmap): Bitmap = try {
        when (filter) {
            FilterType.ORIGINAL -> src
            FilterType.AUTO_ENHANCE -> autoEnhance(src)
            FilterType.WHITE_DOCUMENT ->
                highlightShadow(colorControls(src, brightness = 0.12f, contrast = 1.45f, saturation = 0.9f),
                    highlight = 1.0f, shadow = 0.3f)
            FilterType.BLACK_AND_WHITE ->
                colorControls(src, brightness = 0.05f, contrast = 1.5f, saturation = 0.0f)
            FilterType.REMOVE_NOISE ->
                highlightShadow(denoise(src), highlight = 0.8f, shadow = 1.0f)
            FilterType.BRIGHTEN -> exposure(src, ev = 0.7f)
            FilterType.SHARPEN_TEXT -> unsharp(src, amount = 0.8f, radiusBoost = 2.0f)
            FilterType.RECEIPT ->
                unsharp(colorControls(src, brightness = 0.1f, contrast = 1.6f, saturation = 0.0f),
                    amount = 0.5f, radiusBoost = 1.0f)
        }
    } catch (t: Throwable) {
        src
    }

    /** Rotate clockwise by the given degrees (normalized to 0/90/180/270). */
    fun rotate(src: Bitmap, degrees: Int): Bitmap {
        val d = ((degrees % 360) + 360) % 360
        if (d == 0) return src
        val m = Matrix().apply { postRotate(d.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    // --- Color operations -----------------------------------------------------

    /**
     * CIColorControls equivalent. brightness is additive in [-1,1] (scaled to 8-bit),
     * contrast is multiplicative around mid-gray, saturation multiplicative (0 = grayscale).
     */
    private fun colorControls(src: Bitmap, brightness: Float, contrast: Float, saturation: Float): Bitmap {
        val sat = ColorMatrix().apply { setSaturation(saturation) }
        val t = (-0.5f * contrast + 0.5f) * 255f + brightness * 255f
        val cb = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, t,
            0f, contrast, 0f, 0f, t,
            0f, 0f, contrast, 0f, t,
            0f, 0f, 0f, 1f, 0f
        ))
        cb.preConcat(sat)
        return applyMatrix(src, cb)
    }

    /** CIExposureAdjust: output ≈ input * 2^ev. */
    private fun exposure(src: Bitmap, ev: Float): Bitmap {
        val k = 2f.pow(ev)
        val m = ColorMatrix(floatArrayOf(
            k, 0f, 0f, 0f, 0f,
            0f, k, 0f, 0f, 0f,
            0f, 0f, k, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        return applyMatrix(src, m)
    }

    private fun applyMatrix(src: Bitmap, matrix: ColorMatrix): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    /**
     * CIHighlightShadowAdjust approximation. highlight 1.0 = no change (lower darkens highlights);
     * shadow 0 = no change, 1.0 = max lift. Implemented as a per-channel tone curve.
     */
    private fun highlightShadow(src: Bitmap, highlight: Float, shadow: Float): Bitmap {
        val lut = IntArray(256)
        for (i in 0..255) {
            val n = i / 255f
            // lift shadows (gamma < 1 weighted by shadow amount)
            val shadowGamma = 1f - 0.5f * shadow.coerceIn(0f, 1f)
            var v = n.pow(shadowGamma)
            // pull highlights toward (highlight) ceiling when highlight < 1
            if (highlight < 1f) {
                val ceil = 0.6f + 0.4f * highlight
                v = min(v, ceil) + (v - min(v, ceil)) * highlight
            }
            lut[i] = (v.coerceIn(0f, 1f) * 255f).toInt()
        }
        return applyLut(src, lut)
    }

    /** Mild box-blur to emulate CINoiseReduction's smoothing pass. */
    private fun denoise(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var r = 0; var g = 0; var b = 0; var n = 0
                var dy = -1
                while (dy <= 1) {
                    var dx = -1
                    while (dx <= 1) {
                        val xx = x + dx; val yy = y + dy
                        if (xx in 0 until w && yy in 0 until h) {
                            val c = px[yy * w + xx]
                            r += (c shr 16) and 0xFF
                            g += (c shr 8) and 0xFF
                            b += c and 0xFF
                            n++
                        }
                        dx++
                    }
                    dy++
                }
                out[y * w + x] = (0xFF shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
            }
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    /** CIUnsharpMask / CISharpenLuminance approximation via a 3x3 sharpen kernel. */
    private fun unsharp(src: Bitmap, amount: Float, radiusBoost: Float): Bitmap {
        val a = (0.5f * amount * radiusBoost).coerceIn(0f, 2f)
        val center = 1f + 4f * a
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)
        fun ch(c: Int, s: Int) = (c shr s) and 0xFF
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) { out[i] = px[i]; continue }
                val up = px[i - w]; val dn = px[i + w]; val lf = px[i - 1]; val rt = px[i + 1]; val cc = px[i]
                fun comp(s: Int): Int {
                    val v = center * ch(cc, s) - a * (ch(up, s) + ch(dn, s) + ch(lf, s) + ch(rt, s))
                    return v.toInt().coerceIn(0, 255)
                }
                out[i] = (0xFF shl 24) or (comp(16) shl 16) or (comp(8) shl 8) or comp(0)
            }
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    /** Emulates CIImage.autoAdjustmentFilters: per-channel levels stretch + gentle saturation. */
    private fun autoEnhance(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        // luminance histogram -> 1st/99th percentile for a robust stretch
        val hist = IntArray(256)
        for (c in px) {
            val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
            val l = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
            hist[l]++
        }
        val total = px.size
        val loCut = (total * 0.01f).toInt()
        val hiCut = (total * 0.01f).toInt()
        var lo = 0; var acc = 0
        while (lo < 255 && acc < loCut) { acc += hist[lo]; lo++ }
        var hi = 255; acc = 0
        while (hi > 0 && acc < hiCut) { acc += hist[hi]; hi-- }
        if (hi <= lo) { lo = 0; hi = 255 }
        val range = (hi - lo).coerceAtLeast(1)
        val lut = IntArray(256)
        for (i in 0..255) lut[i] = (((i - lo).toFloat() / range) * 255f).toInt().coerceIn(0, 255)
        val stretched = applyLut(src, lut)
        // gentle saturation boost
        return applyMatrix(stretched, ColorMatrix().apply { setSaturation(1.12f) })
    }

    private fun applyLut(src: Bitmap, lut: IntArray): Bitmap {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        for (i in px.indices) {
            val c = px[i]
            val a = (c shr 24) and 0xFF
            val r = lut[(c shr 16) and 0xFF]
            val g = lut[(c shr 8) and 0xFF]
            val b = lut[c and 0xFF]
            px[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(px, 0, w, 0, 0, w, h)
        return out
    }

    /** Aspect-fit downscale so the longest edge is [maxDimension] px; never upscales. */
    fun makeThumbnail(src: Bitmap, maxDimension: Int = 320): Bitmap {
        val longest = max(src.width, src.height)
        val scale = min(maxDimension.toFloat() / longest, 1f)
        if (scale >= 1f) return src
        val w = (src.width * scale).toInt().coerceAtLeast(1)
        val h = (src.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }
}
