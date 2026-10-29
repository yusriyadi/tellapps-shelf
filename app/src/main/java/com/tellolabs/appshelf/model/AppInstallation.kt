package com.tellolabs.appshelf.model

enum class InstallationStatus(val label: String) {
    UNKNOWN("Status tidak diketahui"),
    NOT_INSTALLED("Belum terpasang"),
    INSTALLED("Terpasang"),
    UPDATE_AVAILABLE("Update tersedia"),
    NEWER_INSTALLED("Versi perangkat lebih baru"),
}

data class AppInstallation(
    val status: InstallationStatus = InstallationStatus.UNKNOWN,
    val versionName: String? = null,
    val versionCode: Long? = null,
)

fun compareInstalledVersion(installed: Long, available: Long?): InstallationStatus = when {
    installed < 0 || available == null || available < 0 -> InstallationStatus.UNKNOWN
    installed < available -> InstallationStatus.UPDATE_AVAILABLE
    installed > available -> InstallationStatus.NEWER_INSTALLED
    else -> InstallationStatus.INSTALLED
}
