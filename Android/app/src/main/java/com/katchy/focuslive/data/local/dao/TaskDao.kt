package com.katchy.focuslive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.katchy.focuslive.data.model.Task
import kotlinx.coroutines.flow.Flow

import androidx.room.Transaction

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllTasks(userId: String): Flow<List<Task>>

    @Query(
        "SELECT * FROM tasks WHERE userId = :userId AND (timestamp <= :endOfDayMillis OR dueDate <= :endOfDayMillis) ORDER BY timestamp DESC"
    )
    fun getTasksUpToDate(userId: String, endOfDayMillis: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId AND userId = :userId")
    suspend fun getTaskById(taskId: String, userId: String): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Query("SELECT * FROM tasks WHERE isSynced = 0 AND userId = :userId")
    suspend fun getUnsyncedTasks(userId: String): List<Task>

    @Query("UPDATE tasks SET isSynced = :isSynced WHERE id = :taskId AND userId = :userId")
    suspend fun updateSyncStatus(taskId: String, userId: String, isSynced: Boolean)

    @Query("DELETE FROM tasks WHERE id = :taskId AND userId = :userId")
    suspend fun deleteTask(taskId: String, userId: String)

    @Query("DELETE FROM tasks WHERE userId = :userId")
    suspend fun deleteAllTasksForUser(userId: String)

    @Query("DELETE FROM tasks WHERE userId = :userId AND isSynced = 1 AND id NOT IN (:validIds)")
    suspend fun deleteSyncedTasksNotInSet(userId: String, validIds: List<String>)

    @Query("DELETE FROM tasks WHERE userId = :userId AND isSynced = 1")
    suspend fun deleteAllSyncedTasksForUser(userId: String)

    @Transaction
    suspend fun refreshTasksTransaction(userId: String, tasks: List<Task>, pendingDeletes: Set<String>) {
        val unsyncedIds = getUnsyncedTasks(userId).map { it.id }.toSet()
        val incomingIds = tasks.map { it.id }
        if (incomingIds.isEmpty()) {
            deleteAllSyncedTasksForUser(userId)
        } else {
            deleteSyncedTasksNotInSet(userId, incomingIds)
        }
        tasks.forEach { remoteTask ->
            if (remoteTask.id !in pendingDeletes && remoteTask.id !in unsyncedIds) {
                val localTask = getTaskById(remoteTask.id, userId)
                val resolvedTask = if (localTask != null) {
                    // Defensively preserve local completion status and merge completedDates
                    val isCompleted = localTask.isCompleted || remoteTask.isCompleted
                    val mergedDates = (localTask.completedDates + remoteTask.completedDates).distinct()
                    val mergedSubtasks = remoteTask.subtasks.map { remoteSub ->
                        val localSub = localTask.subtasks.find { it.id == remoteSub.id }
                        if (localSub != null && localSub.isCompleted) {
                            remoteSub.copy(isCompleted = true)
                        } else {
                            remoteSub
                        }
                    }
                    remoteTask.copy(
                        userId = userId,
                        isSynced = true,
                        isCompleted = isCompleted,
                        completedDates = mergedDates,
                        subtasks = mergedSubtasks
                    )
                } else {
                    remoteTask.copy(userId = userId, isSynced = true)
                }
                insertTask(resolvedTask)
            }
        }
    }
}
