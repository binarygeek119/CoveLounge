package com.binarygeek119.covelounge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "covelounge")

class ServerStore(private val context: Context) {
    private val KEY_URL = stringPreferencesKey("server_url")
    private val KEY_TOKEN = stringPreferencesKey("access_token")
    private val KEY_REFRESH = stringPreferencesKey("refresh_token")

    val serverUrl: Flow<String?> = context.dataStore.data.map { it[KEY_URL] }
    val accessToken: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[KEY_REFRESH] }

    suspend fun save(url: String, token: String, refresh: String?) {
        context.dataStore.edit {
            it[KEY_URL] = url.trimEnd('/')
            it[KEY_TOKEN] = token
            if (refresh != null) it[KEY_REFRESH] = refresh
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
