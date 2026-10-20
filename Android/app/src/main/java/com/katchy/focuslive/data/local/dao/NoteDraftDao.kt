package com.katchy.focuslive.data.local.dao

import androidx.room.*
import com.katchy.focuslive.data.model.NoteDraft

@Dao
interface NoteDraftDao {
    @Query("SELECT * FROM note_drafts WHERE userId = :userId AND noteId = :noteId")
    suspend fun getDraft(userId: String, noteId: String): NoteDraft?

    @Query("SELECT d.* FROM note_drafts d WHERE d.userId = :userId AND NOT EXISTS (SELECT 1 FROM notes n WHERE n.id = d.noteId AND n.userId = d.userId AND n.deletedAt IS NOT NULL) ORDER BY d.updatedAt DESC")
    fun observeDrafts(userId: String): kotlinx.coroutines.flow.Flow<List<NoteDraft>>

    @Query("SELECT * FROM note_drafts WHERE userId = :userId")
    suspend fun getDrafts(userId: String): List<NoteDraft>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: NoteDraft)

    @Query("DELETE FROM note_drafts WHERE userId = :userId AND noteId = :noteId")
    suspend fun clearDraft(userId: String, noteId: String)
}
