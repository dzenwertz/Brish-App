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

    @Query("SELECT * FROM tasks WHERE userId = :userId AND kind = 'AGENDA' AND (courseId IS NULL OR TRIM(courseId) = '') AND (deliverableType IS NULL OR TRIM(deliverableType) = '') ORDER BY timestamp DESC")
    fun getAgendaTasks(userId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND (kind = 'DELIVERY' OR (courseId IS NOT NULL AND TRIM(courseId) != '') OR (deliverableType IS NOT NULL AND TRIM(deliverableType) != '')) ORDER BY timestamp DESC")
    fun getDeliveries(userId: String): Flow<List<Task>>

    @Query(
        "SELECT * FROM tasks WHERE userId = :userId AND (timestamp <= :endOfDayMillis OR dueDate <= :endOfDayMillis) ORDER BY timestamp DESC"
    )
    fun getTasksUpToDate(userId: String, endOfDayMillis: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId AND userId = :userId")
    suspend fun getTaskById(taskId: String, userId: String): Task?

    @Query("SELECT userId FROM tasks WHERE id = :id")
    suspend fun getOwner(id: String): String?

    @Query("UPDATE tasks SET isSynced = 0, revision = revision + 1 WHERE id = :id AND userId = :userId")
    suspend fun markDirty(id: String, userId: String)

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

    @Query("UPDATE tasks SET isSynced = 1 WHERE id = :taskId AND userId = :userId AND revision = :revision")
    suspend fun acknowledgeRevision(taskId: String, userId: String, revision: Long): Int

    @Transaction
    suspend fun refreshTasksTransaction(userId: String, tasks: List<Task>, pendingDeletes: Set<String>) {
        // Only authoritative Source.SERVER snapshots may propagate remote permanent deletes.
        deleteSyncedTasksNotInSet(userId, tasks.map { it.id })
        val unsyncedIds = getUnsyncedTasks(userId).map { it.id }.toSet()
        tasks.forEach { remote ->
            if (remote.id !in pendingDeletes && remote.id !in unsyncedIds) {
                val owner = getOwner(remote.id)
                if (owner != null && owner != userId) return@forEach
                val local = getTaskById(remote.id, userId)
                if (local == null || remote.revision >= local.revision) {
                    insertTask(remote.copy(userId = userId, isSynced = true))
                }
            }
        }
    }
}
