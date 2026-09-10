package com.katchy.focuslive.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.katchy.focuslive.data.local.dao.CourseDao
import com.katchy.focuslive.data.local.dao.PendingDeleteDao
import com.katchy.focuslive.data.model.Course
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

interface CourseRepository {
    fun getCourses(): Flow<List<Course>>
    suspend fun addCourse(course: Course): Result<Unit>
    suspend fun updateCourse(course: Course): Result<Unit>
    suspend fun deleteCourse(courseId: String): Result<Unit>
    suspend fun syncPendingChanges(): Result<Unit>
    suspend fun refreshCourses(): Result<Unit>
    suspend fun clearUserData(userId: String): Result<Unit>
}

@Singleton
class CourseRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val courseDao: CourseDao,
    private val pendingDeleteDao: PendingDeleteDao,
    @ApplicationContext private val context: Context
) : CourseRepository {
    private val currentUserId: String? get() = auth.currentUser?.uid
    private fun collectionFor(userId: String) = firestore.collection("users").document(userId).collection("courses")
    private fun userIdFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun getCourses(): Flow<List<Course>> = userIdFlow().flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList()) else courseDao.getAllCourses(userId)
    }

    override suspend fun addCourse(course: Course): Result<Unit> = save(course, isNew = true)
    override suspend fun updateCourse(course: Course): Result<Unit> = save(course, isNew = false)

    private suspend fun save(course: Course, isNew: Boolean): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return try {
            pendingDeleteDao.delete(course.id, TYPE, userId)
            if (isNew) courseDao.insertCourse(course.copy(userId = userId, isSynced = false))
            else courseDao.updateCourse(course.copy(userId = userId, isSynced = false))
            scheduleSync()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteCourse(courseId: String): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return try {
            courseDao.deleteCourse(courseId, userId)
            pendingDeleteDao.insert(PendingDelete(courseId, TYPE, userId))
            scheduleSync()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun syncPendingChanges(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return try {
            val collection = collectionFor(userId)
            courseDao.getUnsyncedCourses(userId).forEach { course ->
                collection.document(course.id).set(course.copy(isSynced = true)).await()
                courseDao.updateSyncStatus(course.id, userId, true)
            }
            pendingDeleteDao.getPendingDeletes(userId, TYPE).forEach { tombstone ->
                collection.document(tombstone.itemId).delete().await()
                pendingDeleteDao.delete(tombstone.itemId, TYPE, userId)
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun refreshCourses(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return try {
            val courses = collectionFor(userId).get().await().toObjects(Course::class.java)
            val pending = pendingDeleteDao.getPendingDeletes(userId, TYPE).map { it.itemId }.toSet()
            courseDao.refreshCoursesTransaction(userId, courses, pending)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun clearUserData(userId: String): Result<Unit> = try {
        courseDao.deleteAllCoursesForUser(userId)
        pendingDeleteDao.deleteAllForUserAndType(userId, TYPE)
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    private fun scheduleSync() = WorkManager.getInstance(context).enqueueUniqueWork(
        "SyncWork", ExistingWorkPolicy.APPEND_OR_REPLACE,
        OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        ).build()
    )

    private companion object { const val TYPE = "course" }
}
