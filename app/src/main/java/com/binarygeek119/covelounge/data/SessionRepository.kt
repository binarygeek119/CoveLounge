package com.binarygeek119.covelounge.data

import android.content.Context
import com.binarygeek119.covelounge.data.network.CoveApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

class SessionRepository(private val store: ServerStore) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _baseUrl = MutableStateFlow<String?>(null)
    private val _token = MutableStateFlow<String?>(null)
    val baseUrl: StateFlow<String?> = _baseUrl.asStateFlow()
    val token: StateFlow<String?> = _token.asStateFlow()

    fun initFromStore(url: String?, token: String?) {
        _baseUrl.value = url
        _token.value = token
    }

    fun createApi(baseUrl: String, token: String?): CoveApi {
        val cleanBase = baseUrl.trimEnd('/')
        val authInterceptor = okhttp3.Interceptor { chain ->
            val t = token
            val req = if (t != null) chain.request().newBuilder().addHeader("Authorization", "Bearer $t").build() else chain.request()
            chain.proceed(req)
        }
        val authenticator = okhttp3.Authenticator { _, response ->
            val currentRefresh = runBlocking { store.refreshToken.first() }
            if (currentRefresh.isNullOrBlank()) return@Authenticator null
            val refreshClient = OkHttpClient()
            val body = okhttp3.RequestBody.create("application/json".toMediaType(), "{\"refreshToken\":\"$currentRefresh\"}")
            val refreshReq = okhttp3.Request.Builder()
                .url("$cleanBase/api/auth/refresh")
                .post(body)
                .build()
            val refreshRes = refreshClient.newCall(refreshReq).execute()
            if (!refreshRes.isSuccessful) return@Authenticator null
            val jsonStr = refreshRes.body?.string() ?: return@Authenticator null
            val newToken = org.json.JSONObject(jsonStr).optString("token")
            val newRefresh = org.json.JSONObject(jsonStr).optString("refreshToken")
            if (newToken.isBlank()) return@Authenticator null
            runBlocking { store.save(cleanBase, newToken, if (newRefresh.isBlank()) null else newRefresh) }
            response.request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .build()
        return Retrofit.Builder()
            .baseUrl("$cleanBase/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CoveApi::class.java)
    }
}
