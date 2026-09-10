package com.katchy.focuslive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.katchy.focuslive.data.model.Transaction
import kotlinx.coroutines.flow.Flow


@Dao
interface FinanceDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllTransactions(userId: String): Flow<List<com.katchy.focuslive.data.model.Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :transactionId AND userId = :userId")
    suspend fun getTransactionById(transactionId: String, userId: String): com.katchy.focuslive.data.model.Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: com.katchy.focuslive.data.model.Transaction)

    @Update
    suspend fun updateTransaction(transaction: com.katchy.focuslive.data.model.Transaction)

    @Query("SELECT * FROM transactions WHERE isSynced = 0 AND userId = :userId")
    suspend fun getUnsyncedTransactions(userId: String): List<com.katchy.focuslive.data.model.Transaction>

    @Query("UPDATE transactions SET isSynced = :isSynced WHERE id = :transactionId AND userId = :userId")
    suspend fun updateSyncStatus(transactionId: String, userId: String, isSynced: Boolean)

    @Query("DELETE FROM transactions WHERE id = :transactionId AND userId = :userId")
    suspend fun deleteTransaction(transactionId: String, userId: String)

    @Query("SELECT COUNT(*) FROM transactions WHERE userId = :userId")
    suspend fun getTransactionCount(userId: String): Int

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteAllTransactionsForUser(userId: String)

    @Query("DELETE FROM transactions WHERE userId = :userId AND isSynced = 1 AND id NOT IN (:validIds)")
    suspend fun deleteSyncedTransactionsNotInSet(userId: String, validIds: List<String>)

    @Query("DELETE FROM transactions WHERE userId = :userId AND isSynced = 1")
    suspend fun deleteAllSyncedTransactionsForUser(userId: String)

    @androidx.room.Transaction
    suspend fun refreshTransactionsTransaction(
        userId: String,
        transactions: List<com.katchy.focuslive.data.model.Transaction>,
        pendingDeletes: Set<String>
    ) {
        val unsyncedIds = getUnsyncedTransactions(userId).map { it.id }.toSet()
        val incomingIds = transactions.map { it.id }
        if (incomingIds.isEmpty()) {
            deleteAllSyncedTransactionsForUser(userId)
        } else {
            deleteSyncedTransactionsNotInSet(userId, incomingIds)
        }
        transactions.forEach { trans ->
            if (trans.id !in pendingDeletes && trans.id !in unsyncedIds) {
                insertTransaction(trans.copy(userId = userId, isSynced = true))
            }
        }
    }
}
