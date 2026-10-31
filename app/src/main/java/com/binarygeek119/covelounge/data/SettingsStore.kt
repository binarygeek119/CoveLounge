package com.binarygeek119.covelounge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "covelounge_settings")

class SettingsStore(private val context: Context) {
    private val KEY_PREVIEW_MODE = stringPreferencesKey("preview_mode")
    private val KEY_SCREENSHOT_COUNT = intPreferencesKey("screenshot_count")
    private val KEY_SCREENSHOT_INTERVAL = intPreferencesKey("screenshot_interval")
    private val KEY_USE_TRANSCODE = booleanPreferencesKey("use_transcode")
    private val KEY_TRANSCODE_RESOLUTION = stringPreferencesKey("transcode_resolution")
    private val KEY_RATINGS_ON_EXIT = booleanPreferencesKey("ratings_on_exit")

    val previewMode: Flow<String> = context.settingsDataStore.data.map { it[KEY_PREVIEW_MODE] ?: "Off" }
    val screenshotCount: Flow<Int> = context.settingsDataStore.data.map { it[KEY_SCREENSHOT_COUNT] ?: 10 }
    val screenshotInterval: Flow<Int> = context.settingsDataStore.data.map { it[KEY_SCREENSHOT_INTERVAL] ?: 10 }
    val useTranscode: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_USE_TRANSCODE] ?: false }
    val transcodeResolution: Flow<String> = context.settingsDataStore.data.map { it[KEY_TRANSCODE_RESOLUTION] ?: "720p" }
    val ratingsOnExit: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_RATINGS_ON_EXIT] ?: false }

    suspend fun setPreviewMode(mode: String) {
        context.settingsDataStore.edit { it[KEY_PREVIEW_MODE] = mode }
    }

    suspend fun setScreenshotCount(count: Int) {
        context.settingsDataStore.edit { it[KEY_SCREENSHOT_COUNT] = count.coerceIn(1, 30) }
    }

    suspend fun setScreenshotInterval(seconds: Int) {
        context.settingsDataStore.edit { it[KEY_SCREENSHOT_INTERVAL] = seconds.coerceIn(10, 120) }
    }

    suspend fun setUseTranscode(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_USE_TRANSCODE] = enabled }
    }

    suspend fun setTranscodeResolution(resolution: String) {
        context.settingsDataStore.edit { it[KEY_TRANSCODE_RESOLUTION] = resolution }
    }

    suspend fun setRatingsOnExit(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_RATINGS_ON_EXIT] = enabled }
    }
}
