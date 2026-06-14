package com.tertiaryinfotech.scannerapp

import android.app.Application
import com.tertiaryinfotech.scannerapp.data.AppDatabase
import com.tertiaryinfotech.scannerapp.data.ScanDao
import com.tertiaryinfotech.scannerapp.service.StorageService
import com.tertiaryinfotech.scannerapp.settings.SettingsStore

/** Manual dependency container — created once for the process. */
class AppContainer(application: Application) {
    private val database: AppDatabase = AppDatabase.get(application)
    val dao: ScanDao = database.scanDao()
    val storage: StorageService = StorageService(application, dao)
    val settings: SettingsStore = SettingsStore(application)
}

class ScannerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
