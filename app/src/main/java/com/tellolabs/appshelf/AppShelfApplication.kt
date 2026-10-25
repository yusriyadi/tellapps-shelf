package com.tellolabs.appshelf

import android.app.Application
import com.tellolabs.appshelf.data.catalog.CatalogRepository
import com.tellolabs.appshelf.data.download.DownloadRepository
import com.tellolabs.appshelf.data.settings.SettingsRepository
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppShelfApplication : Application() {
    val container by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val http = OkHttpClient.Builder()
        .followSslRedirects(false)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()
    val settings = SettingsRepository(application)
    val catalog = CatalogRepository(application, http, json)
    val downloads = DownloadRepository(application, settings.store, json)
}
