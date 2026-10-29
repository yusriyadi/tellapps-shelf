package com.tellolabs.appshelf.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tellolabs.appshelf.AppContainer
import com.tellolabs.appshelf.BuildConfig
import com.tellolabs.appshelf.data.catalog.CatalogSnapshot
import com.tellolabs.appshelf.data.settings.ThemeMode
import com.tellolabs.appshelf.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CatalogUiState(
    val snapshot: CatalogSnapshot? = null,
    val loading: Boolean = true,
    val fromCache: Boolean = false,
    val error: String? = null,
)

data class SelfUpdateUiState(
    val versionName: String = BuildConfig.VERSION_NAME,
    val isDebug: Boolean = BuildConfig.DEBUG,
    val app: CatalogApp? = null,
    val status: InstallationStatus = InstallationStatus.UNKNOWN,
    val loading: Boolean = true,
    val fromCache: Boolean = false,
    val error: String? = null,
)

class AppViewModel(private val container: AppContainer) : ViewModel() {
    private val mutableCatalog = MutableStateFlow(CatalogUiState())
    val catalog = mutableCatalog.asStateFlow()
    private val mutableInstallations = MutableStateFlow<Map<String, AppInstallation>>(emptyMap())
    val installations = mutableInstallations.asStateFlow()
    val selfUpdate = catalog.map { state ->
        // Match package identity; the catalog entry's ID or display name may change.
        val app = state.snapshot?.catalog?.apps?.firstOrNull { it.packageName == BuildConfig.APPLICATION_ID }
        SelfUpdateUiState(
            app = app,
            status = compareInstalledVersion(BuildConfig.VERSION_CODE.toLong(), app?.releases?.firstOrNull()?.versionCode),
            loading = state.loading,
            fromCache = state.fromCache,
            error = state.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SelfUpdateUiState())
    val downloads = container.downloads.items
    val theme = container.settings.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)
    private val mutableMessage = MutableStateFlow<String?>(null)
    val message = mutableMessage.asStateFlow()
    private var refreshing = false

    init {
        viewModelScope.launch {
            container.catalog.cached()?.let {
                mutableCatalog.value = CatalogUiState(it, loading = false, fromCache = true)
            }
            refresh()
        }
    }

    fun refresh() {
        if (refreshing) return
        refreshing = true
        viewModelScope.launch {
            mutableCatalog.update { it.copy(loading = true, error = null) }
            try {
                val snapshot = container.catalog.refresh()
                mutableCatalog.value = CatalogUiState(snapshot, loading = false)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                mutableCatalog.update {
                    it.copy(loading = false, fromCache = it.snapshot != null,
                        error = "Katalog gagal dimuat. ${e.message ?: "Periksa koneksi internet."}")
                }
            } finally { refreshing = false }
        }
    }

    // Re-read on resume, catalog changes, and package events while foregrounded.
    suspend fun observeInstallations() {
        combine(
            catalog.map { it.snapshot?.catalog?.apps.orEmpty() }.distinctUntilChanged(),
            container.installations.packageChanges(),
        ) { apps, _ -> apps }.collectLatest { apps ->
            mutableInstallations.value = container.installations.check(apps)
        }
    }

    // Called within repeatOnLifecycle(STARTED); no polling while the app is in the background.
    suspend fun observeDownloads() {
        while (true) {
            try { container.downloads.refresh()
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { mutableMessage.value = "Status unduhan gagal dibaca. ${e.message.orEmpty()}" }
            delay(if (downloads.value.any { it.active }) 1_000 else 5_000)
        }
    }

    fun download(app: CatalogApp, release: AppRelease) = action { container.downloads.enqueue(app, release) }
    fun retry(record: DownloadRecord) = action { container.downloads.retry(record) }
    fun cancel(record: DownloadRecord) = action { container.downloads.cancel(record) }
    fun delete(record: DownloadRecord) = action { container.downloads.delete(record) }
    fun cleanOldDownloads(confirmed: List<DownloadRecord>) = action {
        val apps = catalog.value.snapshot?.catalog?.apps.orEmpty()
        val count = container.downloads.deleteOld(confirmed, apps)
        mutableMessage.value = if (count > 0) "$count APK lama dihapus." else "Tidak ada APK lama yang dapat dibersihkan."
    }
    fun setTheme(mode: ThemeMode) = action { container.settings.setTheme(mode) }
    fun clearCache() = action {
        container.catalog.clearCache()
        mutableCatalog.update { it.copy(snapshot = it.snapshot?.copy(syncedAt = 0), fromCache = false) }
        mutableMessage.value = "Cache katalog dihapus. Data di layar tetap tersedia sampai aplikasi ditutup."
    }
    fun showMessage(message: String) { mutableMessage.value = message }
    fun consumeMessage() { mutableMessage.value = null }
    suspend fun installFile(record: DownloadRecord) = container.downloads.installFile(record)

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block()
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { mutableMessage.value = e.message ?: "Terjadi kesalahan. Silakan coba lagi." }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(container) as T
        }
    }
}
