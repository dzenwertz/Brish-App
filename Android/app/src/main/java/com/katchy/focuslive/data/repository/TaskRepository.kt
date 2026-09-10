package com.katchy.focuslive.data.repository

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.katchy.focuslive.data.local.dao.PendingDeleteDao
import com.katchy.focuslive.data.local.dao.TaskDao
import com.katchy.focuslive.data.model.PendingDelete
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.data.worker.SyncWorker
import com.katchy.focuslive.widget.TasksWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

interface TaskRepository {
    fun getTasksForDate(dateMillis: Long): Flow<List<Task>>
    fun getAllTasks(): Flow<List<Task>>
    suspend fun addTask(task: Task): Result<Unit>
    suspend fun updateTask(task: Task): Result<Unit>
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun toggleTaskCompletion(taskId: String, dateMillis: Long = System.currentTimeMillis()): Result<Boolean>
    suspend fun toggleSubtask(taskId: String, subtaskId: String, dateMillis: Long): Result<Unit>
    suspend fun syncPendingChanges(): Result<Unit>
    suspend fun syncPendingChanges(userId: String): Result<Unit>
    suspend fun refreshTasks(): Result<Unit>
    suspend fun refreshTasks(userId: String): Result<Unit>
    suspend fun clearUserData(userId: String): Result<Unit>
}

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val taskDao: TaskDao,
    private val pendingDeleteDao: PendingDeleteDao,
    @ApplicationContext private val context: Context,
    private val notificationSchedulerProvider: Provider<com.katchy.focuslive.scheduler.NotificationScheduler>,
    private val userStatsRepository: com.katchy.focuslive.data.repository.UserStatsRepository,
    private val mascotRepository: com.katchy.focuslive.data.repository.MascotRepository,
    private val timerManager: com.katchy.focuslive.data.manager.TimerManager
) : TaskRepository {

    companion object {
        private const val TAG = "TaskRepository"
        private const val PENDING_DELETE_TYPE = "task"
    }

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private fun collectionFor(userId: String) =
        firestore.collection("users").document(userId).collection("tasks")

    private fun currentUserIdFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private val toggleMutex = Mutex()

    // We narrow the DB query to tasks with timestamp <= endOfDay(date).
    // This drops future tasks (not relevant to the selected date) while still
    // including recurring tasks created on/before the selected date. The viewModel
    // still filters in-memory by recurrence to decide which of these candidates
    // should actually be shown on `dateMillis` (see PlannerViewModel.tasksForSelectedDate).
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTasksForDate(dateMillis: Long): Flow<List<Task>> {
        val endOfDay = java.util.Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }.timeInMillis
        return currentUserIdFlow().flatMapLatest { userId ->
            if (userId != null) {
                taskDao.getTasksUpToDate(userId, endOfDay)
            } else {
                flowOf(emptyList())
            }
        }
    }

    // Returns every task regardless of date. Used by the notification scheduler,
    // which needs future tasks too in order to set reminders for upcoming days.
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllTasks(): Flow<List<Task>> {
        return currentUserIdFlow().flatMapLatest { userId ->
            if (userId != null) {
                taskDao.getAllTasks(userId)
            } else {
                flowOf(emptyList())
            }
        }
    }

    override suspend fun addTask(task: Task): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return try {
            // 0. Remove any pending tombstone so syncPendingChanges doesn't delete it
            pendingDeleteDao.delete(task.id, PENDING_DELETE_TYPE, userId)

            // 1. Save un-synced to Local DB with userId
            taskDao.insertTask(task.copy(userId = userId, isSynced = false))

            // 2. Schedule Sync
            scheduleSync()
            
            // 3. Update Widget
            try { TasksWidget().updateAll(context) } catch (e: Exception) { Log.e(TAG, "Widget update failed", e) }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTask(task: Task): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return try {
            // 0. Remove any pending tombstone so syncPendingChanges doesn't delete it
            pendingDeleteDao.delete(task.id, PENDING_DELETE_TYPE, userId)

            // 1. Save un-synced to Local DB with userId
            taskDao.updateTask(task.copy(userId = userId, isSynced = false))

            // 2. Schedule Sync
            scheduleSync()
            
            // 3. Update Widget
            try { TasksWidget().updateAll(context) } catch (e: Exception) { Log.e(TAG, "Widget update failed", e) }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return try {
            taskDao.deleteTask(taskId, userId)

            // Clear active task if it was this task
            if (timerManager.activeTaskId.value == taskId) {
                timerManager.setActiveTask(null)
            }

            // Guardar tombstone para que syncPendingChanges() borre el documento remoto
            // y refreshTasks() no lo resucite si el borrado inmediato falla.
            pendingDeleteDao.insert(
                PendingDelete(itemId = taskId, type = PENDING_DELETE_TYPE, userId = userId)
            )

            // Fire-and-forget immediate attempt
             try {
                collectionFor(userId).document(taskId).delete()
            } catch (e: Exception) {
                 Log.e(TAG, "Failed to delete remote task $taskId", e)
            }

            scheduleSync()

            // Update Widget
            try { TasksWidget().updateAll(context) } catch (e: Exception) { Log.e(TAG, "Widget update failed", e) }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleTaskCompletion(taskId: String, dateMillis: Long): Result<Boolean> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return toggleMutex.withLock {
            try {
                val task = taskDao.getTaskById(taskId, userId) ?: return@withLock Result.failure(Exception("Task not found"))
                
                val updatedTask: Task
                val isCompleting: Boolean
                
                if (task.repeatMode != "NONE") {
                    // Logic for Recurring Tasks: Toggle date in completedDates list
                    val targetDateStart = getStartOfDay(dateMillis)
                    val isAlreadyCompleted = task.completedDates.any { 
                        com.katchy.focuslive.util.HabitUtils.isSameDay(it, targetDateStart)
                    }
                    
                    val newCompletedDates = if (isAlreadyCompleted) {
                        task.completedDates.filter { 
                            !com.katchy.focuslive.util.HabitUtils.isSameDay(it, targetDateStart)
                        }
                    } else {
                        task.completedDates + targetDateStart
                    }
                    
                    updatedTask = task.copy(completedDates = newCompletedDates, isCompleted = false, isSynced = false)
                    isCompleting = !isAlreadyCompleted
                } else {
                    // Logic for One-off Tasks: also maintain completedDates with the completion date
                    val targetDateStart = getStartOfDay(dateMillis)
                    val willBeCompleted = !task.isCompleted
                    val newCompletedDates = if (willBeCompleted) {
                        task.completedDates + targetDateStart
                    } else {
                        task.completedDates.filter { !com.katchy.focuslive.util.HabitUtils.isSameDay(it, targetDateStart) }
                    }
                    updatedTask = task.copy(isCompleted = willBeCompleted, completedDates = newCompletedDates, isSynced = false)
                    isCompleting = willBeCompleted
                }
                
                // Save un-synced to Local DB
                taskDao.updateTask(updatedTask)
                scheduleSync()
                
                // Update Widget
                try { TasksWidget().updateAll(context) } catch (e: Exception) { Log.e(TAG, "Widget update failed", e) }
                
                // Cancel/Reschedule based on completion (only for one-off tasks)
                if (task.repeatMode == "NONE") {
                    if (isCompleting) {
                        notificationSchedulerProvider.get().cancelTaskReminder(updatedTask.id)
                    } else if (updatedTask.time != null) {
                        notificationSchedulerProvider.get().scheduleTaskReminder(updatedTask)
                    }
                }
                
                // Award/Deduct rewards (XP, Coins, Mascot XP, Completed Tasks count)
                val multiplier = userStatsRepository.getStreakMultiplier()
                val xpAwarded = (10 * multiplier).toInt()
                val coinsAwarded = (5 * multiplier).toInt()
                
                if (isCompleting) {
                    userStatsRepository.addXp(xpAwarded)
                    mascotRepository.addXpToSelectedMascot(xpAwarded)
                    userStatsRepository.incrementTasks()
                    userStatsRepository.addCoins(coinsAwarded)
                    
                    // Early Bird Check (4 AM - 7 AM)
                    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                    if (hour in 4..7) {
                        userStatsRepository.unlockBadge(com.katchy.focuslive.util.GamificationLogic.BADGE_EARLY_BIRD)
                    }
                    
                    // Clear active task in timer if it is this task
                    if (timerManager.activeTaskId.value == task.id) {
                        timerManager.setActiveTask(null)
                    }
                } else {
                    userStatsRepository.deductXp(xpAwarded)
                    mascotRepository.deductXpFromSelectedMascot(xpAwarded)
                    userStatsRepository.decrementTasks()
                    userStatsRepository.deductCoins(coinsAwarded)
                }
                
                Result.success(isCompleting)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun toggleSubtask(taskId: String, subtaskId: String, dateMillis: Long): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return toggleMutex.withLock {
            try {
                val task = taskDao.getTaskById(taskId, userId) ?: return@withLock Result.failure(Exception("Task not found"))
                val targetDateStart = getStartOfDay(dateMillis)
                val updatedSubtasks = task.subtasks.map { subtask ->
                    if (subtask.id == subtaskId) {
                        if (task.repeatMode != "NONE") {
                            val isAlreadyCompleted = subtask.completedDates.any {
                                com.katchy.focuslive.util.HabitUtils.isSameDay(it, targetDateStart)
                            }
                            val newDates = if (isAlreadyCompleted) {
                                subtask.completedDates.filter { !com.katchy.focuslive.util.HabitUtils.isSameDay(it, targetDateStart) }
                            } else {
                                subtask.completedDates + targetDateStart
                            }
                            subtask.copy(completedDates = newDates, isCompleted = false)
                        } else {
                            subtask.copy(isCompleted = !subtask.isCompleted)
                        }
                    } else {
                        subtask
                    }
                }
                val updatedTask = task.copy(subtasks = updatedSubtasks, isSynced = false)
                taskDao.updateTask(updatedTask)
                scheduleSync()
                try { TasksWidget().updateAll(context) } catch (e: Exception) { Log.e(TAG, "Widget update failed", e) }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    override suspend fun syncPendingChanges(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return syncPendingChanges(userId)
    }

    override suspend fun syncPendingChanges(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val collection = collectionFor(userId)

            // 1. Upload pending modifications
            val unsyncedTasks = taskDao.getUnsyncedTasks(userId)
            if (unsyncedTasks.isNotEmpty()) {
                unsyncedTasks.forEach { task ->
                    // Upload with userId
                    collection.document(task.id).set(task.copy(isSynced = true)).await()
                    // Update Local
                    taskDao.updateSyncStatus(task.id, userId, true)
                }
                Log.d(TAG, "Synced ${unsyncedTasks.size} tasks")
            }

            // 2. Upload pending deletes
            val pendingDeletes = pendingDeleteDao.getPendingDeletes(userId, PENDING_DELETE_TYPE)
            if (pendingDeletes.isNotEmpty()) {
                pendingDeletes.forEach { tombstone ->
                    try {
                        collection.document(tombstone.itemId).delete().await()
                        pendingDeleteDao.delete(tombstone.itemId, PENDING_DELETE_TYPE, userId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to delete remote task ${tombstone.itemId}", e)
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.failure(e)
        }
    }

    override suspend fun refreshTasks(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return refreshTasks(userId)
    }

    override suspend fun refreshTasks(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val collection = collectionFor(userId)
            val snapshot = collection.get().await()
            val tasks = snapshot.toObjects(Task::class.java)

            val pendingDeletes = pendingDeleteDao.getPendingDeletes(userId, PENDING_DELETE_TYPE).map { it.itemId }.toSet()
            taskDao.refreshTasksTransaction(userId, tasks, pendingDeletes)
            Log.d(TAG, "Refreshed ${tasks.size} tasks from cloud")

            // Update Widget
            try { TasksWidget().updateAll(context) } catch (e: Exception) { Log.e(TAG, "Widget update failed", e) }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Refresh failed", e)
            Result.failure(e)
        }
    }

    override suspend fun clearUserData(userId: String): Result<Unit> {
        return try {
            taskDao.deleteAllTasksForUser(userId)
            pendingDeleteDao.deleteAllForUserAndType(userId, PENDING_DELETE_TYPE)
            Log.d(TAG, "Cleared tasks for user $userId")

            // Update Widget
            try { TasksWidget().updateAll(context) } catch (e: Exception) { Log.e(TAG, "Widget update failed", e) }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear tasks for user $userId", e)
            Result.failure(e)
        }
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncWork",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncRequest
        )
    }
}
