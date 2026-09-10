package com.katchy.focuslive.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.katchy.focuslive.data.local.dao.LearningSpaceDao
import com.katchy.focuslive.data.local.dao.PendingDeleteDao
import com.katchy.focuslive.data.model.LearningSpace
import com.katchy.focuslive.data.model.PendingDelete
import com.katchy.focuslive.data.worker.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
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
    suspend fun refreshSpaces(): Result<Unit>
    suspend fun clearUserData(userId: String): Result<Unit>
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
class LearningSpaceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val spaceDao: LearningSpaceDao,
    private val pendingDeleteDao: PendingDeleteDao,
    @ApplicationContext private val context: Context
) : LearningSpaceRepository {

    companion object {
        private const val TYPE = "learning_space"
    }

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
            pendingDeleteDao.delete(space.id, TYPE, userId)
            if (isNew) spaceDao.insertSpace(space.copy(userId = userId, isSynced = false))
            else spaceDao.updateSpace(space.copy(userId = userId, isSynced = false))
            scheduleSync()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun archiveSpace(spaceId: String, isArchived: Boolean): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return try {
            spaceDao.setArchived(spaceId, userId, isArchived)
            scheduleSync()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteSpace(spaceId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return try {
            spaceDao.deleteSpaceById(spaceId, userId)
            pendingDeleteDao.insert(PendingDelete(spaceId, TYPE, userId))
            scheduleSync()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun syncPendingChanges(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return try {
            val collection = collectionFor(userId)
            spaceDao.getUnsyncedSpaces(userId).forEach { space ->
                collection.document(space.id).set(space.copy(isSynced = true)).await()
                spaceDao.updateSyncStatus(space.id, userId, true)
            }
            pendingDeleteDao.getPendingDeletes(userId, TYPE).forEach { tombstone ->
                collection.document(tombstone.itemId).delete().await()
                pendingDeleteDao.delete(tombstone.itemId, TYPE, userId)
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun refreshSpaces(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return try {
            val spaces = collectionFor(userId).get().await().toObjects(LearningSpace::class.java)
            val pending = pendingDeleteDao.getPendingDeletes(userId, TYPE).map { it.itemId }.toSet()
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

    private fun scheduleSync() {
        try {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(constraints).build()
            WorkManager.getInstance(context).enqueueUniqueWork("data_sync", ExistingWorkPolicy.KEEP, request)
        } catch (_: Exception) {}
    }
}
