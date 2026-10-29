package com.tellolabs.appshelf.ui.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tellolabs.appshelf.R
import com.tellolabs.appshelf.model.CatalogApp
import com.tellolabs.appshelf.model.AppInstallation
import com.tellolabs.appshelf.ui.InstallationLabel
import com.tellolabs.appshelf.ui.AppIcon
import com.tellolabs.appshelf.ui.CatalogUiState
import java.text.DateFormat
import java.util.Date

@Composable
fun CatalogScreen(state: CatalogUiState, installations: Map<String, AppInstallation>, onRefresh: () -> Unit, onOpen: (String) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    val apps = state.snapshot?.catalog?.apps.orEmpty()
    val filtered = apps.filter { it.name.contains(search, ignoreCase = true) || it.description.orEmpty().contains(search, ignoreCase = true) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(280.dp),
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(value = search, onValueChange = { search = it },
                label = { Text("Cari aplikasi") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                trailingIcon = { if (search.isNotEmpty()) TextButton(onClick = { search = "" }) { Text("Reset") } })
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${filtered.size} aplikasi", modifier = Modifier.weight(1f).padding(top = 14.dp))
                    IconButton(onClick = onRefresh, enabled = !state.loading) {
                        Icon(painterResource(R.drawable.ic_refresh), contentDescription = "Refresh katalog")
                    }
                }
                state.snapshot?.let {
                    if (it.syncedAt > 0) {
                        val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it.syncedAt))
                        Text("${if (state.fromCache) "Cache · " else ""}Sinkronisasi $time", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    if (state.snapshot == null) Button(onClick = onRefresh, enabled = !state.loading) { Text("Coba lagi") }
                }
            }
        }
        if (filtered.isEmpty() && !state.loading && state.snapshot != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (search.isBlank()) "Belum ada aplikasi di katalog." else "Tidak ada aplikasi yang cocok.")
                    if (search.isNotEmpty()) OutlinedButton(onClick = { search = "" }) { Text("Reset pencarian") }
                }
            }
        }
        items(filtered, key = { it.id }) { app -> CatalogCard(app, installations[app.id] ?: AppInstallation()) { onOpen(app.id) } }
    }
}

@Composable
private fun CatalogCard(app: CatalogApp, installation: AppInstallation, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIcon(app)
                Column(Modifier.weight(1f)) {
                    Text(app.name, style = MaterialTheme.typography.titleMedium)
                    Text(app.releases.firstOrNull()?.label ?: "Belum ada rilis", style = MaterialTheme.typography.bodySmall)
                }
            }
            InstallationLabel(installation)
            Text(app.description ?: "Deskripsi belum tersedia", maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}
