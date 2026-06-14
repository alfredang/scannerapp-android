package com.tertiaryinfotech.scannerapp.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Image enhancement filters. Raw values are persisted to the database and must remain stable.
 * Order matters: it is the order shown in the filter strip. Mirrors the iOS FilterType enum.
 */
enum class FilterType(val raw: String, val displayName: String) {
    ORIGINAL("original", "Original"),
    AUTO_ENHANCE("autoEnhance", "Auto"),
    WHITE_DOCUMENT("whiteDocument", "White"),
    BLACK_AND_WHITE("blackAndWhite", "B&W"),
    REMOVE_NOISE("removeNoise", "Denoise"),
    BRIGHTEN("brighten", "Bright"),
    SHARPEN_TEXT("sharpenText", "Sharpen"),
    RECEIPT("receipt", "Receipt");

    val icon: ImageVector
        get() = when (this) {
            ORIGINAL -> Icons.Filled.Image
            AUTO_ENHANCE -> Icons.Filled.AutoFixHigh
            WHITE_DOCUMENT -> Icons.Filled.Description
            BLACK_AND_WHITE -> Icons.Filled.Contrast
            REMOVE_NOISE -> Icons.Filled.AutoAwesome
            BRIGHTEN -> Icons.Filled.Brightness6
            SHARPEN_TEXT -> Icons.Filled.TextFields
            RECEIPT -> Icons.Filled.ReceiptLong
        }

    companion object {
        fun fromRaw(raw: String?): FilterType =
            entries.firstOrNull { it.raw == raw } ?: ORIGINAL
    }
}
