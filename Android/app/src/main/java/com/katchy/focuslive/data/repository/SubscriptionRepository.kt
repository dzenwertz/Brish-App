package com.katchy.focuslive.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Account-scoped entitlements; productivity never writes reward counters. */
@Singleton
class SubscriptionRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val prefs = context.getSharedPreferences("subscriptions", Context.MODE_PRIVATE)
    private val legacy = context.getSharedPreferences("user_stats_prefs", Context.MODE_PRIVATE)
    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()
    init {
        auth.addAuthStateListener { firebase ->
            val user = firebase.currentUser
            if (user != null && !prefs.contains(user.uid) && !prefs.getBoolean("legacy_imported", false)) {
                prefs.edit().putBoolean(user.uid, legacy.getBoolean("is_premium", false))
                    .putBoolean("legacy_imported", true).apply()
            }
            _isPremium.value = user != null && (prefs.getBoolean(user.uid, false) || creator())
        }
    }
    private fun creator(): Boolean = auth.currentUser?.email?.lowercase()?.trim() in setOf(
        "josephivan050505@gmail.com", "cachayvargasivan@gmail.com", "dzenwertz05@gmail.com"
    )
    fun setPremium(enabled: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val value = enabled || creator()
        prefs.edit().putBoolean(uid, value).apply()
        _isPremium.value = value
    }
    fun clearUserData() {
        val uid = auth.currentUser?.uid
        _isPremium.value = uid != null && (prefs.getBoolean(uid, false) || creator())
    }
    suspend fun loadFromFirebase(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("No hay una cuenta activa")
        val snapshot = firestore.collection("users").document(uid).get().await()
        if (auth.currentUser?.uid == uid && snapshot.contains("isPremium")) {
            setPremium(snapshot.getBoolean("isPremium") == true)
        }
    }
}
