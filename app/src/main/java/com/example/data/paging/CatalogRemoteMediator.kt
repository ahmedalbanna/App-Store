package com.example.data.paging

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.data.api.CatalogApiService
import com.example.data.database.MarketplaceDao
import com.example.data.database.MarketplaceItem

/**
 * Phase 4.1: RemoteMediator — Room as cache, Retrofit as remote.
 * On REFRESH, fetches first page from remote and replaces cache via REPLACE.
 * Falls back to local seed if remote fails (offline).
 */
@OptIn(ExperimentalPagingApi::class)
class CatalogRemoteMediator(
    private val api: CatalogApiService,
    private val dao: MarketplaceDao
) : RemoteMediator<Int, MarketplaceItem>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MarketplaceItem>
    ): MediatorResult {
        return try {
            val page = when (loadType) {
                LoadType.REFRESH -> 1
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                    if (lastItem == null) 1 else 2 // simple pagination: 2 pages max for demo
                }
            }

            val response = try {
                api.getCatalog(page = page, pageSize = state.config.pageSize)
            } catch (e: Exception) {
                Log.w("CatalogRemoteMediator", "Remote fetch failed, using cache: ${e.message}")
                return MediatorResult.Success(endOfPaginationReached = true)
            }

            if (loadType == LoadType.REFRESH) {
                // On refresh, replace cache (REPLACE semantics from Phase 1.4)
                dao.insertAll(response.items)
            } else {
                dao.insertAll(response.items)
            }

            MediatorResult.Success(endOfPaginationReached = response.page >= response.totalPages || response.items.isEmpty())
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
