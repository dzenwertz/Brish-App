package com.katchy.focuslive.data.repository

import androidx.room.withTransaction
import com.katchy.focuslive.data.local.BrishDatabase
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.katchy.focuslive.data.local.dao.LearningSpaceDao
import com.katchy.focuslive.data.local.dao.PendingDeleteDao
import com.katchy.focuslive.data.model.LearningSpace
import com.katchy.focuslive.data.model.PendingDelete
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

interface LearningSpaceRepository {
    fun getSpaces(): Flow<List<LearningSpace>>
    fun getActiveSpaces(): Flow<List<LearningSpace>>
    fun getArchivedSpaces(): Flow<List<LearningSpace>>
    suspend fun addSpace(space: LearningSpace): Result<Unit>
    suspend fun updateSpace(space: LearningSpace): Result<Unit>
    suspend fun deleteSpace(spaceId: String): Result<Unit>
    suspend fun archiveSpace(spaceId: String, isArchived: Boolean): Result<Unit>
    suspend fun syncPendingChanges(): Result<Unit>
    suspend fun syncPendingChanges(userId: String): Result<Unit>
    suspend fun refreshSpaces(): Result<Unit>
    suspend fun refreshSpaces(userId: String): Result<Unit>
    suspend fun clearUserData(userId: String): Result<Unit>
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
class LearningSpaceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val spaceDao: LearningSpaceDao,
    private val pendingDeleteDao: PendingDeleteDao,
    private val database: BrishDatabase,
    @ApplicationContext private val context: Context
) : LearningSpaceRepository {

    companion object {
        private const val TYPE = "learning_space"
    }

    private val syncMutex = Mutex()
    private val currentUserId: String? get() = auth.currentUser?.uid
    private fun collectionFor(userId: String) = firestore.collection("users").document(userId).collection("learning_spaces")

    private fun userIdFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun getSpaces(): Flow<List<LearningSpace>> = userIdFlow().flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList()) else spaceDao.getAllSpaces(userId)
    }

    override fun getActiveSpaces(): Flow<List<LearningSpace>> = userIdFlow().flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList()) else spaceDao.getActiveSpaces(userId)
    }

    override fun getArchivedSpaces(): Flow<List<LearningSpace>> = userIdFlow().flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList()) else spaceDao.getArchivedSpaces(userId)
    }

    override suspend fun addSpace(space: LearningSpace): Result<Unit> = save(space, isNew = true)
    override suspend fun updateSpace(space: LearningSpace): Result<Unit> = save(space, isNew = false)

    private suspend fun save(space: LearningSpace, isNew: Boolean): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return try {
            require(space.userId.isBlank() || space.userId == userId) { "Account changed" }
            database.withTransaction {
                require(spaceDao.getOwner(space.id).let { it == null || it == userId }) { "LearningSpace belongs to another account" }
                val local = spaceDao.getSpaceById(space.id, userId)
                if (isNew) require(local == null) { "LearningSpace already exists" }
                else {
                    require(local != null) { "LearningSpace not found" }
                    require(local.revision == space.revision) { "LearningSpace changed; reload before saving" }
                }
                pendingDeleteDao.delete(space.id, TYPE, userId)
                spaceDao.insertSpace(space.copy(userId = userId, isSynced = false, revision = (local?.revision ?: 0) + 1))
            }
            scheduleSync(userId)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun archiveSpace(spaceId: String, isArchived: Boolean): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return try {
            spaceDao.setArchived(spaceId, userId, isArchived)
            scheduleSync(userId)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteSpace(spaceId: String): Result<Unit> = syncMutex.withLock {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        try {
            database.withTransaction {
                require(spaceDao.getSpaceById(spaceId, userId) != null) { "Space not found" }
                require(spaceDao.countSpaceCourses(spaceId, userId) == 0) { "Cannot delete a nonempty space" }
                pendingDeleteDao.insert(PendingDelete(spaceId, TYPE, userId))
                spaceDao.deleteSpaceById(spaceId, userId)
            }
            scheduleSync(userId)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun syncPendingChanges(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return syncPendingChanges(userId)
    }

    override suspend fun syncPendingChanges(userId: String): Result<Unit> = syncMutex.withLock {
        try {
            check(currentUserId == userId) { "Account changed" }
            val collection = collectionFor(userId)
            spaceDao.getUnsyncedSpaces(userId).forEach { space ->
                check(currentUserId == userId) { "Account changed" }
                collection.document(space.id).set(space.copy(isSynced = true)).await()
                spaceDao.acknowledgeRevision(space.id, userId, space.revision)
            }
            pendingDeleteDao.getPendingDeletes(userId, TYPE).forEach { tombstone ->
                check(currentUserId == userId) { "Account changed" }
                collection.document(tombstone.itemId).delete().await()
                pendingDeleteDao.acknowledgeDelete(tombstone.itemId, TYPE, userId, tombstone.timestamp)
                spaceDao.markDirty(tombstone.itemId, userId)
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun refreshSpaces(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return refreshSpaces(userId)
    }

    override suspend fun refreshSpaces(userId: String): Result<Unit> = syncMutex.withLock {
        try {
            check(currentUserId == userId) { "Account changed" }
            val spaces = collectionFor(userId).get(com.google.firebase.firestore.Source.SERVER).await().toObjects(LearningSpace::class.java)
            val pending = pendingDeleteDao.getPendingDeletes(userId, TYPE).map { it.itemId }.toSet()
            check(currentUserId == userId) { "Account changed" }
            spaceDao.refreshSpacesTransaction(userId, spaces, pending)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }


    override suspend fun clearUserData(userId: String): Result<Unit> {
        return try {
            spaceDao.deleteAllSpacesForUser(userId)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun scheduleSync(owner: String) {
        try { com.katchy.focuslive.data.worker.SyncScheduler.enqueue(context, owner) }
        catch (e: Exception) { android.util.Log.e("SyncScheduler", "Local changes retained; enqueue failed", e) }
    }


}
