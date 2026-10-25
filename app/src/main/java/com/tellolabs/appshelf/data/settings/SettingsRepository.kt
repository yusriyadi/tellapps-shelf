package com.tellolabs.appshelf.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.preferences by preferencesDataStore("app_shelf")
enum class ThemeMode(val label: String) { SYSTEM("Ikuti sistem"), LIGHT("Terang"), DARK("Gelap") }

class SettingsRepository(context: Context) {
    val store = context.preferences
    private val themeKey = stringPreferencesKey("theme")
    val theme = store.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: ThemeMode.SYSTEM
    }
    suspend fun setTheme(mode: ThemeMode) { store.edit { it[themeKey] = mode.name } }
}
