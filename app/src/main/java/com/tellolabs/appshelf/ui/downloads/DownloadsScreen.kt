package com.tellolabs.appshelf.ui.downloads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tellolabs.appshelf.model.*
import com.tellolabs.appshelf.ui.DownloadActions
import com.tellolabs.appshelf.ui.formatBytes

@Composable
fun DownloadsScreen(
    downloads: List<DownloadItem>, apps: List<CatalogApp>, onInstall: (DownloadRecord) -> Unit,
    onCancel: (DownloadRecord) -> Unit, onRetry: (DownloadRecord) -> Unit, onDelete: (DownloadRecord) -> Unit,
    onCleanOld: (List<DownloadRecord>) -> Unit,
) {
    var deleting by remember { mutableStateOf<DownloadRecord?>(null) }
    var cleaning by remember { mutableStateOf<List<DownloadRecord>?>(null) }
    val catalog = apps.associateBy { it.id }
    val oldFiles = downloads.filter { it.canCleanOld(catalog[it.record.appId]) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Unduhan APK", style = MaterialTheme.typography.headlineSmall)
                Text("File siap install tetap tersedia jika installer dibatalkan.", style = MaterialTheme.typography.bodySmall)
                if (oldFiles.isNotEmpty()) {
                    val bytes = oldFiles.sumOf { it.fileSizeBytes ?: 0L }
                    Text("${oldFiles.size} APK versi lama · ${formatBytes(bytes)}", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { cleaning = oldFiles.map { it.record } }) { Text("Bersihkan APK lama") }
                }
            }
        }
        if (downloads.isEmpty()) item { Text("Belum ada unduhan. Pilih aplikasi dari katalog untuk mulai.", Modifier.padding(vertical = 24.dp)) }
        items(downloads, key = { it.record.key }) { item ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.record.appName, style = MaterialTheme.typography.titleMedium)
                    Text(item.record.versionLabel)
                    item.fileSizeBytes?.let { Text("Ukuran APK: ${formatBytes(it)}", style = MaterialTheme.typography.bodySmall) }
                    if (item.record.isOlderThanLatest(catalog[item.record.appId])) {
                        Text("Versi lama · Rilis yang lebih baru tersedia di katalog", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DownloadActions(item, onInstall = { onInstall(item.record) }, onCancel = { onCancel(item.record) },
                        onRetry = { onRetry(item.record) }, onDelete = { deleting = item.record })
                }
            }
        }
    }
    deleting?.let { record ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Hapus unduhan?") },
            text = { Text("APK ${record.appName} akan dihapus. Unduhan yang masih berjalan ikut dibatalkan.") },
            confirmButton = { TextButton(onClick = { onDelete(record); deleting = null }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Batal") } })
    }
    cleaning?.let { records ->
        AlertDialog(onDismissRequest = { cleaning = null }, title = { Text("Bersihkan ${records.size} APK lama?") },
            text = { Text("File APK versi lama akan dihapus. Aplikasi terpasang tetap tersedia. Unduhan aktif dan APK versi terbaru tidak ikut dibersihkan.") },
            confirmButton = { TextButton(onClick = { onCleanOld(records); cleaning = null }) { Text("Bersihkan") } },
            dismissButton = { TextButton(onClick = { cleaning = null }) { Text("Batal") } })
    }
}
