package com.binarygeek119.covelounge.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(
    val token: String,
    val refreshToken: String? = null,
    val username: String? = null
)

@Serializable
data class MeResponse(
    val user: MeUser? = null,
    val permissions: List<String> = emptyList()
)

@Serializable
data class MeUser(
    val id: String? = null,
    val username: String? = null
)

@Serializable
data class PaginatedResponse<T>(
    val items: List<T> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val perPage: Int = 25
)

@Serializable
data class VideoDto(
    val id: Int,
    val title: String? = null,
    val imagePath: String? = null
)

@Serializable
data class AudioDto(
    val id: Int,
    val title: String? = null,
    val imagePath: String? = null
)

@Serializable
data class ImageDto(
    val id: Int,
    val title: String? = null
)

interface CoveApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: Map<String, String>): LoginResponse

    @GET("auth/me")
    suspend fun me(): MeResponse

    @GET("videos")
    suspend fun videos(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 25
    ): PaginatedResponse<VideoDto>

    @GET("audios")
    suspend fun audios(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 25
    ): PaginatedResponse<AudioDto>

    @GET("images")
    suspend fun images(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 25
    ): PaginatedResponse<ImageDto>
}
