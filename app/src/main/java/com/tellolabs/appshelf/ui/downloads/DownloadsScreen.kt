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

@Composable
fun DownloadsScreen(
    downloads: List<DownloadItem>, onInstall: (DownloadRecord) -> Unit,
    onCancel: (DownloadRecord) -> Unit, onRetry: (DownloadRecord) -> Unit, onDelete: (DownloadRecord) -> Unit,
) {
    var deleting by remember { mutableStateOf<DownloadRecord?>(null) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Unduhan APK", style = MaterialTheme.typography.headlineSmall)
            Text("File siap install tetap tersedia jika installer dibatalkan.", style = MaterialTheme.typography.bodySmall)
        }
        if (downloads.isEmpty()) item { Text("Belum ada unduhan. Pilih aplikasi dari katalog untuk mulai.", Modifier.padding(vertical = 24.dp)) }
        items(downloads, key = { it.record.key }) { item ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.record.appName, style = MaterialTheme.typography.titleMedium)
                    Text(item.record.versionLabel)
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
}
