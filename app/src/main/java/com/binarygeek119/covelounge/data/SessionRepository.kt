package com.binarygeek119.covelounge.data

import android.content.Context
import com.binarygeek119.covelounge.data.network.CoveApi
import kotlinx.coroutines.flow.MutableStateFlow
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
        val client = OkHttpClient.Builder().apply {
            if (token != null) {
                addInterceptor { chain ->
                    val req = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                    chain.proceed(req)
                }
            }
        }.build()
        return Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CoveApi::class.java)
    }
}
