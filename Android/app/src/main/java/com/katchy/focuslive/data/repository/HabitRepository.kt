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
import com.katchy.focuslive.data.local.dao.HabitDao
import com.katchy.focuslive.data.local.dao.PendingDeleteDao
import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.data.model.PendingDelete
import com.katchy.focuslive.data.worker.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface HabitRepository {
    fun getHabits(): Flow<List<Habit>>
    suspend fun addHabit(habit: Habit): Result<Unit>
    suspend fun updateHabit(habit: Habit): Result<Unit>
    suspend fun deleteHabit(habitId: String): Result<Unit>
    suspend fun syncPendingChanges(): Result<Unit>
    suspend fun syncPendingChanges(userId: String): Result<Unit>
    suspend fun refreshHabits(): Result<Unit>
    suspend fun refreshHabits(userId: String): Result<Unit>
    suspend fun clearUserData(userId: String): Result<Unit>
}

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val habitDao: HabitDao,
    private val pendingDeleteDao: PendingDeleteDao,
    @ApplicationContext private val context: Context
) : HabitRepository {

    companion object {
        private const val TAG = "HabitRepository"
        private const val PENDING_DELETE_TYPE = "habit"
    }

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private fun collectionFor(userId: String) =
        firestore.collection("users").document(userId).collection("habits")

    override fun getHabits(): Flow<List<Habit>> {
        val userId = currentUserId ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return habitDao.getAllHabits(userId)
    }

    override suspend fun addHabit(habit: Habit): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return try {
            pendingDeleteDao.delete(habit.id, PENDING_DELETE_TYPE, userId)
            habitDao.insertHabit(habit.copy(userId = userId, isSynced = false))
            scheduleSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateHabit(habit: Habit): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return try {
            pendingDeleteDao.delete(habit.id, PENDING_DELETE_TYPE, userId)
            habitDao.updateHabit(habit.copy(userId = userId, isSynced = false))
            scheduleSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHabit(habitId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return try {
            habitDao.deleteHabit(habitId, userId)

            pendingDeleteDao.insert(
                PendingDelete(itemId = habitId, type = PENDING_DELETE_TYPE, userId = userId)
            )

            // Fire-and-forget for deletion
            try {
                collectionFor(userId).document(habitId).delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete remote habit $habitId", e)
            }
            scheduleSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPendingChanges(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return syncPendingChanges(userId)
    }

    override suspend fun syncPendingChanges(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val collection = collectionFor(userId)

            val unsynced = habitDao.getUnsyncedHabits(userId)
            if (unsynced.isNotEmpty()) {
                unsynced.forEach { item ->
                    collection.document(item.id).set(item.copy(isSynced = true)).await()
                    habitDao.updateSyncStatus(item.id, userId, true)
                }
            }

            val pendingDeletes = pendingDeleteDao.getPendingDeletes(userId, PENDING_DELETE_TYPE)
            if (pendingDeletes.isNotEmpty()) {
                pendingDeletes.forEach { tombstone ->
                    try {
                        collection.document(tombstone.itemId).delete().await()
                        pendingDeleteDao.delete(tombstone.itemId, PENDING_DELETE_TYPE, userId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to delete remote habit ${tombstone.itemId}", e)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshHabits(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return refreshHabits(userId)
    }

    override suspend fun refreshHabits(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val collection = collectionFor(userId)
            val snapshot = collection.get().await()
            val habits = snapshot.toObjects(Habit::class.java)

            val pendingDeletes = pendingDeleteDao.getPendingDeletes(userId, PENDING_DELETE_TYPE).map { it.itemId }.toSet()
            val mappedHabits = habits.map { habit ->
                val correctedStreak = com.katchy.focuslive.util.HabitUtils.calculateStreak(habit.completedDates)
                habit.copy(currentStreak = correctedStreak)
            }
            habitDao.refreshHabitsTransaction(userId, mappedHabits, pendingDeletes)
            Log.d(TAG, "Refreshed ${habits.size} habits from cloud")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Refresh failed", e)
            Result.failure(e)
        }
    }

    override suspend fun clearUserData(userId: String): Result<Unit> {
        return try {
            habitDao.deleteAllHabitsForUser(userId)
            pendingDeleteDao.deleteAllForUserAndType(userId, PENDING_DELETE_TYPE)
            Log.d(TAG, "Cleared habits for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear habits for user $userId", e)
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
