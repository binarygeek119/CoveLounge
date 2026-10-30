package com.binarygeek119.covelounge.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val LATEST_URL = "https://api.github.com/repos/binarygeek119/CoveLounge/releases/latest"

    data class ReleaseInfo(val tag: String, val apkUrl: String?)

    suspend fun checkLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(LATEST_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            if (conn.responseCode != 200) return@withContext null
            val text = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(text)
            val tag = json.optString("tag_name")
            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    val name = a.optString("name")
                    if (name.endsWith(".apk")) {
                        apkUrl = a.optString("browser_download_url")
                        break
                    }
                }
            }
            ReleaseInfo(tag, apkUrl)
        } catch (_: Exception) {
            null
        }
    }

    fun isNewer(current: String, latestTag: String): Boolean {
        val c = current.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val l = latestTag.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(c.size, l.size)) {
            val cv = c.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }

    suspend fun downloadApk(context: Context, url: String): File? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.cacheDir, "update.apk")
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 60000
            conn.inputStream.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file
        } catch (_: Exception) {
            null
        }
    }

    fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}