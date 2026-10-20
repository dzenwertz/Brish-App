package com.katchy.focuslive.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.katchy.focuslive.data.local.BrishDatabase
import com.katchy.focuslive.data.local.dao.TaskDao
import com.katchy.focuslive.data.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val database: BrishDatabase,
    private val auth: FirebaseAuth,
    private val notificationHelper: NotificationHelper
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val rescheduleMutex = Mutex()

    init {
        // Also cleans retired alarms for signed-out users and on every account transition.
        auth.addAuthStateListener { rescheduleAllAlarms() }
    }

    fun rescheduleAllAlarms() {
        scope.launch { rescheduleAllAlarmsNow() }
    }

    /** Receivers await this work before finishing their goAsync lifetime. */
    suspend fun rescheduleAllAlarmsNow() = withContext(Dispatchers.IO) {
        rescheduleMutex.withLock {
            try {
                cancelRetiredAndStoredAlarms()
                val owner = auth.currentUser?.uid ?: return@withLock
                val tasks = taskDao.getAllTasks(owner).first()
                if (auth.currentUser?.uid != owner) return@withLock
                tasks.forEach { task ->
                    if (auth.currentUser?.uid != owner) return@withLock
                    scheduleTaskReminder(task)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore task reminders", e)
            }
        }
    }

    private fun cancelRetiredAndStoredAlarms() {
        cancelLegacyAlarm(1001)
        cancelLegacyAlarm(1002)
        notificationHelper.cancelNotification(1001)
        notificationHelper.cancelNotification(1002)
        // Read every retained account, including signed-out owners. Never recreate habit alarms.
        database.openHelper.readableDatabase.query("SELECT id FROM habits").use { cursor ->
            while (cursor.moveToNext()) {
                val requestCode = cursor.getString(0).hashCode()
                cancelLegacyAlarm(requestCode)
                notificationHelper.cancelNotification(requestCode)
            }
        }
        // Old task intents lacked an owner. Cancel those and all stored account alarms before restoring.
        database.openHelper.readableDatabase.query("SELECT id, userId FROM tasks").use { cursor ->
            while (cursor.moveToNext()) {
                val taskId = cursor.getString(0)
                val owner = cursor.getString(1)
                cancelLegacyAlarm(taskId.hashCode())
                cancelAlarm(taskId.hashCode(), taskIntent(taskId, owner))
                if (owner != auth.currentUser?.uid) notificationHelper.cancelNotification(taskId.hashCode())
            }
        }
    }

    fun scheduleTaskReminder(task: Task) {
        val owner = auth.currentUser?.uid ?: return
        // Unsaved UI drafts and tasks from another account cannot replace a valid alarm.
        if (task.userId.isBlank() || task.userId != owner) return
        cancelTaskAlarm(task.id)
        val triggerAt = nextTaskReminderAt(task, System.currentTimeMillis()) ?: return
        if (auth.currentUser?.uid != owner) return
        val intent = taskIntent(task.id, owner).apply {
            putExtra(EXTRA_TYPE, TYPE_TASK)
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_OWNER_ID, owner)
            putExtra(EXTRA_TRIGGER_AT, triggerAt)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, task.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleAlarm(triggerAt, pendingIntent)
    }

    fun cancelTaskReminder(taskId: String) {
        cancelTaskAlarm(taskId)
        notificationHelper.cancelNotification(taskId.hashCode())
    }

    private fun cancelTaskAlarm(taskId: String) {
        cancelLegacyAlarm(taskId.hashCode())
        auth.currentUser?.uid?.let { owner ->
            cancelAlarm(taskId.hashCode(), taskIntent(taskId, owner))
        }
    }

    private fun taskIntent(taskId: String, owner: String) =
        Intent(context, ReminderReceiver::class.java).apply {
            // Extras are not part of PendingIntent identity; owner and task ID must be.
            data = Uri.Builder().scheme("brish").authority("task-reminder")
                .appendPath(owner).appendPath(taskId).build()
        }

    private fun cancelLegacyAlarm(requestCode: Int) =
        cancelAlarm(requestCode, Intent(context, ReminderReceiver::class.java))

    private fun cancelAlarm(requestCode: Int, intent: Intent) {
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun scheduleAlarm(timeInMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            } catch (fallback: Exception) {
                Log.e(TAG, "Failed to schedule task reminder", fallback)
            }
        }
    }

    companion object {
        private const val TAG = "NotificationScheduler"
        internal const val EXTRA_TYPE = "type"
        internal const val TYPE_TASK = "task_reminder"
        internal const val EXTRA_TASK_ID = "taskId"
        internal const val EXTRA_OWNER_ID = "ownerId"
        internal const val EXTRA_TRIGGER_AT = "triggerAt"
    }
}
