package com.tellolabs.appshelf.data.download

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import com.tellolabs.appshelf.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class DownloadRepository(
    private val context: Context,
    private val store: DataStore<Preferences>,
    private val json: Json,
) {
    private val manager = context.getSystemService(DownloadManager::class.java)
    private val recordsKey = stringPreferencesKey("downloads")
    private val mutex = Mutex()
    private val mutableItems = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items = mutableItems.asStateFlow()
    private val verified = mutableMapOf<String, Pair<String, Boolean>>()

    private fun directory(): File {
        val root = checkNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)) { "Penyimpanan unduhan tidak tersedia" }
        return File(root, "apks").apply { check(isDirectory || mkdirs()) { "Folder unduhan tidak dapat dibuat" } }
    }

    private fun file(record: DownloadRecord): File {
        require(record.fileName.matches(Regex("[a-f0-9-]{36}\\.apk"))) { "Nama file unduhan tidak valid" }
        return File(directory(), record.fileName)
    }

    private suspend fun records(): List<DownloadRecord> {
        val raw = store.data.first()[recordsKey] ?: return emptyList()
        return json.decodeFromString<List<DownloadRecord>>(raw)
    }
    private suspend fun save(records: List<DownloadRecord>) { store.edit { it[recordsKey] = json.encodeToString(records) } }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock { publish(records()) }
    }
    private fun publish(records: List<DownloadRecord>) { mutableItems.value = records.map(::query).asReversed() }

    suspend fun enqueue(app: CatalogApp, release: AppRelease) = withContext(Dispatchers.IO) {
        require(release.apkUrl.isHttpsUrl()) { "URL APK HTTPS tidak valid" }
        require(release.minSdk == null || release.minSdk <= android.os.Build.VERSION.SDK_INT) { "Versi Android perangkat tidak kompatibel" }
        mutex.withLock {
            val all = records()
            val existing = all.firstOrNull { it.appId == app.id && it.url == release.apkUrl }
            if (existing != null && query(existing).let { it.active || it.status == DownloadStatus.READY }) {
                publish(all)
                return@withLock
            }
            start(all, existing, app.id, app.name, release.label, requireNotNull(release.apkUrl), release.sha256, release.versionCode, app.packageName)
        }
    }

    suspend fun retry(record: DownloadRecord) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val all = records()
            val current = all.firstOrNull { it.key == record.key } ?: return@withLock
            if (query(current).let { it.active || it.status == DownloadStatus.READY }) return@withLock
            start(all, current, current.appId, current.appName, current.versionLabel, current.url, current.sha256, current.versionCode, current.packageName)
        }
    }

    private suspend fun start(all: List<DownloadRecord>, old: DownloadRecord?, appId: String, name: String, version: String, url: String, sha: String?, versionCode: Long?, packageName: String?) {
        require(url.isHttpsUrl()) { "URL APK HTTPS tidak valid" }
        val nameOnDisk = "${UUID.randomUUID()}.apk"
        val destination = File(directory(), nameOnDisk)
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("$name · $version")
            .setDescription("TellApps Shelf")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))
        val id = manager.enqueue(request)
        val next = DownloadRecord(old?.key ?: UUID.randomUUID().toString(), id, appId, name, version, url, nameOnDisk, sha, versionCode = versionCode, packageName = packageName)
        val updated = all.filterNot { it.key == old?.key } + next
        try {
            save(updated)
        } catch (e: Exception) {
            manager.remove(id)
            destination.delete()
            throw e
        }
        old?.let { manager.remove(it.managerId); file(it).delete(); verified.remove(it.key) }
        publish(updated)
    }

    suspend fun cancel(record: DownloadRecord) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val all = records()
            val current = all.firstOrNull { it.key == record.key } ?: return@withLock
            // Persist intent before touching DownloadManager so process death cannot lose cancellation.
            val updated = all.map { if (it.key == current.key) it.copy(cancelled = true) else it }
            save(updated)
            manager.remove(current.managerId)
            file(current).delete()
            verified.remove(current.key)
            publish(updated)
        }
    }

    suspend fun delete(record: DownloadRecord) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val all = records()
            val current = all.firstOrNull { it.key == record.key } ?: return@withLock
            manager.remove(current.managerId)
            val apk = file(current)
            check(!apk.exists() || apk.delete()) { "File APK tidak dapat dihapus" }
            val updated = all.filterNot { it.key == record.key }
            save(updated)
            verified.remove(record.key)
            publish(updated)
        }
    }

    suspend fun deleteOld(confirmed: List<DownloadRecord>, apps: List<CatalogApp>): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            val all = records()
            val approved = confirmed.associateBy { it.key }
            val catalog = apps.associateBy { it.id }
            val removed = mutableSetOf<String>()
            var failed = 0
            all.forEach { record ->
                // A retry may reuse the key but point at a new transfer. Never
                // delete a replacement download the user did not confirm.
                if (approved[record.key]?.managerId == record.managerId && query(record).canCleanOld(catalog[record.appId])) {
                    try {
                        manager.remove(record.managerId)
                        val apk = file(record)
                        check(!apk.exists() || apk.delete()) { "File tidak dapat dihapus" }
                        removed += record.key
                        verified.remove(record.key)
                    } catch (_: RuntimeException) { failed++ }
                }
            }
            val updated = all.filterNot { it.key in removed }
            save(updated)
            publish(updated)
            check(failed == 0) { "${removed.size} APK lama dihapus; $failed file gagal dihapus. Silakan coba lagi." }
            removed.size
        }
    }

    suspend fun installFile(record: DownloadRecord): File = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = records().firstOrNull { it.key == record.key } ?: error("Unduhan tidak ditemukan")
            verified.remove(current.key) // Verify again just before granting installer access.
            val item = query(current)
            check(item.status == DownloadStatus.READY) { item.message ?: item.status.label }
            file(current)
        }
    }

    private fun query(record: DownloadRecord): DownloadItem {
        if (record.cancelled) {
            manager.remove(record.managerId)
            file(record).delete()
            return DownloadItem(record, DownloadStatus.CANCELLED)
        }
        manager.query(DownloadManager.Query().setFilterById(record.managerId))?.use { cursor ->
            if (!cursor.moveToFirst()) return DownloadItem(record, DownloadStatus.MISSING)
            val downloaded = cursor.number(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val total = cursor.number(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val status = when (cursor.number(DownloadManager.COLUMN_STATUS).toInt()) {
                DownloadManager.STATUS_PENDING -> DownloadStatus.QUEUED
                DownloadManager.STATUS_RUNNING -> DownloadStatus.RUNNING
                DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val apk = file(record)
                    if (!apk.isFile || !apk.canRead() || apk.length() == 0L) DownloadStatus.MISSING
                    else if (!checksumMatches(record, apk)) DownloadStatus.INVALID
                    else DownloadStatus.READY
                }
                else -> DownloadStatus.FAILED
            }
            val message = when (status) {
                DownloadStatus.FAILED -> failureMessage(cursor.number(DownloadManager.COLUMN_REASON).toInt())
                DownloadStatus.PAUSED -> "Menunggu jaringan atau dijeda oleh sistem"
                DownloadStatus.INVALID -> "APK berbeda dari checksum katalog. Unduh ulang sebelum install."
                DownloadStatus.MISSING -> "File APK sudah tidak tersedia. Silakan unduh ulang."
                else -> null
            }
            return DownloadItem(record, status, downloaded, total, message,
                fileSizeBytes = if (status in setOf(DownloadStatus.READY, DownloadStatus.INVALID)) file(record).length() else null)
        }
        return DownloadItem(record, DownloadStatus.MISSING)
    }

    private fun checksumMatches(record: DownloadRecord, apk: File): Boolean {
        val expected = record.sha256 ?: return true
        if (!expected.matches(Regex("[A-Fa-f0-9]{64}"))) return false
        val fingerprint = "${apk.length()}:${apk.lastModified()}:$expected"
        verified[record.key]?.takeIf { it.first == fingerprint }?.let { return it.second }
        val digest = MessageDigest.getInstance("SHA-256")
        apk.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        return actual.equals(expected, ignoreCase = true).also { verified[record.key] = fingerprint to it }
    }

    private fun Cursor.number(column: String) = getLong(getColumnIndexOrThrow(column))
    private fun failureMessage(reason: Int): String = when (reason) {
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Ruang penyimpanan tidak cukup"
        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Penyimpanan tidak tersedia"
        DownloadManager.ERROR_HTTP_DATA_ERROR -> "Koneksi terputus saat mengunduh"
        in 400..599 -> "Server APK mengembalikan HTTP $reason"
        else -> "Unduhan gagal (kode $reason). Periksa koneksi lalu coba lagi."
    }
}
