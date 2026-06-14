package com.tertiaryinfotech.scannerapp.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.tertiaryinfotech.scannerapp.ScannerApplication
import com.tertiaryinfotech.scannerapp.model.FilterType
import com.tertiaryinfotech.scannerapp.settings.AppSettings
import com.tertiaryinfotech.scannerapp.settings.PdfPageSize
import com.tertiaryinfotech.scannerapp.settings.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val store: SettingsStore) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setQuality(value: Float) = viewModelScope.launch { store.setQuality(value) }
    fun setPageSize(size: PdfPageSize) = viewModelScope.launch { store.setPageSize(size) }
    fun setDefaultFilter(filter: FilterType) = viewModelScope.launch { store.setDefaultFilter(filter) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ScannerApplication
                SettingsViewModel(app.container.settings)
            }
        }
    }
}
