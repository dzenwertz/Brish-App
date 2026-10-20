package com.katchy.focuslive.data.manager

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.katchy.focuslive.service.TimerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/** Platform boundary for monotonic time, account ownership, and foreground execution. */
@Singleton
class TimerRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth
) {
    fun owner(): String = auth.currentUser?.uid?.let { "user:$it" } ?: "guest"
    fun elapsedMs(): Long = SystemClock.elapsedRealtime()
    fun bootCount(): Int? = runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
    }.getOrNull()?.takeIf { it >= 0 }

    val ownerChanges = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(owner()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    fun startForegroundService() {
        try { ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java)) }
        catch (e: Exception) { android.util.Log.e("TimerRuntime", "Unable to start timer foreground service", e) }
    }

    fun stopForegroundService() {
        com.katchy.focuslive.ui.util.SoundManager.stopFocusSound()
        context.stopService(Intent(context, TimerService::class.java))
    }
    fun notifyCompletion(completedWork: Boolean) {
        com.katchy.focuslive.ui.util.NotificationHelper.showPomodoroFinishedNotification(context, completedWork)
    }
}
