package com.binarygeek119.covelounge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.settingsDataStore by preferencesDataStore(name = "covelounge_settings")

class SettingsStore(private val context: Context) {
    private val KEY_PREVIEW_MODE = stringPreferencesKey("preview_mode")
    private val KEY_SCREENSHOT_COUNT = intPreferencesKey("screenshot_count")
    private val KEY_SCREENSHOT_INTERVAL = intPreferencesKey("screenshot_interval")
    private val KEY_USE_TRANSCODE = booleanPreferencesKey("use_transcode")
    private val KEY_TRANSCODE_RESOLUTION = stringPreferencesKey("transcode_resolution")
    private val KEY_RATINGS_ON_EXIT = booleanPreferencesKey("ratings_on_exit")
    private val KEY_APP_PIN = stringPreferencesKey("app_pin")

    val previewMode: Flow<String> = context.settingsDataStore.data.map { it[KEY_PREVIEW_MODE] ?: "Off" }
    val screenshotCount: Flow<Int> = context.settingsDataStore.data.map { it[KEY_SCREENSHOT_COUNT] ?: 10 }
    val screenshotInterval: Flow<Int> = context.settingsDataStore.data.map { it[KEY_SCREENSHOT_INTERVAL] ?: 10 }
    val useTranscode: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_USE_TRANSCODE] ?: false }
    val transcodeResolution: Flow<String> = context.settingsDataStore.data.map { it[KEY_TRANSCODE_RESOLUTION] ?: "720p" }
    val ratingsOnExit: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_RATINGS_ON_EXIT] ?: false }
    val appPin: Flow<String?> = context.settingsDataStore.data.map { it[KEY_APP_PIN] }

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

    suspend fun setAppPin(pin: String) {
        val hash = hashPin(pin)
        context.settingsDataStore.edit { it[KEY_APP_PIN] = hash }
    }

    suspend fun removeAppPin() {
        context.settingsDataStore.edit { it.remove(KEY_APP_PIN) }
    }

    companion object {
        fun hashPin(pin: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(pin.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
