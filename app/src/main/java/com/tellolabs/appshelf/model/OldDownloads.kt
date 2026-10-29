package com.tellolabs.appshelf.model

fun DownloadRecord.isOlderThanLatest(app: CatalogApp?): Boolean {
    if (app == null || app.id != appId) return false
    if (packageName != null && packageName != app.packageName) return false
    val latest = app.releases.firstOrNull() ?: return false
    val index = app.releases.indexOfFirst { it.apkUrl == url }
    val downloadedCode = versionCode ?: app.releases.getOrNull(index)?.versionCode
    val latestCode = latest.versionCode
    if ((downloadedCode != null && downloadedCode < 0) || (latestCode != null && latestCode < 0)) return false
    if (downloadedCode != null && latestCode != null) {
        return downloadedCode >= 0 && latestCode >= 0 && downloadedCode < latestCode
    }
    // Legacy downloads have no version snapshot. Only infer age when the URL
    // is explicitly present in the ordered history; an absent URL proves nothing.
    return index > 0
}

fun DownloadItem.canCleanOld(app: CatalogApp?): Boolean =
    !active && status in setOf(DownloadStatus.READY, DownloadStatus.INVALID) && record.isOlderThanLatest(app)
