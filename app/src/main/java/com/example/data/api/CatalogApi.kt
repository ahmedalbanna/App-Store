package com.example.data.api

import com.example.data.database.MarketplaceItem
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Phase 4.1: Remote catalog — paginated backend (Retrofit + Moshi).
 * Room remains cache; RemoteMediator handles sync. Falls back to local seed.
 */
@JsonClass(generateAdapter = true)
data class CatalogResponse(
    @Json(name = "items") val items: List<MarketplaceItem> = emptyList(),
    @Json(name = "page") val page: Int = 1,
    @Json(name = "totalPages") val totalPages: Int = 1,
    @Json(name = "totalItems") val totalItems: Int = 0
)

interface CatalogApiService {
    @GET("v1/catalog")
    suspend fun getCatalog(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("type") type: String? = null
    ): CatalogResponse

    @GET("v1/catalog/updates")
    suspend fun getUpdates(
        @Query("sinceVersion") sinceVersion: Int = 1
    ): CatalogResponse
}

object CatalogRetrofitClient {
    private const val BASE_URL = "https://api.alwaha.example.com/"

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: CatalogApiService by lazy {
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create())
            .build()
        retrofit.create(CatalogApiService::class.java)
    }
}
