package com.tertiaryinfotech.scannerapp.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tertiaryinfotech.scannerapp.model.FilterType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** PDF page geometry. Mirrors the iOS PageSize options. */
enum class PdfPageSize(val raw: String, val displayName: String, val widthPt: Float?, val heightPt: Float?) {
    A4("a4", "A4", 595.2f, 841.8f),
    LETTER("letter", "Letter", 612f, 792f),
    FIT("fit", "Fit to Image", null, null);

    companion object {
        fun fromRaw(raw: String?): PdfPageSize = entries.firstOrNull { it.raw == raw } ?: FIT
    }
}

data class AppSettings(
    val pdfQuality: Float = 0.85f,
    val pdfPageSize: PdfPageSize = PdfPageSize.FIT,
    val defaultFilter: FilterType = FilterType.ORIGINAL
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val QUALITY = floatPreferencesKey("pdfQuality")
        val PAGE_SIZE = stringPreferencesKey("pdfPageSize")
        val DEFAULT_FILTER = stringPreferencesKey("defaultFilter")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            pdfQuality = prefs[Keys.QUALITY] ?: 0.85f,
            pdfPageSize = PdfPageSize.fromRaw(prefs[Keys.PAGE_SIZE]),
            defaultFilter = FilterType.fromRaw(prefs[Keys.DEFAULT_FILTER])
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setQuality(value: Float) {
        context.dataStore.edit { it[Keys.QUALITY] = value.coerceIn(0.30f, 1.0f) }
    }

    suspend fun setPageSize(size: PdfPageSize) {
        context.dataStore.edit { it[Keys.PAGE_SIZE] = size.raw }
    }

    suspend fun setDefaultFilter(filter: FilterType) {
        context.dataStore.edit { it[Keys.DEFAULT_FILTER] = filter.raw }
    }
}
