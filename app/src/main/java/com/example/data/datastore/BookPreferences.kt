package com.example.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bookDataStore by preferencesDataStore(name = "book_prefs")

class BookPreferences(private val context: Context) {
    private val FONT_SCALE = floatPreferencesKey("font_scale")
    private val NIGHT_MODE = booleanPreferencesKey("night_mode")

    val fontScale: Flow<Float> = context.bookDataStore.data.map { it[FONT_SCALE] ?: 1f }
    val isNightMode: Flow<Boolean> = context.bookDataStore.data.map { it[NIGHT_MODE] ?: false }

    suspend fun setFontScale(scale: Float) {
        context.bookDataStore.edit { it[FONT_SCALE] = scale.coerceIn(0.8f, 1.4f) }
    }

    suspend fun setNightMode(enabled: Boolean) {
        context.bookDataStore.edit { it[NIGHT_MODE] = enabled }
    }
}
