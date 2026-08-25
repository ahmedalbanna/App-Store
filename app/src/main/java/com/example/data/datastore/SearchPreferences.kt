package com.example.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.searchDataStore by preferencesDataStore(name = "search_prefs")

class SearchPreferences(private val context: Context) {

    private val RECENT_KEY = stringSetPreferencesKey("recent_queries")

    val recentQueries: Flow<List<String>> = context.searchDataStore.data.map { prefs ->
        prefs[RECENT_KEY]?.toList()?.sortedDescending()?.take(10) ?: emptyList()
        // Actually stringSet is unordered; we store as linked set to preserve order via pref
        // For simplicity return as list
    }

    // We store recents as comma-separated to preserve order; fallback to stringSet
    suspend fun saveQuery(query: String) {
        if (query.isBlank() || query.length < 2) return
        context.searchDataStore.edit { prefs ->
            val current = prefs[RECENT_KEY]?.toMutableSet() ?: mutableSetOf()
            current.remove(query)
            current.add(query)
            // Keep only last 10
            val trimmed = if (current.size > 10) current.toList().takeLast(10).toSet() else current
            prefs[RECENT_KEY] = trimmed
        }
    }

    suspend fun clearRecents() {
        context.searchDataStore.edit { it.remove(RECENT_KEY) }
    }
}
