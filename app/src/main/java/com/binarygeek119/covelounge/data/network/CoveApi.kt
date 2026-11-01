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
    val performers: List<PerformerSummaryDto>? = null,
    val rating: Int? = null,
    val files: List<VideoFileDto>? = null,
    val studio: String? = null,
    val container: String? = null,
    val resolution: String? = null
)

@Serializable
data class VideoFileDto(
    val id: Int = 0,
    val duration: Double = 0.0,
    val container: String? = null,
    val videoCodec: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
data class AudioDto(
    val id: Int,
    val title: String? = null,
    val details: String? = null,
    val imagePath: String? = null,
    val rating: Int? = null
)

@Serializable
data class ImageDto(
    val id: Int,
    val title: String? = null,
    val details: String? = null,
    val rating: Int? = null
)

@Serializable
data class GalleryDto(
    val id: Int,
    val title: String? = null,
    val rating: Int? = null
)

@Serializable
data class LoungeAppDto(
    val id: String = "",
    val label: String? = null,
    val serverUrl: String? = null,
    val apiKey: String? = null,
    val resetCodeHash: String? = null,
    val resetCodeExpiresAt: String? = null,
    val resetApproved: Boolean = false
)

@Serializable
data class LoungeResetCodeResponse(
    val code: String = "",
    val expiresAt: String? = null
)

@Serializable
data class LoungeResetVerifyResponse(
    val ok: Boolean = false,
    val error: String? = null
)

@Serializable
data class LoungeRegisterResetResponse(
    val ok: Boolean = false,
    val error: String? = null
)

@Serializable
data class LoungeResetStatusResponse(
    val approved: Boolean = false
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

    @GET("plugins/com.binarygeek119.cove-lounge/apps")
    suspend fun loungeApps(): List<LoungeAppDto>

    @GET("plugins/com.binarygeek119.cove-lounge/apps/{appId}/reset-code")
    suspend fun loungeResetCode(@retrofit2.http.Path("appId") appId: String): LoungeResetCodeResponse

    @POST("plugins/com.binarygeek119.cove-lounge/reset/verify")
    suspend fun loungeResetVerify(@Body body: Map<String, String>): LoungeResetVerifyResponse

    @POST("plugins/com.binarygeek119.cove-lounge/reset/register")
    suspend fun loungeRegisterReset(@Body body: Map<String, String>): LoungeRegisterResetResponse

    @GET("plugins/com.binarygeek119.cove-lounge/reset/status")
    suspend fun loungeResetStatus(): LoungeResetStatusResponse

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

    @PUT("engagement/{hostType}/{id}/rating")
    suspend fun setRating(
        @retrofit2.http.Path("hostType") hostType: String,
        @retrofit2.http.Path("id") id: Int,
        @Body body: VideoRatingDto
    ): EntityEngagementDto

    @GET("performers")
    suspend fun performers(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 30,
        @Query("sort") sort: String? = null,
        @Query("direction") direction: String? = null,
        @Query("q") q: String? = null
    ): PaginatedResponse<PerformerDto>

    @GET("performers/{id}")
    suspend fun performerDetail(@retrofit2.http.Path("id") id: Int): PerformerDto
}

@Serializable
data class EntityEngagementDto(
    val hostId: Int = 0,
    val isFavorite: Boolean = false,
    val rating: Int? = null,
    val playCount: Int = 0,
    val likeCount: Int = 0,
    val resumeTime: Double = 0.0
)

@Serializable
data class EntityFavoriteDto(val isFavorite: Boolean)

@Serializable
data class PerformerDto(
    val id: Int = 0,
    val name: String? = null,
    val imagePath: String? = null,
    val gender: String? = null,
    val born: String? = null,
    val country: String? = null,
    val ethnicity: String? = null,
    val height: String? = null,
    val measurements: String? = null,
    val eyeColor: String? = null,
    val hairColor: String? = null,
    val fakeTits: String? = null,
    val tattoos: String? = null,
    val piercings: String? = null,
    val career: String? = null
)

@Serializable
data class VideoRatingDto(val value: Int? = null, val aspect: String = "overall")
