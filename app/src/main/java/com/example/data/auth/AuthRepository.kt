package com.example.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Phase 4.2: Auth & Library sync — Firebase Auth (anonymous → Google).
 * Handles anonymous sign-in, userId flow, and library sync placeholder.
 */
class AuthRepository {

    private val auth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        Log.w("AuthRepository", "FirebaseAuth not available (google-services.json missing): ${e.message}")
        null
    }

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _isAnonymous = MutableStateFlow(true)
    val isAnonymous: StateFlow<Boolean> = _isAnonymous.asStateFlow()

    init {
        // Try to restore current user
        auth?.currentUser?.let { user ->
            _userId.value = user.uid
            _isAnonymous.value = user.isAnonymous
            Log.d("AuthRepository", "Restored user ${user.uid} anonymous=${user.isAnonymous}")
        } ?: run {
            // Auto sign-in anonymously if no user (best-effort, no crash if Firebase not configured)
            // Caller should invoke signInAnonymously() explicitly
        }
    }

    suspend fun signInAnonymously(): Result<String> {
        val firebaseAuth = auth ?: return Result.failure(Exception("FirebaseAuth not configured"))
        return try {
            val result = firebaseAuth.signInAnonymously().await()
            val user = result.user
            if (user != null) {
                _userId.value = user.uid
                _isAnonymous.value = true
                Log.d("AuthRepository", "Anonymous sign-in success: ${user.uid}")
                Result.success(user.uid)
            } else {
                Result.failure(Exception("Anonymous sign-in returned null user"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Anonymous sign-in failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            auth?.signOut()
            _userId.value = null
            _isAnonymous.value = true
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign out failed: ${e.message}")
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth?.currentUser

    // Phase 4.2: Library sync placeholder — in real app, sync bookmarks/downloads to Firestore
    suspend fun syncLibraryToCloud(bookmarkedIds: List<String>, downloadedIds: List<String>): Result<Unit> {
        val uid = _userId.value ?: return Result.failure(Exception("Not signed in"))
        return try {
            // Placeholder: In real app, write to Firestore:
            // Firebase.firestore.collection("users").document(uid).set(mapOf("bookmarks" to bookmarkedIds, "downloads" to downloadedIds))
            Log.d("AuthRepository", "Sync library for $uid: ${bookmarkedIds.size} bookmarks, ${downloadedIds.size} downloads")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
