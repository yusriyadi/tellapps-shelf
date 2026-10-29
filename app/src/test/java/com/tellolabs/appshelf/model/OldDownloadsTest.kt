package com.tellolabs.appshelf.model

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class OldDownloadsTest {
    private val latest = AppRelease(versionCode = 7, apkUrl = "https://example.com/new.apk")
    private val previous = AppRelease(versionCode = 6, apkUrl = "https://example.com/old.apk")
    private val app = CatalogApp("test", "Test", packageName = "com.test", releases = listOf(latest, previous))
    private val record = DownloadRecord("key", 1, "test", "Test", "0.4.1", "https://example.com/old.apk",
        "00000000-0000-0000-0000-000000000000.apk", versionCode = 6, packageName = "com.test")

    @Test fun preservesLatestAndNewerSnapshotsEvenWithDifferentUrls() {
        assertFalse(record.copy(versionCode = 7).isOlderThanLatest(app))
        assertFalse(record.copy(versionCode = 8).isOlderThanLatest(app))
    }

    @Test fun recognizesOldSnapshotAfterReleaseLeavesCatalog() {
        assertTrue(record.isOlderThanLatest(app.copy(releases = listOf(latest))))
    }

    @Test fun neverGuessesAgeFromMissingUrlOrVersionName() {
        assertFalse(record.copy(url = "https://example.com/unknown.apk", versionCode = null).isOlderThanLatest(app))
        assertFalse(record.isOlderThanLatest(null))
        assertFalse(record.copy(packageName = "com.other").isOlderThanLatest(app))
    }

    @Test fun legacyRecordsCanUseExplicitOrderedHistory() {
        assertTrue(record.copy(versionCode = null).isOlderThanLatest(app))
        assertTrue(record.copy(versionCode = null).isOlderThanLatest(app.copy(releases = listOf(latest.copy(versionCode = null), previous.copy(versionCode = null)))))
    }

    @Test fun activeAndMissingFilesAreExcludedFromCleanup() {
        for (status in listOf(DownloadStatus.QUEUED, DownloadStatus.RUNNING, DownloadStatus.PAUSED, DownloadStatus.VERIFYING, DownloadStatus.MISSING, DownloadStatus.CANCELLED)) {
            assertFalse(DownloadItem(record, status).canCleanOld(app))
        }
        assertTrue(DownloadItem(record, DownloadStatus.READY).canCleanOld(app))
        assertTrue(DownloadItem(record, DownloadStatus.INVALID).canCleanOld(app))
    }

    @Test fun legacyDownloadJsonStillDecodes() {
        val restored = Json.decodeFromString<DownloadRecord>("""{"key":"key","managerId":1,"appId":"test","appName":"Test","versionLabel":"old","url":"https://example.com/old.apk","fileName":"file.apk"}""")
        assertNull(restored.versionCode)
        assertNull(restored.packageName)
    }
}
