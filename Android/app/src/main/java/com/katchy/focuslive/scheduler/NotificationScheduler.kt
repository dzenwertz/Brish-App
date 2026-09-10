package com.katchy.focuslive.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.data.repository.HabitRepository
import com.katchy.focuslive.data.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun rescheduleAllAlarms() {
        scope.launch {
            Log.d("NotificationScheduler", "Rescheduling all alarms...")
            
            // 1. Basic Daily Notifications
            scheduleDailyNotifications()
            
            // 2. Reschedule Tasks (use getAllTasks so future-dated tasks still get reminders)
            try {
                val tasks = taskRepository.getAllTasks().first()
                tasks.forEach { task ->
                    if (!task.isCompleted) {
                         scheduleTaskReminder(task)
                    }
                }
                Log.d("NotificationScheduler", "Rescheduled ${tasks.size} tasks")
            } catch (e: Exception) {
                Log.e("NotificationScheduler", "Failed to reschedule tasks", e)
            }

            // 3. Reschedule Habits
            try {
                val habits = habitRepository.getHabits().first()
                habits.forEach { habit ->
                    if (habit.reminderTime != null) {
                        scheduleHabitReminder(habit)
                    }
                }
                Log.d("NotificationScheduler", "Rescheduled ${habits.size} habits")
            } catch (e: Exception) {
                Log.e("NotificationScheduler", "Failed to reschedule habits", e)
            }
        }
    }

    fun scheduleDailyNotifications() {
        schedulePlannerNotification()
        scheduleStreakNotification()
    }

    private fun schedulePlannerNotification() {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("type", "daily_planner")
        }
        
        // 9:00 AM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(calendar.timeInMillis, pendingIntent)
    }

    private fun scheduleStreakNotification() {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("type", "habit_check")
        }
        
        // 20:00 (8 PM)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(calendar.timeInMillis, pendingIntent)
    }

    fun scheduleHabitReminder(habit: Habit) {
        val reminderTime = habit.reminderTime ?: return
        if (habit.daysOfWeek.isEmpty()) return // No days selected

        val parts = reminderTime.split(":")
        if (parts.size != 2) return

        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("type", "specific_habit")
            putExtra("habitId", habit.id)
            putExtra("habitTitle", habit.title)
            putExtra("habitIcon", habit.icon)
        }

        val now = Calendar.getInstance()
        var targetCalendar: Calendar? = null

        // Find the next valid alarm time
        for (i in 0..7) {
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Check if this candidate day is in the selected days
            // Calendar.SUNDAY = 1, Saturday = 7. Matches our data model.
            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            
            if (habit.daysOfWeek.contains(dayOfWeek)) {
                if (candidate.timeInMillis > now.timeInMillis) {
                    targetCalendar = candidate
                    break
                }
            }
        }

        if (targetCalendar != null) {
            // Use habit.id.hashCode() for unique RequestCode
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                habit.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleAlarm(targetCalendar.timeInMillis, pendingIntent)
            Log.d("NotificationScheduler", "Scheduled habit '${habit.title}' for ${targetCalendar.time}")
        }
    }

    fun cancelHabitReminder(habit: Habit) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleTaskReminder(task: com.katchy.focuslive.data.model.Task) {
        if (!task.hasDueDate) return
        val time = task.time ?: return
        val parts = time.split(":")
        if (parts.size != 2) return
        
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("type", "task_reminder")
            putExtra("taskId", task.id)
            putExtra("taskTitle", task.title)
            putExtra("taskCategory", task.category)
        }

        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = task.timestamp
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If recurring and calendar is in the past, advance to next occurrence
        if (calendar.before(now)) {
            when (task.repeatMode) {
                "DAILY" -> {
                    while (calendar.before(now)) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                "WEEKLY" -> {
                    while (calendar.before(now)) {
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }
                "CUSTOM" -> {
                    if (task.repeatDays.isNotEmpty()) {
                        for (i in 0..14) {
                            val candidate = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, i)
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            if (task.repeatDays.contains(candidate.get(Calendar.DAY_OF_WEEK)) && candidate.after(now)) {
                                calendar.timeInMillis = candidate.timeInMillis
                                break
                            }
                        }
                    }
                }
                else -> {
                    // Non-recurring task whose time is in the past: do not schedule
                    return
                }
            }
        }

        // Final sanity check: must be strictly in the future
        if (calendar.timeInMillis <= now.timeInMillis) return

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(calendar.timeInMillis, pendingIntent)
    }

    fun cancelTaskReminder(taskId: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        // We only need the correct RequestCode (hashCode of ID) + Intent Class to match
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun scheduleAlarm(timeInMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w("NotificationScheduler", "Exact alarm permission revoked, falling back", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } catch (fallbackEx: Exception) {
                Log.e("NotificationScheduler", "Failed to schedule alarm on fallback", fallbackEx)
            }
        } catch (e: Exception) {
            Log.e("NotificationScheduler", "Failed to schedule alarm", e)
        }
    }
}
