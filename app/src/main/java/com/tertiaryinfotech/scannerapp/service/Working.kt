package com.tertiaryinfotech.scannerapp.service

import android.graphics.Bitmap
import com.tertiaryinfotech.scannerapp.model.FilterType

/** An in-memory page during capture/edit, before it is persisted. */
data class WorkingPage(
    val id: String,
    val original: Bitmap,
    val filter: FilterType,
    val rotationDegrees: Int,
    val processed: Bitmap,
    val ocrText: String = ""
)

/** An in-memory document being assembled in the editor. */
data class WorkingDocument(
    val id: String,
    val name: String,
    val pages: List<WorkingPage>
) {
    val hasPages: Boolean get() = pages.isNotEmpty()
}
