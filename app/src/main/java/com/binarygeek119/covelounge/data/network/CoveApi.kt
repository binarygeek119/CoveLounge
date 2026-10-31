package com.binarygeek119.covelounge.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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
data class PerformerSummaryDto(
    val id: Int = 0,
    val name: String? = null,
    val imagePath: String? = null
)

@Serializable
data class VideoDto(
    val id: Int,
    val title: String? = null,
    val details: String? = null,
    val imagePath: String? = null,
    val performers: List<PerformerSummaryDto>? = null
)

@Serializable
data class AudioDto(
    val id: Int,
    val title: String? = null,
    val details: String? = null,
    val imagePath: String? = null
)

@Serializable
data class ImageDto(
    val id: Int,
    val title: String? = null,
    val details: String? = null
)

@Serializable
data class GalleryDto(
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
        @Query("perPage") perPage: Int = 25,
        @Query("sort") sort: String? = null,
        @Query("direction") direction: String? = null,
        @Query("seed") seed: Int? = null,
        @Query("q") q: String? = null
    ): PaginatedResponse<VideoDto>

    @GET("audios")
    suspend fun audios(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 25,
        @Query("sort") sort: String? = null,
        @Query("direction") direction: String? = null,
        @Query("seed") seed: Int? = null,
        @Query("q") q: String? = null
    ): PaginatedResponse<AudioDto>

    @GET("images")
    suspend fun images(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 25,
        @Query("sort") sort: String? = null,
        @Query("direction") direction: String? = null,
        @Query("seed") seed: Int? = null,
        @Query("q") q: String? = null
    ): PaginatedResponse<ImageDto>

    @GET("videos/{id}")
    suspend fun videoDetail(@retrofit2.http.Path("id") id: Int): VideoDto

    @POST("videos/{id}/play")
    suspend fun recordVideoPlay(@retrofit2.http.Path("id") id: Int): retrofit2.Response<Unit>

    @POST("videos/{id}/like")
    suspend fun likeVideo(@retrofit2.http.Path("id") id: Int): Int

    @GET("audios/{id}")
    suspend fun audioDetail(@retrofit2.http.Path("id") id: Int): AudioDto

    @POST("audios/{id}/play")
    suspend fun recordAudioPlay(@retrofit2.http.Path("id") id: Int): retrofit2.Response<Unit>

    @POST("audios/{id}/like")
    suspend fun likeAudio(@retrofit2.http.Path("id") id: Int): Int

    @GET("images/{id}")
    suspend fun imageDetail(@retrofit2.http.Path("id") id: Int): ImageDto

    @GET("galleries")
    suspend fun galleries(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 25,
        @Query("sort") sort: String? = null,
        @Query("direction") direction: String? = null,
        @Query("q") q: String? = null
    ): PaginatedResponse<GalleryDto>

    @GET("galleries/{id}")
    suspend fun galleryDetail(@retrofit2.http.Path("id") id: Int): GalleryDto

    @POST("images/{id}/like")
    suspend fun likeImage(@retrofit2.http.Path("id") id: Int): Int

    @GET("engagement/{hostType}/{id}")
    suspend fun getEngagement(
        @retrofit2.http.Path("hostType") hostType: String,
        @retrofit2.http.Path("id") id: Int
    ): EntityEngagementDto

    @PUT("engagement/{hostType}/{id}/favorite")
    suspend fun setFavorite(
        @retrofit2.http.Path("hostType") hostType: String,
        @retrofit2.http.Path("id") id: Int,
        @Body body: EntityFavoriteDto
    ): EntityEngagementDto

    @GET("performers")
    suspend fun performers(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 30,
        @Query("sort") sort: String? = null,
        @Query("direction") direction: String? = null
    ): PaginatedResponse<PerformerDto>
}

@Serializable
data class EntityEngagementDto(
    val hostId: Int = 0,
    val isFavorite: Boolean = false,
    val playCount: Int = 0
)

@Serializable
data class EntityFavoriteDto(val isFavorite: Boolean)

@Serializable
data class PerformerDto(
    val id: Int = 0,
    val name: String? = null,
    val imagePath: String? = null
)
