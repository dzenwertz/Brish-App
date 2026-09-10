package com.katchy.focuslive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.katchy.focuslive.data.model.Habit
import kotlinx.coroutines.flow.Flow

import androidx.room.Transaction

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllHabits(userId: String): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :habitId AND userId = :userId")
    suspend fun getHabitById(habitId: String, userId: String): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Query("SELECT * FROM habits WHERE isSynced = 0 AND userId = :userId")
    suspend fun getUnsyncedHabits(userId: String): List<Habit>

    @Query("UPDATE habits SET isSynced = :isSynced WHERE id = :habitId AND userId = :userId")
    suspend fun updateSyncStatus(habitId: String, userId: String, isSynced: Boolean)

    @Query("DELETE FROM habits WHERE id = :habitId AND userId = :userId")
    suspend fun deleteHabit(habitId: String, userId: String)

    @Query("DELETE FROM habits WHERE userId = :userId")
    suspend fun deleteAllHabitsForUser(userId: String)

    @Query("DELETE FROM habits WHERE userId = :userId AND isSynced = 1 AND id NOT IN (:validIds)")
    suspend fun deleteSyncedHabitsNotInSet(userId: String, validIds: List<String>)

    @Query("DELETE FROM habits WHERE userId = :userId AND isSynced = 1")
    suspend fun deleteAllSyncedHabitsForUser(userId: String)

    @Transaction
    suspend fun refreshHabitsTransaction(userId: String, habits: List<Habit>, pendingDeletes: Set<String>) {
        val unsyncedIds = getUnsyncedHabits(userId).map { it.id }.toSet()
        val incomingIds = habits.map { it.id }
        if (incomingIds.isEmpty()) {
            deleteAllSyncedHabitsForUser(userId)
        } else {
            deleteSyncedHabitsNotInSet(userId, incomingIds)
        }
        habits.forEach { habit ->
            if (habit.id !in pendingDeletes && habit.id !in unsyncedIds) {
                insertHabit(habit.copy(userId = userId, isSynced = true))
            }
        }
    }
}
