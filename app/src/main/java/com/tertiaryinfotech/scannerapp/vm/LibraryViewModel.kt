package com.tertiaryinfotech.scannerapp.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.tertiaryinfotech.scannerapp.ScannerApplication
import com.tertiaryinfotech.scannerapp.data.DocumentWithPages
import com.tertiaryinfotech.scannerapp.data.ScanDao
import com.tertiaryinfotech.scannerapp.service.StorageService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val storage: StorageService,
    dao: ScanDao
) : ViewModel() {

    val documents: StateFlow<List<DocumentWithPages>> =
        dao.observeDocuments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    fun setSearch(text: String) { _searchText.value = text }

    /** Case-insensitive substring match on name OR combined OCR text. */
    fun filter(docs: List<DocumentWithPages>, query: String): List<DocumentWithPages> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return docs
        return docs.filter {
            it.document.name.lowercase().contains(q) ||
                it.document.combinedOcrText.lowercase().contains(q)
        }
    }

    fun rename(documentId: String, newName: String) = viewModelScope.launch {
        storage.rename(documentId, newName)
    }

    fun delete(documentId: String) = viewModelScope.launch {
        storage.delete(documentId)
    }

    fun duplicate(source: DocumentWithPages) = viewModelScope.launch {
        storage.duplicate(source)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ScannerApplication
                LibraryViewModel(app.container.storage, app.container.dao)
            }
        }
    }
}
