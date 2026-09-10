package com.katchy.focuslive.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.scheduler.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var habitRepository: com.katchy.focuslive.data.repository.HabitRepository

    @Inject
    lateinit var taskRepository: com.katchy.focuslive.data.repository.TaskRepository

    @Inject
    lateinit var userStatsRepository: com.katchy.focuslive.data.repository.UserStatsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type") ?: return

        when (type) {
            "daily_planner" -> {
                notificationHelper.showNotification(
                    1001,
                    "¡Hora de Planificar!",
                    "Organiza tu día para ser más productivo. 📝",
                    "planner"
                )
                notificationScheduler.scheduleDailyNotifications()
            }
            "habit_check" -> {
                val stats = userStatsRepository.userStats.value
                val streak = stats.streak
                val lastLogin = stats.lastLoginDate
                
                val todayMidnight = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val message = if (lastLogin < todayMidnight && streak > 0) {
                    "¡No dejes apagar tu flama de $streak ${if (streak == 1) "día" else "días"}! 🔥"
                } else {
                    "No olvides marcar tus hábitos de hoy. 🔥"
                }

                notificationHelper.showNotification(
                    1002,
                    "¡Mantén tu racha!",
                    message,
                    "habits",
                    NotificationHelper.STREAK_CHANNEL_ID
                )
                notificationScheduler.scheduleDailyNotifications()
            }
            "specific_habit" -> {
                val title = intent.getStringExtra("habitTitle") ?: "Hábito"
                val icon = intent.getStringExtra("habitIcon") ?: "🌱"
                val habitId = intent.getStringExtra("habitId")
                
                notificationHelper.showNotification(
                    (habitId?.hashCode() ?: 0),
                    "$icon $title",
                    "¡Es hora de tu hábito!",
                    "habits"
                )
                
                // Reschedule for next day if habit still exists
                if (habitId != null) {
                    val pendingResult = goAsync()
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            val habits = habitRepository.getHabits().firstOrNull() // Get current list snapshot
                            val habit = habits?.find { it.id == habitId }
                            if (habit != null && habit.reminderTime != null) {
                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    notificationScheduler.scheduleHabitReminder(habit)
                                }
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            "task_reminder" -> {
                val taskId = intent.getStringExtra("taskId")
                val title = intent.getStringExtra("taskTitle") ?: "Tarea pendiente"
                // Funny Messages Logic
                val messages = listOf(
                    "¡Muévete! Es hora de $title.",
                    "Deja de procrastinar. $title te espera.",
                    "¡Alerta! Tienes una misión: $title.",
                    "Menos chisme, más acción. Toca $title.",
                    "¿Sigues ahí? ¡Levántate! Es hora de $title.",
                    "Tu yo del futuro te agradecerá si haces $title ahora.",
                    "¡Bip Bop! Hora del show: $title.",
                    "No lo pienses, solo hazlo: $title."
                )
                val body = messages.random()

                notificationHelper.showNotification(
                    (taskId?.hashCode() ?: 0),
                    "¡Es la hora!",
                    body,
                    "planner" // Using planner channel
                )

                // Reschedule for next occurrence if recurring task
                if (taskId != null) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val tasks = taskRepository.getAllTasks().firstOrNull()
                            val task = tasks?.find { it.id == taskId }
                            if (task != null && task.time != null && task.repeatMode != "NONE") {
                                withContext(Dispatchers.Main) {
                                    notificationScheduler.scheduleTaskReminder(task)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ReminderReceiver", "Failed to reschedule recurring task $taskId", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }
}
