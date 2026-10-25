package com.tellolabs.appshelf.platform.install

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class InstallCoordinator(private val context: Context) {
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun install(file: File) {
        check(file.isFile && file.canRead() && file.length() > 0) { "File APK tidak tersedia. Silakan unduh ulang." }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.clipData = ClipData.newRawUri("APK", uri)
        try { context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            error("Installer APK tidak tersedia di perangkat ini.")
        }
    }
}
