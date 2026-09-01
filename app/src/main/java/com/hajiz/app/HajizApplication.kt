package com.hajiz.app

import android.app.Application
import com.hajiz.app.data.HajizDatabase
import com.hajiz.app.data.SettingsRepository
import com.hajiz.app.filtering.LocalBlocklistProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HajizApplication : Application() {
    val database by lazy { HajizDatabase.getInstance(this) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val blocklistProvider by lazy { LocalBlocklistProvider(database.blockedDomainDao()) }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            blocklistProvider.seedSafeDefaults()
        }
    }
}