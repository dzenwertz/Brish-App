package com.katchy.focuslive.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.katchy.focuslive.R

object NotificationHelper {
    const val CHANNEL_ID_TIMER = "timer_notifications"
    const val NOTIFICATION_ID_POMODORO = 3002

    fun showPomodoroFinishedNotification(context: Context, completedWork: Boolean = true) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timerName = "Sesiones de enfoque"
                val timerDesc = "Avisos al finalizar el enfoque o el descanso"
                val timerChannel = NotificationChannel(
                    CHANNEL_ID_TIMER,
                    timerName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = timerDesc
                }
                notificationManager.createNotificationChannel(timerChannel)
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID_TIMER)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(if (completedWork) "Enfoque terminado" else "Descanso terminado")
                .setContentText(if (completedWork) "Es momento de descansar." else "Puedes comenzar otra sesión de enfoque.")
                .setContentIntent(android.app.PendingIntent.getActivity(context, NOTIFICATION_ID_POMODORO,
                    android.content.Intent(context, com.katchy.focuslive.MainActivity::class.java).apply {
                        putExtra("destination", "focus")
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_POMODORO, builder.build())
                }
            } else {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_POMODORO, builder.build())
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Exception caught while showing pomodoro notification", e)
        }
    }
}
