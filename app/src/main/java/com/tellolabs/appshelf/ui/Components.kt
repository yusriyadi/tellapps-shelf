package com.tellolabs.appshelf.ui

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import com.tellolabs.appshelf.R
import com.tellolabs.appshelf.BuildConfig
import coil3.compose.SubcomposeAsyncImage
import com.tellolabs.appshelf.model.*

@Composable
fun AppIcon(app: CatalogApp) {
    val modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
    val knownIcon = when (app.packageName) {
        "com.tellolabs.localguard" -> R.drawable.ic_catalog_localguard
        "com.tellolabs.appfin" -> R.drawable.ic_catalog_appfin
        "id.tellolabs.launcher" -> R.drawable.ic_catalog_tellauncher
        BuildConfig.APPLICATION_ID -> R.drawable.ic_catalog_shelf
        null -> when (app.id) {
            "localguard" -> R.drawable.ic_catalog_localguard
            "appfin" -> R.drawable.ic_catalog_appfin
            "tellauncher" -> R.drawable.ic_catalog_tellauncher
            "tellapps-shelf" -> R.drawable.ic_catalog_shelf
            else -> null
        }
        else -> null
    }
    val fallback: @Composable () -> Unit = {
        if (knownIcon != null) {
            Image(painterResource(knownIcon), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
        } else {
            Box(modifier.background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(app.name.take(1).uppercase(), style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
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

@Composable
fun InstallationLabel(installation: AppInstallation) {
    Text(
        text = installation.status.label,
        style = MaterialTheme.typography.labelLarge,
        color = if (installation.status == InstallationStatus.UPDATE_AVAILABLE)
            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
