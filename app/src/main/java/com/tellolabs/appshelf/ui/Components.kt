package com.tellolabs.appshelf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.tellolabs.appshelf.model.*

@Composable
fun AppIcon(app: CatalogApp) {
    val modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
    val fallback: @Composable () -> Unit = {
        Box(modifier.background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(app.name.take(1).uppercase(), style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
    if (app.iconUrl.isHttpsUrl()) {
        SubcomposeAsyncImage(model = app.iconUrl, contentDescription = null, modifier = modifier,
            contentScale = ContentScale.Crop, loading = { fallback() }, error = { fallback() })
    } else fallback()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadActions(
    item: DownloadItem,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(item.status.label, style = MaterialTheme.typography.labelLarge)
        if (item.active) {
            if (item.progress != null) {
                LinearProgressIndicator(progress = { item.progress ?: 0f }, modifier = Modifier.fillMaxWidth())
                Text("${((item.progress ?: 0f) * 100).toInt()}% · ${formatBytes(item.downloadedBytes)} / ${formatBytes(item.totalBytes)}")
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("${formatBytes(item.downloadedBytes)} diunduh")
            }
        }
        item.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                item.status == DownloadStatus.READY -> Button(onClick = onInstall) { Text("Install APK") }
                item.active -> OutlinedButton(onClick = onCancel) { Text("Batalkan") }
                else -> Button(onClick = onRetry) { Text("Coba lagi") }
            }
            onDelete?.let { TextButton(onClick = it) { Text("Hapus") } }
        }
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes < 0 -> "Ukuran belum diketahui"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
}
