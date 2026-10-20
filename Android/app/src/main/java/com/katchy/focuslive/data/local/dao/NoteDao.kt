package com.katchy.focuslive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.katchy.focuslive.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE userId = :userId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllNotes(userId: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId AND userId = :userId")
    suspend fun getNoteById(noteId: String, userId: String): Note?

    @Query("SELECT * FROM notes WHERE userId = :userId AND deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrashNotes(userId: String): Flow<List<Note>>

    @Query("UPDATE notes SET isSynced = 1 WHERE id = :noteId AND userId = :userId AND revision = :revision")
    suspend fun acknowledgeRevision(noteId: String, userId: String, revision: Long): Int

    @Query("SELECT userId FROM notes WHERE id = :id")
    suspend fun getOwner(id: String): String?

    @Query("UPDATE notes SET isSynced = 0, revision = revision + 1 WHERE id = :id AND userId = :userId")
    suspend fun markDirty(id: String, userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Query("SELECT * FROM notes WHERE isSynced = 0 AND userId = :userId")
    suspend fun getUnsyncedNotes(userId: String): List<Note>

    @Query("UPDATE notes SET isSynced = :isSynced WHERE id = :noteId AND userId = :userId")
    suspend fun updateSyncStatus(noteId: String, userId: String, isSynced: Boolean)

    @Query("DELETE FROM notes WHERE id = :noteId AND userId = :userId")
    suspend fun deleteNote(noteId: String, userId: String)

    @Query("DELETE FROM notes WHERE userId = :userId")
    suspend fun deleteAllNotesForUser(userId: String)

    @Query("DELETE FROM notes WHERE userId = :userId AND isSynced = 1 AND id NOT IN (:validIds) AND NOT EXISTS (SELECT 1 FROM note_drafts d WHERE d.userId = :userId AND d.noteId = notes.id)")
    suspend fun deleteSyncedNotesNotInSet(userId: String, validIds: List<String>)

    @Query("DELETE FROM notes WHERE userId = :userId AND isSynced = 1 AND deletedAt IS NULL")
    suspend fun deleteAllSyncedNotesForUser(userId: String)

    @androidx.room.Transaction
    suspend fun refreshNotesTransaction(userId: String, notes: List<Note>, pendingDeletes: Set<String>) {
        // Called only after an authoritative Source.SERVER read; dirty rows/drafts survive absence.
        deleteSyncedNotesNotInSet(userId, notes.map { it.id })
        val unsyncedIds = getUnsyncedNotes(userId).map { it.id }.toSet()
        notes.forEach { note ->
            if (note.id !in pendingDeletes && note.id !in unsyncedIds) {
                val owner = getOwner(note.id)
                if (owner != null && owner != userId) return@forEach
                val local = getNoteById(note.id, userId)
                if (local == null || note.revision >= local.revision) {
                    insertNote(note.copy(userId = userId, isSynced = true))
                }
            }
        }
    }
}
