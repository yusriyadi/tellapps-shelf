package com.tellolabs.appshelf.model

import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Serializable
data class Catalog(val schemaVersion: Int, val apps: List<CatalogApp>)

@Serializable
data class CatalogApp(
    val id: String,
    val name: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val packageName: String? = null,
    val repositoryUrl: String? = null,
    val releases: List<AppRelease> = emptyList(),
)

@Serializable
data class AppRelease(
    val tag: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val releasedAt: String? = null,
    val minSdk: Int? = null,
    val changelog: List<String> = emptyList(),
    val apkUrl: String? = null,
    val sizeBytes: Long? = null,
    val sha256: String? = null,
) {
    val label: String get() = versionName?.takeIf { it.isNotBlank() } ?: "Nomor versi belum tersedia"
}

fun String?.isHttpsUrl(): Boolean = this?.toHttpUrlOrNull()?.let {
    it.isHttps && it.host.isNotBlank() && it.username.isEmpty() && it.password.isEmpty()
} == true

@Serializable
data class DownloadRecord(
    val key: String,
    val managerId: Long,
    val appId: String,
    val appName: String,
    val versionLabel: String,
    val url: String,
    val fileName: String,
    val sha256: String? = null,
    val cancelled: Boolean = false,
    val versionCode: Long? = null,
    val packageName: String? = null,
)

enum class DownloadStatus(val label: String) {
    QUEUED("Dalam antrean"), RUNNING("Mengunduh"), PAUSED("Dijeda sistem"),
    VERIFYING("Memeriksa APK"), READY("Siap install"), FAILED("Gagal"),
    CANCELLED("Dibatalkan"), MISSING("File tidak ditemukan"), INVALID("Checksum tidak cocok"),
}

data class DownloadItem(
    val record: DownloadRecord,
    val status: DownloadStatus,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = -1,
    val message: String? = null,
    val fileSizeBytes: Long? = null,
) {
    val active: Boolean get() = status in setOf(DownloadStatus.QUEUED, DownloadStatus.RUNNING, DownloadStatus.PAUSED, DownloadStatus.VERIFYING)
    val progress: Float? get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else null
}
