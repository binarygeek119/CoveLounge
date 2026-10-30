package com.binarygeek119.covelounge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "covelounge_settings")

class SettingsStore(private val context: Context) {
    private val KEY_PREVIEW_MODE = stringPreferencesKey("preview_mode")
    private val KEY_SCREENSHOT_COUNT = intPreferencesKey("screenshot_count")

    val previewMode: Flow<String> = context.settingsDataStore.data.map { it[KEY_PREVIEW_MODE] ?: "Off" }
    val screenshotCount: Flow<Int> = context.settingsDataStore.data.map { it[KEY_SCREENSHOT_COUNT] ?: 10 }

    suspend fun setPreviewMode(mode: String) {
        context.settingsDataStore.edit { it[KEY_PREVIEW_MODE] = mode }
    }

    suspend fun setScreenshotCount(count: Int) {
        context.settingsDataStore.edit { it[KEY_SCREENSHOT_COUNT] = count.coerceIn(1, 30) }
    }
}
