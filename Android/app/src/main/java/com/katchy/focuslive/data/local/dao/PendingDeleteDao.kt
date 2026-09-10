package com.katchy.focuslive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.katchy.focuslive.data.model.PendingDelete

@Dao
interface PendingDeleteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingDelete: PendingDelete)

    @Query("SELECT * FROM pending_deletes WHERE userId = :userId AND type = :type")
    suspend fun getPendingDeletes(userId: String, type: String): List<PendingDelete>

    @Query("DELETE FROM pending_deletes WHERE itemId = :itemId AND type = :type AND userId = :userId")
    suspend fun delete(itemId: String, type: String, userId: String)

    @Query("DELETE FROM pending_deletes WHERE userId = :userId AND type = :type")
    suspend fun deleteAllForUserAndType(userId: String, type: String)

    @Query("DELETE FROM pending_deletes WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
