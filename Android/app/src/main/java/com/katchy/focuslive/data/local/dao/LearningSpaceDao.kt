package com.katchy.focuslive.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.katchy.focuslive.data.model.LearningSpace
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningSpaceDao {
    @Query("SELECT * FROM learning_spaces WHERE userId = :userId ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllSpaces(userId: String): Flow<List<LearningSpace>>

    @Query("SELECT * FROM learning_spaces WHERE userId = :userId AND isArchived = 0 ORDER BY sortOrder ASC, createdAt ASC")
    fun getActiveSpaces(userId: String): Flow<List<LearningSpace>>

    @Query("SELECT * FROM learning_spaces WHERE userId = :userId AND isArchived = 1 ORDER BY sortOrder ASC, createdAt ASC")
    fun getArchivedSpaces(userId: String): Flow<List<LearningSpace>>

    @Query("SELECT * FROM learning_spaces WHERE isSynced = 0 AND userId = :userId")
    suspend fun getUnsyncedSpaces(userId: String): List<LearningSpace>

    @Query("UPDATE learning_spaces SET isSynced = :isSynced WHERE id = :id AND userId = :userId")
    suspend fun updateSyncStatus(id: String, userId: String, isSynced: Boolean)

    @Query("SELECT * FROM learning_spaces WHERE id = :id AND userId = :userId")
    suspend fun getSpaceById(id: String, userId: String): LearningSpace?

    @Query("UPDATE learning_spaces SET isArchived = :isArchived, isSynced = 0 WHERE id = :id AND userId = :userId")
    suspend fun setArchived(id: String, userId: String, isArchived: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpace(space: LearningSpace)

    @Update
    suspend fun updateSpace(space: LearningSpace)

    @Delete
    suspend fun deleteSpace(space: LearningSpace)

    @Query("DELETE FROM learning_spaces WHERE id = :id AND userId = :userId")
    suspend fun deleteSpaceById(id: String, userId: String)

    @Query("DELETE FROM learning_spaces WHERE userId = :userId")
    suspend fun deleteAllSpacesForUser(userId: String)

    @Query("DELETE FROM learning_spaces WHERE userId = :userId AND isSynced = 1 AND id NOT IN (:validIds)")
    suspend fun deleteSyncedSpacesNotInSet(userId: String, validIds: List<String>)

    @Query("DELETE FROM learning_spaces WHERE userId = :userId AND isSynced = 1")
    suspend fun deleteAllSyncedSpacesForUser(userId: String)

    @androidx.room.Transaction
    suspend fun refreshSpacesTransaction(
        userId: String,
        spaces: List<LearningSpace>,
        pendingDeletes: Set<String>
    ) {
        val unsyncedIds = getUnsyncedSpaces(userId).map { it.id }.toSet()
        val incomingIds = spaces.map { it.id }
        if (incomingIds.isEmpty()) {
            deleteAllSyncedSpacesForUser(userId)
        } else {
            deleteSyncedSpacesNotInSet(userId, incomingIds)
        }
        spaces.forEach { space ->
            if (space.id !in pendingDeletes && space.id !in unsyncedIds) {
                insertSpace(space.copy(userId = userId, isSynced = true))
            }
        }
    }
}
