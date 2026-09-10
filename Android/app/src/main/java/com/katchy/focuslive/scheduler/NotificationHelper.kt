package com.katchy.focuslive.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.katchy.focuslive.MainActivity
import com.katchy.focuslive.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "focus_live_reminders"
        const val STREAK_CHANNEL_ID = "focus_live_streaks"
        const val CHANNEL_NAME = "Recordatorios Generales"
        const val STREAK_CHANNEL_NAME = "Rachas de Hábitos"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            // General Channel
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Recordatorios de planificación y tareas"
            }
            
            // Streak Channel
            val streakChannel = NotificationChannel(STREAK_CHANNEL_ID, STREAK_CHANNEL_NAME, importance).apply {
                description = "Recordatorios para mantener tus rachas"
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(streakChannel)
        }
    }

    fun showNotification(
        id: Int,
        title: String,
        content: String,
        destination: String? = null,
        channelId: String = CHANNEL_ID
    ) {
        // Guard: On Android 13+ (TIRAMISU), calling notify() without POST_NOTIFICATIONS
        // permission throws SecurityException. Skip silently to avoid Crashlytics noise.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (destination != null) {
                putExtra("navigation_destination", destination)
            }
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use app icon or specific notification icon
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
        notificationManager.notify(id, builder.build())
    }
}
