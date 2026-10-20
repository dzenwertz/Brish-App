package com.katchy.focuslive.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.katchy.focuslive.data.local.dao.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var notificationScheduler: NotificationScheduler
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var auth: FirebaseAuth

    override fun onReceive(context: Context, intent: Intent) {
        // Ignore retired daily/habit and ownerless task alarms, even if already in flight.
        if (intent.getStringExtra(NotificationScheduler.EXTRA_TYPE) != NotificationScheduler.TYPE_TASK) return
        val taskId = intent.getStringExtra(NotificationScheduler.EXTRA_TASK_ID)?.takeIf { it.isNotBlank() } ?: return
        val owner = intent.getStringExtra(NotificationScheduler.EXTRA_OWNER_ID)?.takeIf { it.isNotBlank() } ?: return
        val triggerAt = intent.getLongExtra(NotificationScheduler.EXTRA_TRIGGER_AT, 0L)
        if (triggerAt <= 0L || auth.currentUser?.uid != owner) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = taskDao.getTaskById(taskId, owner) ?: return@launch
                if (task.userId != owner || auth.currentUser?.uid != owner) return@launch
                // A task edit or completion can invalidate an intent already queued for delivery.
                val stillScheduled = nextTaskReminderAt(task, triggerAt - 1) == triggerAt
                withContext(Dispatchers.Main.immediate) {
                    if (auth.currentUser?.uid != owner) return@withContext
                    if (stillScheduled && System.currentTimeMillis() >= triggerAt) {
                        notificationHelper.showNotification(
                            task.id.hashCode(), task.title,
                            if (isDeliveryReminder(task)) "Tu entrega vence ahora." else "Es hora de tu tarea.",
                            taskReminderDestination(task)
                        )
                    }
                    notificationScheduler.scheduleTaskReminder(task)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Failed to deliver task reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
