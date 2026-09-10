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

    fun showPomodoroFinishedNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timerName = "Protocolo de Enfoque"
                val timerDesc = "Alertas cuando terminas un bloque de Deep Work"
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
                .setContentTitle("¡Deep Work Terminado!")
                .setContentText("Alcanzaste tu meta. ¡Tómate un descanso!")
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
