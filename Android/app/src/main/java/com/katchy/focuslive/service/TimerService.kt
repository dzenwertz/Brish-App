package com.katchy.focuslive.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.katchy.focuslive.R
import com.katchy.focuslive.data.manager.TimerManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : Service() {

    @Inject
    lateinit var timerManager: TimerManager

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val NOTIFICATION_ID = 3001
    private val CHANNEL_ID = "focus_timer_channel"
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        
        // Acquire partial WakeLock to keep CPU awake when screen is off
        try {
            val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Brish:FocusTimerWakeLock").apply {
                val remainingMs = (timerManager.timeLeft.value * 1000L) + 60_000L
                acquire(remainingMs.coerceIn(60_000L, 3 * 60 * 60 * 1000L))
            }
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Failed to acquire WakeLock", e)
        }

        createNotificationChannel()
        startForegroundService()
        observeTimer()
    }

    private fun startForegroundService() {
        val notification = createNotification("Enfoque activo", "25:00 restantes")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, 
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) 
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE 
                    else 0
                ) 
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Exception caught", e)
            // Gracefully stop the service if foreground permission/start is denied by system restrictions
            stopSelf()
        }
    }

    private fun observeTimer() {
        serviceScope.launch {
            combine(
                timerManager.timeLeft,
                timerManager.timerState,
                timerManager.isTimerRunning
            ) { timeLeft, state, isRunning ->
                Triple(timeLeft, state, isRunning)
            }.collectLatest { (timeLeft, state, isRunning) ->
                val minutes = String.format(java.util.Locale.US, "%02d", timeLeft / 60)
                val seconds = String.format(java.util.Locale.US, "%02d", timeLeft % 60)
                val title = if (state == TimerManager.TimerState.WORK) "Enfoque activo" else "Descanso activo"
                
                // Update Widget
                try {
                    val context = applicationContext
                    val glanceId = androidx.glance.appwidget.GlanceAppWidgetManager(context).getGlanceIds(com.katchy.focuslive.widget.PomodoroWidget::class.java).firstOrNull()
                    if (glanceId != null) {
                        androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { prefs ->
                            prefs[androidx.datastore.preferences.core.stringPreferencesKey("time_left")] = "$minutes:$seconds"
                            prefs[androidx.datastore.preferences.core.booleanPreferencesKey("is_running")] = isRunning
                            prefs[androidx.datastore.preferences.core.stringPreferencesKey("status")] = title
                        }
                        com.katchy.focuslive.widget.PomodoroWidget().update(context, glanceId)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TimerService", "Exception caught", e)
                }

                if (!isRunning) {
                     stopSelf()
                     return@collectLatest
                }
                
                updateNotification(title, "$minutes:$seconds restantes")
            }
        }
    }





    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification(title, content))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Temporizador de Enfoque",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Failed to release WakeLock", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_TOGGLE_TIMER -> timerManager.toggleTimer()
                ACTION_RESET_TIMER -> timerManager.resetTimer()
            }
        }
        if (!timerManager.isTimerRunning.value && intent?.action == null) {
            stopSelf()
        }
        return START_STICKY
    }

    companion object {
        const val ACTION_TOGGLE_TIMER = "com.katchy.focuslive.service.ACTION_TOGGLE_TIMER"
        const val ACTION_RESET_TIMER = "com.katchy.focuslive.service.ACTION_RESET_TIMER"
    }
}
