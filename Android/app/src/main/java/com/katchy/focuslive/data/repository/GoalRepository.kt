package com.katchy.focuslive.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.katchy.focuslive.data.model.Goal
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface GoalRepository {
    fun getGoals(): Flow<List<Goal>>
    suspend fun addGoal(goal: Goal): Result<Unit>
    suspend fun updateGoal(goal: Goal): Result<Unit>
    suspend fun deleteGoal(goalId: String): Result<Unit>
}

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : GoalRepository {

    companion object {
        private const val TAG = "GoalRepository"
    }

    private val goalsCollection
        get() = auth.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).collection("goals")
        }

    override fun getGoals(): Flow<List<Goal>> = callbackFlow {
        val collection = goalsCollection
        if (collection == null) {
            Log.w(TAG, "No user logged in, returning empty goals list")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = collection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching goals: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val goals = snapshot?.toObjects(Goal::class.java) ?: emptyList()
                trySend(goals)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun addGoal(goal: Goal): Result<Unit> {
        return try {
            val collection = goalsCollection
            if (collection == null) {
                Log.w(TAG, "Cannot add goal: No user logged in")
                return Result.failure(IllegalStateException("Usuario no autenticado"))
            }
            collection.add(goal).await()
            Log.d(TAG, "Goal added successfully: ${goal.title}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding goal: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateGoal(goal: Goal): Result<Unit> {
        return try {
            val collection = goalsCollection
            if (collection == null) {
                Log.w(TAG, "Cannot update goal: No user logged in")
                return Result.failure(IllegalStateException("Usuario no autenticado"))
            }
            if (goal.id.isEmpty()) {
                Log.w(TAG, "Cannot update goal: Empty ID")
                return Result.failure(IllegalArgumentException("ID de meta vacío"))
            }
            collection.document(goal.id).set(goal).await()
            Log.d(TAG, "Goal updated successfully: ${goal.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating goal: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteGoal(goalId: String): Result<Unit> {
        return try {
            val collection = goalsCollection
            if (collection == null) {
                Log.w(TAG, "Cannot delete goal: No user logged in")
                return Result.failure(IllegalStateException("Usuario no autenticado"))
            }
            if (goalId.isEmpty()) {
                Log.w(TAG, "Cannot delete goal: Empty ID")
                return Result.failure(IllegalArgumentException("ID de meta vacío"))
            }
            collection.document(goalId).delete().await()
            Log.d(TAG, "Goal deleted successfully: $goalId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting goal: ${e.message}", e)
            Result.failure(e)
        }
    }
}
