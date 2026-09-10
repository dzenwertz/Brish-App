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
import com.katchy.focuslive.data.local.dao.FinanceDao
import com.katchy.focuslive.data.local.dao.PendingDeleteDao
import com.katchy.focuslive.data.model.PendingDelete
import com.katchy.focuslive.data.model.Transaction
import com.katchy.focuslive.data.worker.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface FinanceRepository {
    fun getTransactions(): Flow<List<Transaction>>
    suspend fun addTransaction(transaction: Transaction): Result<Unit>
    suspend fun updateTransaction(transaction: Transaction): Result<Unit>
    suspend fun deleteTransaction(transactionId: String): Result<Unit>
    suspend fun getTransactionCount(): Int
    suspend fun syncPendingChanges(): Result<Unit>
    suspend fun syncPendingChanges(userId: String): Result<Unit>
    suspend fun refreshTransactions(): Result<Unit>
    suspend fun refreshTransactions(userId: String): Result<Unit>
    suspend fun clearUserData(userId: String): Result<Unit>
}

@Singleton
class FinanceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val financeDao: FinanceDao,
    private val pendingDeleteDao: PendingDeleteDao,
    @ApplicationContext private val context: Context
) : FinanceRepository {

    companion object {
        private const val TAG = "FinanceRepository"
        private const val PENDING_DELETE_TYPE = "transaction"
    }

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private fun collectionFor(userId: String) =
        firestore.collection("users").document(userId).collection("finance")

    private fun currentUserIdFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTransactions(): Flow<List<Transaction>> {
        return currentUserIdFlow().flatMapLatest { userId ->
            if (userId != null) {
                financeDao.getAllTransactions(userId)
            } else {
                flowOf(emptyList())
            }
        }
    }

    override suspend fun addTransaction(transaction: Transaction): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return try {
            pendingDeleteDao.delete(transaction.id, PENDING_DELETE_TYPE, userId)
            financeDao.insertTransaction(transaction.copy(userId = userId, isSynced = false))
            scheduleSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTransactionCount(): Int {
        val userId = currentUserId ?: return 0
        return financeDao.getTransactionCount(userId)
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return try {
            pendingDeleteDao.delete(transaction.id, PENDING_DELETE_TYPE, userId)
            financeDao.updateTransaction(transaction.copy(userId = userId, isSynced = false))
            scheduleSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return try {
            financeDao.deleteTransaction(transactionId, userId)

            pendingDeleteDao.insert(
                PendingDelete(itemId = transactionId, type = PENDING_DELETE_TYPE, userId = userId)
            )

            // Fire-and-forget for deletion (limit of current offline-delete strategy)
             try {
                collectionFor(userId).document(transactionId).delete()
            } catch (e: Exception) {
                 Log.e(TAG, "Failed to delete remote transaction $transactionId", e)
            }
            scheduleSync()
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
            val collection = collectionFor(userId)

            val unsynced = financeDao.getUnsyncedTransactions(userId)
            if (unsynced.isNotEmpty()) {
                unsynced.forEach { item ->
                    collection.document(item.id).set(item.copy(isSynced = true)).await()
                    financeDao.updateSyncStatus(item.id, userId, true)
                }
            }

            val pendingDeletes = pendingDeleteDao.getPendingDeletes(userId, PENDING_DELETE_TYPE)
            if (pendingDeletes.isNotEmpty()) {
                pendingDeletes.forEach { tombstone ->
                    try {
                        collection.document(tombstone.itemId).delete().await()
                        pendingDeleteDao.delete(tombstone.itemId, PENDING_DELETE_TYPE, userId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to delete remote transaction ${tombstone.itemId}", e)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshTransactions(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("No user logged in"))
        return refreshTransactions(userId)
    }

    override suspend fun refreshTransactions(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val collection = collectionFor(userId)
            val snapshot = collection.get().await()
            val transactions = snapshot.toObjects(Transaction::class.java)

            val pendingDeletes = pendingDeleteDao.getPendingDeletes(userId, PENDING_DELETE_TYPE).map { it.itemId }.toSet()
            financeDao.refreshTransactionsTransaction(userId, transactions, pendingDeletes)
            Log.d(TAG, "Refreshed ${transactions.size} transactions from cloud")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Refresh failed", e)
            Result.failure(e)
        }
    }

    override suspend fun clearUserData(userId: String): Result<Unit> {
        return try {
            financeDao.deleteAllTransactionsForUser(userId)
            pendingDeleteDao.deleteAllForUserAndType(userId, PENDING_DELETE_TYPE)
            Log.d(TAG, "Cleared transactions for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear transactions for user $userId", e)
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
