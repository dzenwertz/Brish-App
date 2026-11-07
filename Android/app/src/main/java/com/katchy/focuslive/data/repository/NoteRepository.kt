package com.katchy.focuslive.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.katchy.focuslive.data.local.dao.NoteDao
import com.katchy.focuslive.data.local.dao.PendingDeleteDao
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.model.PendingDelete
import com.katchy.focuslive.widget.NotesWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import androidx.room.withTransaction
import com.google.gson.Gson
import com.katchy.focuslive.data.local.BrishDatabase
import com.katchy.focuslive.data.model.NoteDraft
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface NoteRepository {
    suspend fun getNote(noteId: String): Note?
    fun getTrashNotes(): Flow<List<Note>>
    suspend fun restoreNote(noteId: String): Result<Unit>
    suspend fun deletePermanently(noteId: String): Result<Unit>
    suspend fun saveDraft(note: Note): Result<Unit>
    suspend fun getDraft(noteId: String): Note?
    fun getDrafts(): Flow<List<Note>>
    suspend fun clearDraft(noteId: String): Result<Unit>
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val noteDao: NoteDao,
    private val noteCategoryDao: com.katchy.focuslive.data.local.dao.NoteCategoryDao,
    private val pendingDeleteDao: PendingDeleteDao,
    private val database: BrishDatabase,
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
    private val syncMutex = Mutex()
    private val gson = Gson()
    private fun userIdFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun getCategories(): Flow<List<com.katchy.focuslive.data.model.NoteCategory>> =
        userIdFlow().flatMapLatest { id -> if (id == null) flowOf(emptyList()) else noteCategoryDao.getAllCategories(id) }

    override suspend fun addCategory(category: com.katchy.focuslive.data.model.NoteCategory) {
        val userId = currentUserId ?: error("No user logged in")
        require(category.userId.isBlank() || category.userId == userId) { "Account changed" }
        database.withTransaction {
            require(noteCategoryDao.getOwner(category.id).let { it == null || it == userId }) { "Folder belongs to another account" }
            val local = noteCategoryDao.getCategoryById(category.id, userId)
            pendingDeleteDao.delete(category.id, PENDING_DELETE_TYPE_CATEGORY, userId)
            noteCategoryDao.insertCategory(category.copy(userId = userId, isSynced = false, revision = (local?.revision ?: 0) + 1))
        }
        scheduleSync(userId)
    }

    override suspend fun updateCategory(category: com.katchy.focuslive.data.model.NoteCategory) {
        val userId = currentUserId ?: error("No user logged in")
        require(category.userId.isBlank() || category.userId == userId) { "Account changed" }
        database.withTransaction {
            require(noteCategoryDao.getOwner(category.id).let { it == null || it == userId }) { "Folder belongs to another account" }
            val local = noteCategoryDao.getCategoryById(category.id, userId) ?: error("Folder not found")
            require(local.revision == category.revision) { "Folder changed; reload before saving" }
            noteCategoryDao.updateCategory(category.copy(userId = userId, isSynced = false, revision = local.revision + 1))
        }
        scheduleSync(userId)
    }

    override suspend fun deleteCategory(category: com.katchy.focuslive.data.model.NoteCategory) = syncMutex.withLock {
        val userId = currentUserId ?: error("No user logged in")
        require(category.userId.isBlank() || category.userId == userId) { "Account changed" }
        database.withTransaction {
            require(noteCategoryDao.getCategoryById(category.id, userId) != null) { "Folder not found" }
            noteCategoryDao.deleteCategoryById(category.id, userId)
            noteCategoryDao.detachNotesFromCategory(category.id, userId)
            database.noteDraftDao().getDrafts(userId).forEach { draft ->
                val note = gson.fromJson(draft.payload, Note::class.java)
                if (note.categoryId == category.id) {
                    val local = noteDao.getNoteById(note.id, userId)
                    val detached = note.copy(categoryId = null, revision = local?.revision ?: note.revision)
                    database.noteDraftDao().saveDraft(draft.copy(payload = gson.toJson(detached)))
                }
            }
            pendingDeleteDao.insert(PendingDelete(itemId = category.id, type = PENDING_DELETE_TYPE_CATEGORY, userId = userId))
        }
        scheduleSync(userId)
    }

    override fun getNotes(): Flow<List<Note>> = userIdFlow().flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else noteDao.getAllNotes(id)
    }

    override fun getTrashNotes(): Flow<List<Note>> = userIdFlow().flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else noteDao.getTrashNotes(id)
    }

    override suspend fun getNote(noteId: String): Note? = currentUserId?.let { noteDao.getNoteById(noteId, it) }
    override fun getDrafts(): Flow<List<Note>> = userIdFlow().flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else database.noteDraftDao().observeDrafts(id).map { drafts ->
            drafts.map { gson.fromJson(it.payload, Note::class.java) }
        }
    }

    override suspend fun getDraft(noteId: String): Note? = currentUserId?.let { id ->
        database.noteDraftDao().getDraft(id, noteId)?.let { gson.fromJson(it.payload, Note::class.java) }
    }

    override suspend fun saveDraft(note: Note): Result<Unit> = runCatching {
        val id = currentUserId ?: error("No user logged in")
        require(note.userId.isNotBlank() && note.userId == id) { "Account changed; draft belongs to another account" }
        database.withTransaction {
            require(noteDao.getNoteById(note.id, id)?.deletedAt == null) { "Note is in trash" }
            note.courseId?.takeIf { it.isNotBlank() }?.let {
                require(database.courseDao().getCourseById(it, id) != null) { "Course no longer exists" }
            }
            note.categoryId?.takeIf { it.isNotBlank() }?.let {
                require(noteCategoryDao.getCategoryById(it, id) != null) { "Folder no longer exists" }
            }
            database.noteDraftDao().saveDraft(NoteDraft(id, note.id, gson.toJson(note), System.currentTimeMillis()))
        }
    }

    override suspend fun clearDraft(noteId: String): Result<Unit> = runCatching {
        val id = currentUserId ?: error("No user logged in")
        database.noteDraftDao().clearDraft(id, noteId)
    }

    override suspend fun addNote(note: Note): Result<Unit> = saveNote(note, true)
    override suspend fun updateNote(note: Note): Result<Unit> = saveNote(note, false)

    private suspend fun saveNote(note: Note, isNew: Boolean): Result<Unit> = runCatching {
        val id = currentUserId ?: error("No user logged in")
        require(note.userId.isBlank() || note.userId == id) { "Account changed" }
        database.withTransaction {
            require(noteDao.getOwner(note.id).let { it == null || it == id }) { "Note belongs to another account" }
            val local = noteDao.getNoteById(note.id, id)
            if (!isNew) {
                require(local != null) { "Note not found" }
                require(local.deletedAt == null) { "Note is in trash" }
                require(local.revision == note.revision) { "Note changed; reload before saving" }
            } else if (local != null) {
                require(local.deletedAt == null) { "Note is in trash" }
                require(local.revision == note.revision) { "Note changed; reload before saving" }
            }
            note.courseId?.takeIf { it.isNotBlank() }?.let {
                require(database.courseDao().getCourseById(it, id) != null) { "Course no longer exists" }
            }
            note.categoryId?.takeIf { it.isNotBlank() }?.let {
                require(noteCategoryDao.getCategoryById(it, id) != null) { "Folder no longer exists" }
            }
            pendingDeleteDao.delete(note.id, PENDING_DELETE_TYPE_NOTE, id)
            noteDao.insertNote(note.copy(userId = id, isSynced = false, deletedAt = null,
                updatedAt = System.currentTimeMillis(), revision = (local?.revision ?: note.revision) + 1))
            // Leave recovery drafts available until the editor explicitly clears them after success.
        }
        scheduleSync(id)
        try { NotesWidget().updateAll(context) } catch (e: Exception) { Log.e(TAG, "Widget update failed", e) }
        Unit
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> = setTrashed(noteId, true)
    override suspend fun restoreNote(noteId: String): Result<Unit> = setTrashed(noteId, false)

    private suspend fun setTrashed(noteId: String, trashed: Boolean): Result<Unit> = runCatching {
        val id = currentUserId ?: error("No user logged in")
        database.withTransaction {
            val local = noteDao.getNoteById(noteId, id) ?: error("Note not found")
            val now = System.currentTimeMillis()
            noteDao.updateNote(local.copy(deletedAt = if (trashed) now else null,
                updatedAt = now, revision = local.revision + 1, isSynced = false))
            if (trashed) database.noteDraftDao().clearDraft(id, noteId)
        }
        scheduleSync(id)
        try { NotesWidget().updateAll(context) } catch (e: Exception) { Log.e(TAG, "Widget update failed", e) }
        Unit
    }

    override suspend fun deletePermanently(noteId: String): Result<Unit> = syncMutex.withLock {
        runCatching {
            val id = currentUserId ?: error("No user logged in")
            database.withTransaction {
                val local = noteDao.getNoteById(noteId, id) ?: error("Note not found")
                require(local.deletedAt != null) { "Move note to trash first" }
                pendingDeleteDao.insert(PendingDelete(itemId = noteId, type = PENDING_DELETE_TYPE_NOTE, userId = id))
                noteDao.deleteNote(noteId, id)
                database.noteDraftDao().clearDraft(id, noteId)
            }
            scheduleSync(id)
        }
    }

    override suspend fun syncPendingChanges(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return syncPendingChanges(userId)
    }

    override suspend fun syncPendingChanges(userId: String): Result<Unit> = withContext(Dispatchers.IO) { syncMutex.withLock {
        try {
            check(currentUserId == userId) { "Account changed" }
            // 1. Sync Categories first (because notes reference categoryId)
            val unsyncedCats = noteCategoryDao.getUnsyncedCategories(userId)
            if (unsyncedCats.isNotEmpty()) {
                val catCollection = categoriesCollectionFor(userId)
                unsyncedCats.forEach { item ->
                    check(currentUserId == userId) { "Account changed" }
                    catCollection.document(item.id).set(item.copy(isSynced = true)).await()
                    noteCategoryDao.acknowledgeRevision(item.id, userId, item.revision)
                }
            }

            // 2. Sync category deletes
            val pendingCatDeletes = pendingDeleteDao.getPendingDeletes(userId, PENDING_DELETE_TYPE_CATEGORY)
            if (pendingCatDeletes.isNotEmpty()) {
                val catCollection = categoriesCollectionFor(userId)
                pendingCatDeletes.forEach { tombstone ->
                    try {
                        check(currentUserId == userId) { "Account changed" }
                        catCollection.document(tombstone.itemId).delete().await()
                        pendingDeleteDao.acknowledgeDelete(tombstone.itemId, PENDING_DELETE_TYPE_CATEGORY, userId, tombstone.timestamp)
                        noteCategoryDao.markDirty(tombstone.itemId, userId)
                    } catch (e: Exception) {
                        throw e
                    }
                }
            }

            // 3. Sync Notes
            val unsyncedNotes = noteDao.getUnsyncedNotes(userId)
            if (unsyncedNotes.isNotEmpty()) {
                val collection = notesCollectionFor(userId)
                unsyncedNotes.forEach { item ->
                    val docRef = collection.document(item.id)
                    check(currentUserId == userId) { "Account changed" }
                    docRef.set(item.copy(isSynced = true)).await()
                    noteDao.acknowledgeRevision(item.id, userId, item.revision)
                }
            }

            // 4. Sync note deletes
            val pendingNoteDeletes = pendingDeleteDao.getPendingDeletes(userId, PENDING_DELETE_TYPE_NOTE)
            if (pendingNoteDeletes.isNotEmpty()) {
                val collection = notesCollectionFor(userId)
                pendingNoteDeletes.forEach { tombstone ->
                    try {
                        check(currentUserId == userId) { "Account changed" }
                        collection.document(tombstone.itemId).delete().await()
                        pendingDeleteDao.acknowledgeDelete(tombstone.itemId, PENDING_DELETE_TYPE_NOTE, userId, tombstone.timestamp)
                        noteDao.markDirty(tombstone.itemId, userId)
                    } catch (e: Exception) {
                        throw e
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    }

    override suspend fun refreshNotes(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return refreshNotes(userId)
    }

    override suspend fun refreshNotes(userId: String): Result<Unit> = withContext(Dispatchers.IO) { syncMutex.withLock {
        try {
            check(currentUserId == userId) { "Account changed" }
            // 1. Refresh Categories
            val catCollection = categoriesCollectionFor(userId)
            val catSnapshot = catCollection.get(com.google.firebase.firestore.Source.SERVER).await()
            val categories = catSnapshot.toObjects(com.katchy.focuslive.data.model.NoteCategory::class.java)
            val pendingCatDeletes = pendingDeleteDao.getPendingDeletes(userId, PENDING_DELETE_TYPE_CATEGORY).map { it.itemId }.toSet()
            check(currentUserId == userId) { "Account changed" }
            noteCategoryDao.refreshCategoriesTransaction(userId, categories, pendingCatDeletes)

            // 2. Refresh Notes (replace total)
            val collection = notesCollectionFor(userId)
            val snapshot = collection.get(com.google.firebase.firestore.Source.SERVER).await()
            val notes = snapshot.toObjects(Note::class.java)

            val pendingNoteDeletes = pendingDeleteDao.getPendingDeletes(userId, PENDING_DELETE_TYPE_NOTE).map { it.itemId }.toSet()
            check(currentUserId == userId) { "Account changed" }
            noteDao.refreshNotesTransaction(userId, notes, pendingNoteDeletes)
            // Re-evaluate parent removal after note changes, preserving dirty/draft references.
            check(currentUserId == userId) { "Account changed" }
            noteCategoryDao.refreshCategoriesTransaction(userId, categories, pendingCatDeletes)
            Log.d(TAG, "Refreshed ${categories.size} categories and ${notes.size} notes from cloud")

            // Update Widget
            try { NotesWidget().updateAll(context) } catch (e: Exception) { Log.e(TAG, "Widget update failed", e) }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Refresh Notes failed", e)
            Result.failure(e)
        }
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

    private fun scheduleSync(owner: String) {
        try { com.katchy.focuslive.data.worker.SyncScheduler.enqueue(context, owner) }
        catch (e: Exception) { android.util.Log.e("SyncScheduler", "Local changes retained; enqueue failed", e) }
    }


}
