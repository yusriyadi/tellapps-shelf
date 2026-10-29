package com.tellolabs.appshelf.ui.detail

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tellolabs.appshelf.model.*
import com.tellolabs.appshelf.ui.AppIcon
import com.tellolabs.appshelf.ui.InstallationLabel
import com.tellolabs.appshelf.ui.DownloadActions
import com.tellolabs.appshelf.ui.formatBytes
import com.tellolabs.appshelf.ui.formatReleaseDate

@Composable
fun DetailScreen(
    app: CatalogApp?, installation: AppInstallation, downloads: List<DownloadItem>, onDownload: (AppRelease) -> Unit,
    onInstall: (DownloadRecord) -> Unit, onCancel: (DownloadRecord) -> Unit,
    onRetry: (DownloadRecord) -> Unit, onRepository: (String) -> Unit, onOpenApp: (String) -> Unit,
) {
    if (app == null) {
        Text("Aplikasi tidak ditemukan dalam katalog. Kembali lalu refresh katalog.", Modifier.padding(24.dp))
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppIcon(app)
                    Column(Modifier.weight(1f)) {
                        Text(app.name, style = MaterialTheme.typography.headlineSmall)
                        Text(app.packageName ?: "Package name belum tersedia", style = MaterialTheme.typography.bodySmall)
                    }
                }
                InstallationLabel(installation)
                installation.versionCode?.let { code ->
                    Text("Versi perangkat: ${installation.versionName?.takeIf { it.isNotBlank() } ?: "Tanpa nama versi"} (code $code)",
                        style = MaterialTheme.typography.bodySmall)
                }
                app.packageName?.takeIf { it.isNotBlank() && installation.canOpen }?.let { packageName ->
                    FilledTonalButton(onClick = { onOpenApp(packageName) }) { Text("Buka aplikasi") }
                }
                Text(app.description ?: "Deskripsi belum tersedia")
                if (app.repositoryUrl.isHttpsUrl()) {
                    OutlinedButton(onClick = { onRepository(requireNotNull(app.repositoryUrl)) }) { Text("Buka repository") }
                }
                Text("Riwayat rilis", style = MaterialTheme.typography.titleLarge)
                if (app.releases.isEmpty()) Text("Belum ada rilis yang tersedia.")
            }
        }
        itemsIndexed(app.releases) { index, release ->
            OutlinedCard(Modifier.fillMaxWidth(), colors = CardDefaults.outlinedCardColors(
                containerColor = if (index == 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface,
            )) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (index == 0) Text("Rilis terbaru", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(release.label, style = MaterialTheme.typography.titleMedium)
                    release.tag?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    release.versionCode?.let { Text("Version code: $it") }
                    formatReleaseDate(release.releasedAt)?.let { Text("Dirilis $it") }
                    release.minSdk?.let { Text("Minimum Android API $it") }
                    val item = downloads.firstOrNull { it.record.appId == app.id && it.record.url == release.apkUrl }
                    val size = item?.fileSizeBytes ?: item?.totalBytes?.takeIf { it > 0 } ?: release.sizeBytes?.takeIf { it >= 0 }
                    Text(size?.let { "Ukuran APK: ${formatBytes(it)}" } ?: "Ukuran APK belum tersedia", style = MaterialTheme.typography.bodySmall)
                    Text(if (index == 0) "Yang baru" else "Catatan rilis", style = MaterialTheme.typography.titleSmall)
                    if (release.changelog.isEmpty()) Text("Catatan rilis belum tersedia")
                    else release.changelog.forEach { Text("• $it") }
                    val compatible = release.minSdk == null || release.minSdk <= Build.VERSION.SDK_INT
                    val validUrl = release.apkUrl.isHttpsUrl()
                    if (!compatible) Text("Tidak kompatibel dengan versi Android perangkat ini.", color = MaterialTheme.colorScheme.error)
                    if (item != null) {
                        DownloadActions(item, onInstall = { onInstall(item.record) },
                            onCancel = { onCancel(item.record) }, onRetry = { if (compatible && validUrl) onRetry(item.record) })
                    } else {
                        Button(onClick = { onDownload(release) }, enabled = compatible && validUrl) {
                            Text(if (index == 0 && installation.status == InstallationStatus.UPDATE_AVAILABLE) "Download update" else "Download APK")
                        }
                    }
                    if (!validUrl) Text("URL APK belum tersedia atau tidak valid.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("Instalasi dikonfirmasi melalui installer Android. Jika APK ditolak, periksa kompatibilitas dan signing aplikasi.", style = MaterialTheme.typography.bodySmall) }
    }
}
