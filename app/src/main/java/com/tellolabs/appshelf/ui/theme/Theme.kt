package com.tellolabs.appshelf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tellolabs.appshelf.data.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF46644C), onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E9CC), onPrimaryContainer = Color(0xFF17391F),
    secondary = Color(0xFF536354), background = Color(0xFFF7F9F3), surface = Color(0xFFF7F9F3),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFAACEAF), onPrimary = Color(0xFF15371E),
    primaryContainer = Color(0xFF2E4D35), onPrimaryContainer = Color(0xFFC8E9CC),
    secondary = Color(0xFFBACBBA), background = Color(0xFF101510), surface = Color(0xFF101510),
)

@Composable
fun AppShelfTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.DARK -> true; ThemeMode.LIGHT -> false }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
