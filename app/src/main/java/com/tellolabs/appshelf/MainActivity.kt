package com.tellolabs.appshelf

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tellolabs.appshelf.data.settings.ThemeMode
import com.tellolabs.appshelf.model.isHttpsUrl
import com.tellolabs.appshelf.platform.install.InstallCoordinator
import com.tellolabs.appshelf.ui.AppViewModel
import com.tellolabs.appshelf.ui.navigation.AppNavigation
import com.tellolabs.appshelf.ui.theme.AppShelfTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as AppShelfApplication).container
        val installer = InstallCoordinator(this)
        setContent {
            val vm: AppViewModel = viewModel(factory = AppViewModel.factory(container))
            val mode by vm.theme.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && systemDark)
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            LaunchedEffect(lifecycle, vm) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { vm.observeDownloads() }
            }
            LaunchedEffect(lifecycle, vm) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { vm.observeInstallations() }
            }
            val scope = rememberCoroutineScope()
            var installing by remember { mutableStateOf(false) }
            val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                vm.showMessage(if (installer.canInstall()) "Izin aktif. Ketuk Install APK untuk melanjutkan."
                    else "Izin sumber instalasi belum aktif. APK tetap tersedia di Unduhan.")
            }
            AppShelfTheme(mode) {
                AppNavigation(vm, onInstall = { record ->
                    if (!installer.canInstall()) {
                        try {
                            permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                        } catch (_: ActivityNotFoundException) {
                            vm.showMessage("Pengaturan sumber instalasi tidak tersedia.")
                        }
                    } else if (!installing) {
                        installing = true
                        scope.launch {
                            try { installer.install(vm.installFile(record))
                            } catch (e: CancellationException) { throw e
                            } catch (e: Exception) { vm.showMessage(e.message ?: "Installer gagal dibuka.")
                            } finally { installing = false }
                        }
                    }
                }, onOpenUrl = { url ->
                    if (url.isHttpsUrl()) {
                        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (_: ActivityNotFoundException) { vm.showMessage("Browser tidak tersedia di perangkat ini.") }
                    }
                })
            }
        }
    }
}
