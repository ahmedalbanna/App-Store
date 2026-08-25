package com.example.data.api

import android.util.Log
import com.example.BuildConfig

/**
 * Phase 3.1: Centralized Gemini API key handling.
 * Single source of truth for key retrieval, validation, and Firebase AI vs REST decision.
 */
object GeminiConfig {
    val apiKey: String get() = BuildConfig.GEMINI_API_KEY

    val isConfigured: Boolean
        get() = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "GEMINI_API_KEY"

    val keyStatus: String
        get() = when {
            apiKey.isEmpty() -> "EMPTY"
            apiKey == "MY_GEMINI_API_KEY" -> "PLACEHOLDER"
            apiKey == "GEMINI_API_KEY" -> "PLACEHOLDER"
            apiKey.length < 20 -> "SUSPICIOUS"
            else -> "CONFIGURED"
        }

    fun logStatus() {
        Log.d("GeminiConfig", "Gemini key status: $keyStatus (length=${apiKey.length})")
    }

    // For Firebase AI (if google-services.json absent, falls back to REST)
    fun shouldUseFirebaseAi(): Boolean {
        // Firebase AI requires FirebaseApp initialization; if not available, use REST
        return try {
            com.google.firebase.FirebaseApp.getInstance() != null && isConfigured
        } catch (e: Exception) {
            false
        }
    }
}
