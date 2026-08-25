package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketplaceDao {
    @Query("SELECT * FROM marketplace_items")
    fun getAllItems(): Flow<List<MarketplaceItem>>

    @Query("SELECT * FROM marketplace_items WHERE type = :type")
    fun getItemsByType(type: String): Flow<List<MarketplaceItem>>

    @Query("SELECT * FROM marketplace_items WHERE id = :id")
    fun getItemByIdFlow(id: String): Flow<MarketplaceItem?>

    @Query("SELECT * FROM marketplace_items WHERE id = :id")
    suspend fun getItemById(id: String): MarketplaceItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: MarketplaceItem)

    @Query("SELECT COUNT(*) FROM marketplace_items")
    suspend fun getItemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MarketplaceItem>)

    @Update
    suspend fun updateItem(item: MarketplaceItem)

    @Query("UPDATE marketplace_items SET downloadState = :state, downloadProgress = :progress WHERE id = :id")
    suspend fun updateDownloadState(id: String, state: String, progress: Int)

    @Query("UPDATE marketplace_items SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateIsBookmarked(id: String, isBookmarked: Boolean)

    @Query("UPDATE marketplace_items SET lastReadPage = :lastPage WHERE id = :id")
    suspend fun updateBookProgress(id: String, lastPage: Int)

    @Query("UPDATE marketplace_items SET isInstalled = :isInstalled, installedVersionCode = :installedVersion WHERE id = :id")
    suspend fun updateInstallationState(id: String, isInstalled: Boolean, installedVersion: Int)
}
