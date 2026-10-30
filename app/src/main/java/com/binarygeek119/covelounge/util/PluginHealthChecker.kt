package com.binarygeek119.covelounge.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object PluginHealthChecker {
    suspend fun check(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val url = "${baseUrl.trimEnd('/')}/api/plugins/com.binarygeek119.cove-lounge/health"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }
}