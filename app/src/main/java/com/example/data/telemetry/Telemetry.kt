package com.example.data.telemetry

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.telemetryDataStore by preferencesDataStore(name = "telemetry_prefs")

/**
 * Phase 3.4: Telemetry opt-in — logs anonymous search→install conversion, no PII.
 * Opt-in stored in DataStore; when disabled, all log calls are no-ops.
 */
class Telemetry(private val context: Context) {

    private val OPT_IN_KEY = booleanPreferencesKey("telemetry_opt_in")

    val isOptedIn: Flow<Boolean> = context.telemetryDataStore.data.map { it[OPT_IN_KEY] ?: false }

    suspend fun setOptIn(enabled: Boolean) {
        context.telemetryDataStore.edit { it[OPT_IN_KEY] = enabled }
        Log.d("Telemetry", "Opt-in set to $enabled")
    }

    suspend fun logSearch(query: String, resultCount: Int) {
        if (!isOptedInNow()) return
        Log.d("Telemetry", "search length=${query.length} results=$resultCount")
    }

    suspend fun logInstall(itemId: String, itemType: String) {
        if (!isOptedInNow()) return
        Log.d("Telemetry", "install type=$itemType idHash=${itemId.hashCode()}")
    }

    suspend fun logChatMessage(isUser: Boolean) {
        if (!isOptedInNow()) return
        Log.d("Telemetry", "chat isUser=$isUser")
    }

    suspend fun isOptedInNow(): Boolean {
        return try {
            val flow = context.telemetryDataStore.data.map { it[OPT_IN_KEY] ?: false }
            flow.first()
        } catch (e: Exception) {
            false
        }
    }
}
