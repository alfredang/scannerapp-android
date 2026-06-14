package com.tertiaryinfotech.scannerapp.service

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** On-device text recognition via ML Kit (Latin). Mirrors the iOS Vision OCRService. */
object OcrService {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val text = result.textBlocks
                    .flatMap { it.lines }
                    .joinToString("\n") { it.text }
                cont.resume(text)
            }
            .addOnFailureListener { cont.resume("") }
    }
}
