package com.tertiaryinfotech.scannerapp.vm

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.tertiaryinfotech.scannerapp.ScannerApplication
import com.tertiaryinfotech.scannerapp.imaging.ImageProcessor
import com.tertiaryinfotech.scannerapp.model.FilterType
import com.tertiaryinfotech.scannerapp.service.OcrService
import com.tertiaryinfotech.scannerapp.service.StorageService
import com.tertiaryinfotech.scannerapp.service.WorkingDocument
import com.tertiaryinfotech.scannerapp.service.WorkingPage
import com.tertiaryinfotech.scannerapp.settings.SettingsStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScannerViewModel(
    private val storage: StorageService,
    private val settings: SettingsStore
) : ViewModel() {

    private val _working = MutableStateFlow<WorkingDocument?>(null)
    val working: StateFlow<WorkingDocument?> = _working.asStateFlow()

    private val _isRunningOcr = MutableStateFlow(false)
    val isRunningOcr: StateFlow<Boolean> = _isRunningOcr.asStateFlow()

    fun beginNewDocument(bitmaps: List<Bitmap>) {
        if (bitmaps.isEmpty()) return
        viewModelScope.launch {
            val filter = settings.current().defaultFilter
            val pages = withContext(Dispatchers.Default) { bitmaps.map { makePage(it, filter) } }
            _working.value = WorkingDocument(UUID.randomUUID().toString(), suggestedName(), pages)
        }
    }

    fun addPages(bitmaps: List<Bitmap>) {
        if (bitmaps.isEmpty()) return
        viewModelScope.launch {
            val filter = settings.current().defaultFilter
            val newPages = withContext(Dispatchers.Default) { bitmaps.map { makePage(it, filter) } }
            val current = _working.value
            _working.value = if (current == null) {
                WorkingDocument(UUID.randomUUID().toString(), suggestedName(), newPages)
            } else {
                current.copy(pages = current.pages + newPages)
            }
        }
    }

    fun deletePage(pageId: String) {
        val current = _working.value ?: return
        val remaining = current.pages.filterNot { it.id == pageId }
        _working.value = if (remaining.isEmpty()) null else current.copy(pages = remaining)
    }

    fun setName(name: String) {
        _working.value = _working.value?.copy(name = name)
    }

    fun setFilter(pageId: String, filter: FilterType) {
        updatePage(pageId) { it.copy(filter = filter) }
    }

    fun applyFilterToAll(filter: FilterType) {
        val current = _working.value ?: return
        viewModelScope.launch {
            val pages = withContext(Dispatchers.Default) {
                current.pages.map { it.copy(filter = filter, processed = render(it.original, filter, it.rotationDegrees)) }
            }
            _working.value = current.copy(pages = pages)
        }
    }

    fun rotate(pageId: String) {
        updatePage(pageId) { it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360) }
    }

    private fun updatePage(pageId: String, transform: (WorkingPage) -> WorkingPage) {
        val current = _working.value ?: return
        viewModelScope.launch {
            val pages = current.pages.map { page ->
                if (page.id != pageId) page
                else withContext(Dispatchers.Default) {
                    val changed = transform(page)
                    changed.copy(processed = render(changed.original, changed.filter, changed.rotationDegrees))
                }
            }
            _working.value = current.copy(pages = pages)
        }
    }

    fun runOcr(onDone: () -> Unit = {}) {
        val current = _working.value ?: return
        viewModelScope.launch {
            _isRunningOcr.value = true
            val pages = current.pages.map { it.copy(ocrText = OcrService.recognize(it.processed)) }
            _working.value = current.copy(pages = pages)
            _isRunningOcr.value = false
            onDone()
        }
    }

    fun save(onSaved: (String) -> Unit) {
        val current = _working.value ?: return
        if (!current.hasPages) return
        viewModelScope.launch {
            val id = storage.persist(current)
            _working.value = null
            onSaved(id)
        }
    }

    fun discard() {
        _working.value = null
    }

    private fun makePage(bitmap: Bitmap, filter: FilterType): WorkingPage {
        val processed = render(bitmap, filter, 0)
        return WorkingPage(
            id = UUID.randomUUID().toString(),
            original = bitmap,
            filter = filter,
            rotationDegrees = 0,
            processed = processed
        )
    }

    private fun render(original: Bitmap, filter: FilterType, rotation: Int): Bitmap =
        ImageProcessor.process(original, filter, rotation)

    private fun suggestedName(): String =
        "Scan " + SimpleDateFormat("yyyy-MM-dd HH.mm", Locale.getDefault()).format(Date())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ScannerApplication
                ScannerViewModel(app.container.storage, app.container.settings)
            }
        }
    }
}
