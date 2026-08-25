package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.api.GeminiApiService
import com.example.data.api.RetrofitClient
import com.example.data.api.CatalogApiService
import com.example.data.api.CatalogRetrofitClient
import com.example.data.auth.AuthRepository
import com.example.data.database.AppDatabase
import com.example.data.database.MARKETPLACE_DB_MIGRATION_1_2
import com.example.data.database.MARKETPLACE_DB_MIGRATION_2_3
import com.example.data.database.MarketplaceDao
import com.example.data.database.MarketplaceRepository
import com.example.data.database.MarketplaceRepositoryImpl
import com.example.data.datastore.BookPreferences
import com.example.data.datastore.SearchPreferences
import com.example.data.telemetry.Telemetry

/**
 * Phase 1.1: Manual DI container (Hilt alternative without Gradle plugin classpath issues).
 * Provides same Singletons as Hilt's DatabaseModule/RepositoryModule but via plain Kotlin.
 * Keeps @Singleton semantics via lazy singletons and @ApplicationContext via constructor.
 */
class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "alwaha_marketplace_db"
        )
            .addMigrations(MARKETPLACE_DB_MIGRATION_1_2, MARKETPLACE_DB_MIGRATION_2_3)
            .build()
    }

    val marketplaceDao: MarketplaceDao by lazy { database.marketplaceDao() }

    val marketplaceRepository: MarketplaceRepository by lazy {
        MarketplaceRepositoryImpl(marketplaceDao, context.applicationContext)
    }

    val searchPreferences: SearchPreferences by lazy { SearchPreferences(context.applicationContext) }
    val bookPreferences: BookPreferences by lazy { BookPreferences(context.applicationContext) }
    val chatDao by lazy { database.chatDao() }
    val telemetry: Telemetry by lazy { Telemetry(context.applicationContext) }
    val authRepository: AuthRepository by lazy { AuthRepository() }
    val catalogApi: CatalogApiService by lazy { CatalogRetrofitClient.service }

    val geminiService: GeminiApiService by lazy { RetrofitClient.service }
}
