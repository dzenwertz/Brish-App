package com.katchy.focuslive.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.katchy.focuslive.data.model.NoteCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteCategoryDao {
    @Query("SELECT * FROM note_categories WHERE userId = :userId ORDER BY sortOrder ASC")
    fun getAllCategories(userId: String): Flow<List<NoteCategory>>

    @Query("SELECT * FROM note_categories WHERE isSynced = 0 AND userId = :userId")
    suspend fun getUnsyncedCategories(userId: String): List<NoteCategory>

    @Query("UPDATE note_categories SET isSynced = :isSynced WHERE id = :id AND userId = :userId")
    suspend fun updateSyncStatus(id: String, userId: String, isSynced: Boolean)

    @Query("SELECT * FROM note_categories WHERE id = :id AND userId = :userId")
    suspend fun getCategoryById(id: String, userId: String): NoteCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: NoteCategory)

    @Update
    suspend fun updateCategory(category: NoteCategory)

    @Delete
    suspend fun deleteCategory(category: NoteCategory)
    
    @Query("UPDATE notes SET categoryId = null WHERE categoryId = :categoryId AND userId = :userId")
    suspend fun detachNotesFromCategory(categoryId: String, userId: String)

    @Query("DELETE FROM note_categories WHERE userId = :userId")
    suspend fun deleteAllCategoriesForUser(userId: String)

    @Query("DELETE FROM note_categories WHERE userId = :userId AND isSynced = 1 AND id NOT IN (:validIds)")
    suspend fun deleteSyncedCategoriesNotInSet(userId: String, validIds: List<String>)

    @Query("DELETE FROM note_categories WHERE userId = :userId AND isSynced = 1")
    suspend fun deleteAllSyncedCategoriesForUser(userId: String)

    @androidx.room.Transaction
    suspend fun refreshCategoriesTransaction(
        userId: String,
        categories: List<NoteCategory>,
        pendingDeletes: Set<String>
    ) {
        val unsyncedIds = getUnsyncedCategories(userId).map { it.id }.toSet()
        val incomingIds = categories.map { it.id }
        if (incomingIds.isEmpty()) {
            deleteAllSyncedCategoriesForUser(userId)
        } else {
            deleteSyncedCategoriesNotInSet(userId, incomingIds)
        }
        categories.forEach { cat ->
            if (cat.id !in pendingDeletes && cat.id !in unsyncedIds) {
                insertCategory(cat.copy(userId = userId, isSynced = true))
            }
        }
    }
}
