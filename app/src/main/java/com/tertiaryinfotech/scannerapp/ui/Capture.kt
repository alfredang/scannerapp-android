package com.tertiaryinfotech.scannerapp.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.tertiaryinfotech.scannerapp.service.BitmapIo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun Context.findActivity(): Activity {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    error("No Activity in context chain")
}

/**
 * Returns a launcher lambda that opens the ML Kit Document Scanner (edge detection, perspective
 * correction, multi-page, gallery import). Falls back to the system Photo Picker on devices
 * without Google Play services (e.g. some emulators). Decoded page bitmaps are delivered to
 * [onCaptured] on the main thread.
 */
@Composable
fun rememberDocumentCapture(onCaptured: (List<Bitmap>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun deliver(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            val bitmaps = withContext(Dispatchers.IO) { BitmapIo.loadBitmaps(context, uris) }
            if (bitmaps.isNotEmpty()) onCaptured(bitmaps)
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris -> deliver(uris) }

    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
        val uris = scanResult?.pages.orEmpty().mapNotNull { it.imageUri }
        deliver(uris)
    }

    return {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(30)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(context.findActivity())
            .addOnSuccessListener { intentSender ->
                scanLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                pickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
    }
}
