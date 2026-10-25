package com.tellolabs.appshelf.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tellolabs.appshelf.data.catalog.CATALOG_URL
import com.tellolabs.appshelf.data.settings.ThemeMode

@Composable
fun SettingsScreen(mode: ThemeMode, onTheme: (ThemeMode) -> Unit, onClearCache: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Tema", style = MaterialTheme.typography.titleLarge)
        ThemeMode.entries.forEach { theme ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = mode == theme, onClick = { onTheme(theme) }, role = Role.RadioButton).padding(vertical = 8.dp)) {
                RadioButton(selected = mode == theme, onClick = null)
                Text(theme.label, Modifier.padding(start = 12.dp, top = 2.dp))
            }
        }
        HorizontalDivider()
        Text("Sumber katalog", style = MaterialTheme.typography.titleMedium)
        Text(CATALOG_URL, style = MaterialTheme.typography.bodySmall)
        Text("APK diunduh dari URL rilis pada katalog. TellApps Shelf tidak memasang APK secara otomatis.")
        OutlinedButton(onClick = { confirm = true }) { Text("Bersihkan cache katalog") }
        Text("Pembersihan cache tidak menghapus APK. Kelola file APK dari halaman Unduhan.", style = MaterialTheme.typography.bodySmall)
        Text("TellApps Shelf 0.1.0", style = MaterialTheme.typography.labelLarge)
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Bersihkan cache katalog?") },
        text = { Text("Katalog offline akan tersedia lagi setelah refresh berikutnya berhasil.") },
        confirmButton = { TextButton(onClick = { onClearCache(); confirm = false }) { Text("Bersihkan") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Batal") } })
}
