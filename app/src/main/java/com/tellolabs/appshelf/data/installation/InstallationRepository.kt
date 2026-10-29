package com.tellolabs.appshelf.data.installation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import com.tellolabs.appshelf.model.AppInstallation
import com.tellolabs.appshelf.model.CatalogApp
import com.tellolabs.appshelf.model.InstallationStatus
import com.tellolabs.appshelf.model.compareInstalledVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext

class InstallationRepository(private val context: Context) {
    // Keep in sync with <queries> in AndroidManifest.xml. A missing package outside
    // this set may be hidden on Android 11+, so its absence cannot be confirmed.
    private val queryablePackages = setOf(
        "com.tellolabs.localguard",
        "com.tellolabs.appfin",
        "id.tellolabs.launcher",
    )

    suspend fun check(apps: List<CatalogApp>): Map<String, AppInstallation> = withContext(Dispatchers.IO) {
        apps.associate { app -> app.id to checkApp(app) }
    }

    private fun checkApp(app: CatalogApp): AppInstallation {
        val packageName = app.packageName?.takeIf { it.isNotBlank() } ?: return AppInstallation()
        return try {
            val manager = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= 33) {
                manager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                manager.getPackageInfo(packageName, 0)
            }
            val installedCode = PackageInfoCompat.getLongVersionCode(info)
            AppInstallation(
                status = compareInstalledVersion(installedCode, app.releases.firstOrNull()?.versionCode),
                versionName = info.versionName,
                versionCode = installedCode,
                canOpen = try {
                    manager.getLaunchIntentForPackage(packageName) != null
                } catch (_: RuntimeException) { false },
            )
        } catch (_: PackageManager.NameNotFoundException) {
            val absenceIsReliable = Build.VERSION.SDK_INT < 30 || packageName == context.packageName || packageName in queryablePackages
            AppInstallation(if (absenceIsReliable) InstallationStatus.NOT_INSTALLED else InstallationStatus.UNKNOWN)
        } catch (_: RuntimeException) {
            // A denied or unavailable package query is not evidence of absence.
            AppInstallation()
        }
    }

    // Collected only while the UI is RESUMED. Register before the initial check
    // so installs/removals during that check trigger another read from the OS.
    fun packageChanges(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { trySend(Unit) }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        trySend(Unit)
        awaitClose { context.unregisterReceiver(receiver) }
    }.conflate()
}
