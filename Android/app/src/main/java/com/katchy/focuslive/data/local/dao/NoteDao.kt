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
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllNotes(userId: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId AND userId = :userId")
    suspend fun getNoteById(noteId: String, userId: String): Note?

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

    @Query("DELETE FROM notes WHERE userId = :userId AND isSynced = 1 AND id NOT IN (:validIds)")
    suspend fun deleteSyncedNotesNotInSet(userId: String, validIds: List<String>)

    @Query("DELETE FROM notes WHERE userId = :userId AND isSynced = 1")
    suspend fun deleteAllSyncedNotesForUser(userId: String)

    @androidx.room.Transaction
    suspend fun refreshNotesTransaction(userId: String, notes: List<Note>, pendingDeletes: Set<String>) {
        val unsyncedIds = getUnsyncedNotes(userId).map { it.id }.toSet()
        val incomingIds = notes.map { it.id }
        if (incomingIds.isEmpty()) {
            deleteAllSyncedNotesForUser(userId)
        } else {
            deleteSyncedNotesNotInSet(userId, incomingIds)
        }
        notes.forEach { note ->
            if (note.id !in pendingDeletes && note.id !in unsyncedIds) {
                insertNote(note.copy(userId = userId, isSynced = true))
            }
        }
    }
}
