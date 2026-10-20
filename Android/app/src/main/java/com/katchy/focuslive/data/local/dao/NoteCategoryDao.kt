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

    @Query("UPDATE note_categories SET isSynced = 1 WHERE id = :id AND userId = :userId AND revision = :revision")
    suspend fun acknowledgeRevision(id: String, userId: String, revision: Long): Int

    @Query("UPDATE note_categories SET isSynced = :isSynced WHERE id = :id AND userId = :userId")
    suspend fun updateSyncStatus(id: String, userId: String, isSynced: Boolean)

    @Query("SELECT * FROM note_categories WHERE id = :id AND userId = :userId")
    suspend fun getCategoryById(id: String, userId: String): NoteCategory?

    @Query("UPDATE note_categories SET isSynced = 0, revision = revision + 1 WHERE id = :id AND userId = :userId")
    suspend fun markDirty(id: String, userId: String)

    @Query("SELECT userId FROM note_categories WHERE id = :id")
    suspend fun getOwner(id: String): String?

    @Query("SELECT payload FROM note_drafts WHERE userId = :userId")
    suspend fun getDraftPayloads(userId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: NoteCategory)

    @Update
    suspend fun updateCategory(category: NoteCategory)

    @Delete
    suspend fun deleteCategory(category: NoteCategory)

    @Query("DELETE FROM note_categories WHERE id = :id AND userId = :userId")
    suspend fun deleteCategoryById(id: String, userId: String)
    
    @Query("UPDATE notes SET categoryId = null, isSynced = 0, revision = revision + 1, updatedAt = :updatedAt WHERE categoryId = :categoryId AND userId = :userId")
    suspend fun detachNotesFromCategory(categoryId: String, userId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM note_categories WHERE userId = :userId")
    suspend fun deleteAllCategoriesForUser(userId: String)

    @Query("DELETE FROM note_categories WHERE userId = :userId AND isSynced = 1 AND id NOT IN (:validIds) AND NOT EXISTS (SELECT 1 FROM notes child WHERE child.userId = :userId AND child.categoryId = note_categories.id)")
    suspend fun deleteSyncedCategoriesNotInSet(userId: String, validIds: List<String>)

    @Query("DELETE FROM note_categories WHERE userId = :userId AND isSynced = 1")
    suspend fun deleteAllSyncedCategoriesForUser(userId: String)

    @androidx.room.Transaction
    suspend fun refreshCategoriesTransaction(
        userId: String,
        categories: List<NoteCategory>,
        pendingDeletes: Set<String>
    ) {
        // Preserve references owned by durable drafts, including drafts with no committed note.
        val draftReferences = getDraftPayloads(userId).mapNotNull { payload ->
            com.google.gson.Gson().fromJson(payload, com.katchy.focuslive.data.model.Note::class.java).categoryId
        }
        deleteSyncedCategoriesNotInSet(userId, (categories.map { it.id } + draftReferences).distinct())
        val unsyncedIds = getUnsyncedCategories(userId).map { it.id }.toSet()
        categories.forEach { cat ->
            if (cat.id !in pendingDeletes && cat.id !in unsyncedIds) {
                val owner = getOwner(cat.id)
                if (owner != null && owner != userId) return@forEach
                val local = getCategoryById(cat.id, userId)
                if (local == null || cat.revision >= local.revision) {
                    insertCategory(cat.copy(userId = userId, isSynced = true))
                }
            }
        }
    }
}
