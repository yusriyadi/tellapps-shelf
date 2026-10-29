package com.tellolabs.appshelf.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.tellolabs.appshelf.model.DownloadRecord
import com.tellolabs.appshelf.model.AppInstallation
import com.tellolabs.appshelf.ui.AppViewModel
import com.tellolabs.appshelf.ui.catalog.CatalogScreen
import com.tellolabs.appshelf.ui.detail.DetailScreen
import com.tellolabs.appshelf.ui.downloads.DownloadsScreen
import com.tellolabs.appshelf.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: AppViewModel, onInstall: (DownloadRecord) -> Unit, onOpenUrl: (String) -> Unit, onOpenApp: (String) -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "catalog"
    val main = route == "catalog" || route == "downloads"
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val installations by viewModel.installations.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbars = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbars.showSnackbar(it, withDismissAction = true)
            viewModel.consumeMessage()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(when { route == "settings" -> "Pengaturan"; !main -> "Detail aplikasi"; else -> "TellApps Shelf" }) },
                navigationIcon = { if (!main) TextButton(onClick = { nav.popBackStack() }) { Text("Kembali") } },
                actions = { if (main) TextButton(onClick = { nav.navigate("settings") { launchSingleTop = true } }) { Text("Pengaturan") } })
        },
        bottomBar = {
            if (main) NavigationBar {
                NavigationBarItem(selected = route == "catalog", label = { Text("Katalog") },
                    icon = { Text("▤") }, onClick = {
                        nav.navigate("catalog") { popUpTo("catalog") { saveState = true }; launchSingleTop = true; restoreState = true }
                    })
                NavigationBarItem(selected = route == "downloads", label = { Text("Unduhan") },
                    icon = { Text("↓") }, onClick = {
                        nav.navigate("downloads") { popUpTo("catalog") { saveState = true }; launchSingleTop = true; restoreState = true }
                    })
                composable("settings") { SettingsScreen(theme, viewModel::setTheme, viewModel::clearCache) }
            }
        },
        snackbarHost = { SnackbarHost(snackbars) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
            NavHost(navController = nav, startDestination = "catalog", modifier = Modifier.widthIn(max = if (main && route == "catalog") 1200.dp else 760.dp).fillMaxSize()) {
                composable("catalog") {
                    CatalogScreen(catalog, installations, viewModel::refresh) { nav.navigate("detail/${Uri.encode(it)}") }
                }
                composable("downloads") {
                    DownloadsScreen(downloads, onInstall, viewModel::cancel, viewModel::retry, viewModel::delete)
                }
                composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { detail ->
                    val app = catalog.snapshot?.catalog?.apps?.firstOrNull { it.id == detail.arguments?.getString("id") }
                    DetailScreen(app, installations[app?.id] ?: AppInstallation(), downloads, onDownload = { release -> app?.let { viewModel.download(it, release) } },
                        onInstall = onInstall, onCancel = viewModel::cancel, onRetry = viewModel::retry, onRepository = onOpenUrl, onOpenApp = onOpenApp)
                }
            }
        }
    }
}
