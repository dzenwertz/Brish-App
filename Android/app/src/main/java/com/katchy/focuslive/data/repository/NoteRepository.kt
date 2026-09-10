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
import com.katchy.focuslive.data.local.dao.NoteDao
import com.katchy.focuslive.data.local.dao.PendingDeleteDao
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.model.PendingDelete
import com.katchy.focuslive.data.worker.SyncWorker
import com.katchy.focuslive.widget.NotesWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface NoteRepository {
    fun getNotes(): Flow<List<Note>>
    suspend fun addNote(note: Note): Result<Unit>
    suspend fun updateNote(note: Note): Result<Unit>
    suspend fun deleteNote(noteId: String): Result<Unit>
    suspend fun syncPendingChanges(): Result<Unit>
    suspend fun syncPendingChanges(userId: String): Result<Unit>
    suspend fun refreshNotes(): Result<Unit>
    suspend fun refreshNotes(userId: String): Result<Unit>
    suspend fun clearUserData(userId: String): Result<Unit>

    // Categories
    fun getCategories(): Flow<List<com.katchy.focuslive.data.model.NoteCategory>>
    suspend fun addCategory(category: com.katchy.focuslive.data.model.NoteCategory)
    suspend fun updateCategory(category: com.katchy.focuslive.data.model.NoteCategory)
    suspend fun deleteCategory(category: com.katchy.focuslive.data.model.NoteCategory)
}

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val noteDao: NoteDao,
    private val noteCategoryDao: com.katchy.focuslive.data.local.dao.NoteCategoryDao,
    private val pendingDeleteDao: PendingDeleteDao,
    @ApplicationContext private val context: Context
) : NoteRepository {

    companion object {
        private const val TAG = "NoteRepository"
        private const val PENDING_DELETE_TYPE_NOTE = "note"
        private const val PENDING_DELETE_TYPE_CATEGORY = "note_category"
    }

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private fun notesCollectionFor(userId: String) =
        firestore.collection("users").document(userId).collection("notes")

    private fun categoriesCollectionFor(userId: String) =
        firestore.collection("users").document(userId).collection("note_categories")

    // --- Categories (Synced to Cloud) ---
    override fun getCategories(): Flow<List<com.katchy.focuslive.data.model.NoteCategory>> {
        val userId = currentUserId ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return noteCategoryDao.getAllCategories(userId)
    }

    override suspend fun addCategory(category: com.katchy.focuslive.data.model.NoteCategory) {
        val userId = currentUserId ?: return
        pendingDeleteDao.delete(category.id, PENDING_DELETE_TYPE_CATEGORY, userId)
        noteCategoryDao.insertCategory(category.copy(userId = userId, isSynced = false))
        scheduleSync()
    }

    override suspend fun updateCategory(category: com.katchy.focuslive.data.model.NoteCategory) {
        val userId = currentUserId ?: return
        pendingDeleteDao.delete(category.id, PENDING_DELETE_TYPE_CATEGORY, userId)
        noteCategoryDao.updateCategory(category.copy(userId = userId, isSynced = false))
        scheduleSync()
    }

    override suspend fun deleteCategory(category: com.katchy.focuslive.data.model.NoteCategory) {
        val userId = currentUserId ?: return
        noteCategoryDao.deleteCategory(category.copy(userId = userId))
        noteCategoryDao.detachNotesFromCategory(category.id, userId)

        pendingDeleteDao.insert(
            PendingDelete(itemId = category.id, type = PENDING_DELETE_TYPE_CATEGORY, userId = userId)
        )

        // Fire-and-forget for remote deletion
        try {
            categoriesCollectionFor(userId).document(category.id).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete remote category ${category.id}", e)
        }
        scheduleSync()
    }

    override fun getNotes(): Flow<List<Note>> {
        val userId = currentUserId ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return noteDao.getAllNotes(userId)
    }

    override suspend fun addNote(note: Note): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return try {
            pendingDeleteDao.delete(note.id, PENDING_DELETE_TYPE_NOTE, userId)
            noteDao.insertNote(note.copy(userId = userId, isSynced = false))
            scheduleSync()
            
            // Update Widget
            try { NotesWidget().updateAll(context) } catch (e: Exception) { Log.e(TAG, "Widget update failed", e) }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNote(note: Note): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return try {
            pendingDeleteDao.delete(note.id, PENDING_DELETE_TYPE_NOTE, userId)
            noteDao.updateNote(note.copy(userId = userId, isSynced = false))
            scheduleSync()
            
            // Update Widget
            try { NotesWidget().updateAll(context) } catch (e: Exception) { Log.e(TAG, "Widget update failed", e) }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return try {
            noteDao.deleteNote(noteId, userId)

            pendingDeleteDao.insert(
                PendingDelete(itemId = noteId, type = PENDING_DELETE_TYPE_NOTE, userId = userId)
            )

            // Fire-and-forget for deletion
            try {
                notesCollectionFor(userId).document(noteId).delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete remote note $noteId", e)
            }

            scheduleSync()

            // Update Widget
            try { NotesWidget().updateAll(context) } catch (e: Exception) { Log.e(TAG, "Widget update failed", e) }

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
            // 1. Sync Categories first (because notes reference categoryId)
            val unsyncedCats = noteCategoryDao.getUnsyncedCategories(userId)
            if (unsyncedCats.isNotEmpty()) {
                val catCollection = categoriesCollectionFor(userId)
                unsyncedCats.forEach { item ->
                    catCollection.document(item.id).set(item.copy(isSynced = true)).await()
                    noteCategoryDao.updateSyncStatus(item.id, userId, true)
                }
            }

            // 2. Sync category deletes
            val pendingCatDeletes = pendingDeleteDao.getPendingDeletes(userId, PENDING_DELETE_TYPE_CATEGORY)
            if (pendingCatDeletes.isNotEmpty()) {
                val catCollection = categoriesCollectionFor(userId)
                pendingCatDeletes.forEach { tombstone ->
                    try {
                        catCollection.document(tombstone.itemId).delete().await()
                        pendingDeleteDao.delete(tombstone.itemId, PENDING_DELETE_TYPE_CATEGORY, userId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to delete remote category ${tombstone.itemId}", e)
                    }
                }
            }

            // 3. Sync Notes
            val unsyncedNotes = noteDao.getUnsyncedNotes(userId)
            if (unsyncedNotes.isNotEmpty()) {
                val collection = notesCollectionFor(userId)
                unsyncedNotes.forEach { item ->
                    val docRef = collection.document(item.id)
                    docRef.set(item.copy(isSynced = true)).await()
                    noteDao.updateSyncStatus(item.id, userId, true)
                }
            }

            // 4. Sync note deletes
            val pendingNoteDeletes = pendingDeleteDao.getPendingDeletes(userId, PENDING_DELETE_TYPE_NOTE)
            if (pendingNoteDeletes.isNotEmpty()) {
                val collection = notesCollectionFor(userId)
                pendingNoteDeletes.forEach { tombstone ->
                    try {
                        collection.document(tombstone.itemId).delete().await()
                        pendingDeleteDao.delete(tombstone.itemId, PENDING_DELETE_TYPE_NOTE, userId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to delete remote note ${tombstone.itemId}", e)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshNotes(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return refreshNotes(userId)
    }

    override suspend fun refreshNotes(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Refresh Categories
            val catCollection = categoriesCollectionFor(userId)
            val catSnapshot = catCollection.get().await()
            val categories = catSnapshot.toObjects(com.katchy.focuslive.data.model.NoteCategory::class.java)
            val pendingCatDeletes = pendingDeleteDao.getPendingDeletes(userId, PENDING_DELETE_TYPE_CATEGORY).map { it.itemId }.toSet()
            noteCategoryDao.refreshCategoriesTransaction(userId, categories, pendingCatDeletes)

            // 2. Refresh Notes (replace total)
            val collection = notesCollectionFor(userId)
            val snapshot = collection.get().await()
            val notes = snapshot.toObjects(Note::class.java)

            val pendingNoteDeletes = pendingDeleteDao.getPendingDeletes(userId, PENDING_DELETE_TYPE_NOTE).map { it.itemId }.toSet()
            noteDao.refreshNotesTransaction(userId, notes, pendingNoteDeletes)
            Log.d(TAG, "Refreshed ${categories.size} categories and ${notes.size} notes from cloud")

            // Update Widget
            try { NotesWidget().updateAll(context) } catch (e: Exception) { Log.e(TAG, "Widget update failed", e) }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Refresh Notes failed", e)
            Result.failure(e)
        }
    }

    override suspend fun clearUserData(userId: String): Result<Unit> {
        return try {
            noteCategoryDao.deleteAllCategoriesForUser(userId)
            noteDao.deleteAllNotesForUser(userId)
            pendingDeleteDao.deleteAllForUserAndType(userId, PENDING_DELETE_TYPE_CATEGORY)
            pendingDeleteDao.deleteAllForUserAndType(userId, PENDING_DELETE_TYPE_NOTE)
            Log.d(TAG, "Cleared notes and categories for user $userId")
            
            // Update Widget
            try { NotesWidget().updateAll(context) } catch (e: Exception) { Log.e(TAG, "Widget update failed", e) }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear notes/categories for user $userId", e)
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
