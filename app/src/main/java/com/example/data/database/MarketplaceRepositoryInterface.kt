package com.example.data.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for marketplace data operations.
 * Extracted for testability and DI (Phase 1.2).
 */
interface MarketplaceRepository {
    val allItems: Flow<List<MarketplaceItem>>

    fun getItemsByType(type: String): Flow<List<MarketplaceItem>>
    fun getItemById(id: String): Flow<MarketplaceItem?>

    suspend fun checkAndPrepulateDatabase()

    /** Starts simulated download; tracked via Job map so it can be cancelled. */
    fun triggerDownloadProgress(itemId: String, scope: CoroutineScope)

    suspend fun installPackage(itemId: String)

    suspend fun resetDownloadState(itemId: String)

    suspend fun toggleBookmark(itemId: String)

    suspend fun saveBookProgress(itemId: String, lastPage: Int)

    /** Cancel ongoing download job if any */
    fun cancelDownload(itemId: String)

    /** For testing / cleanup */
    fun cancelAllDownloads()
}
