package com.katchy.focuslive.data.repository

import androidx.room.withTransaction
import com.katchy.focuslive.data.local.BrishDatabase
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.katchy.focuslive.data.local.dao.CourseDao
import com.katchy.focuslive.data.local.dao.PendingDeleteDao
import com.katchy.focuslive.data.model.Course
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

interface CourseRepository {
    fun getCourses(): Flow<List<Course>>
    suspend fun addCourse(course: Course): Result<Unit>
    suspend fun updateCourse(course: Course): Result<Unit>
    suspend fun deleteCourse(courseId: String): Result<Unit>
    suspend fun syncPendingChanges(): Result<Unit>
    suspend fun syncPendingChanges(userId: String): Result<Unit>
    suspend fun refreshCourses(): Result<Unit>
    suspend fun refreshCourses(userId: String): Result<Unit>
    suspend fun clearUserData(userId: String): Result<Unit>
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
class CourseRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val courseDao: CourseDao,
    private val pendingDeleteDao: PendingDeleteDao,
    private val database: BrishDatabase,
    @ApplicationContext private val context: Context
) : CourseRepository {
    private val syncMutex = Mutex()
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
            require(course.userId.isBlank() || course.userId == userId) { "Account changed" }
            database.withTransaction {
                require(courseDao.getOwner(course.id).let { it == null || it == userId }) { "Course belongs to another account" }
                val local = courseDao.getCourseById(course.id, userId)
                if (isNew) require(local == null) { "Course already exists" }
                else {
                    require(local != null) { "Course not found" }
                    require(local.revision == course.revision) { "Course changed; reload before saving" }
                }
                if (course.spaceId.isNotBlank()) {
                    require(database.learningSpaceDao().getSpaceById(course.spaceId, userId) != null) { "Space no longer exists" }
                }
                pendingDeleteDao.delete(course.id, TYPE, userId)
                courseDao.insertCourse(course.copy(userId = userId, isSynced = false, revision = (local?.revision ?: 0) + 1))
            }
            scheduleSync(userId)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteCourse(courseId: String): Result<Unit> = syncMutex.withLock {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        try {
            database.withTransaction {
                require(courseDao.getCourseById(courseId, userId) != null) { "Course not found" }
                require(courseDao.countCourseContents(courseId, userId) == 0) { "Cannot delete a nonempty course" }
                require(database.noteDraftDao().getDrafts(userId).none {
                    com.google.gson.Gson().fromJson(it.payload, com.katchy.focuslive.data.model.Note::class.java).courseId == courseId
                }) { "Cannot delete a course containing drafts" }
                pendingDeleteDao.insert(PendingDelete(courseId, TYPE, userId))
                courseDao.deleteCourse(courseId, userId)
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
            courseDao.getUnsyncedCourses(userId).forEach { course ->
                check(currentUserId == userId) { "Account changed" }
                collection.document(course.id).set(course.copy(isSynced = true)).await()
                courseDao.acknowledgeRevision(course.id, userId, course.revision)
            }
            pendingDeleteDao.getPendingDeletes(userId, TYPE).forEach { tombstone ->
                check(currentUserId == userId) { "Account changed" }
                collection.document(tombstone.itemId).delete().await()
                pendingDeleteDao.acknowledgeDelete(tombstone.itemId, TYPE, userId, tombstone.timestamp)
                courseDao.markDirty(tombstone.itemId, userId)
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun refreshCourses(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("No user logged in"))
        return refreshCourses(userId)
    }

    override suspend fun refreshCourses(userId: String): Result<Unit> = syncMutex.withLock {
        try {
            check(currentUserId == userId) { "Account changed" }
            val courses = collectionFor(userId).get(com.google.firebase.firestore.Source.SERVER).await().toObjects(Course::class.java)
            val pending = pendingDeleteDao.getPendingDeletes(userId, TYPE).map { it.itemId }.toSet()
            check(currentUserId == userId) { "Account changed" }
            courseDao.refreshCoursesTransaction(userId, courses, pending)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }


    override suspend fun clearUserData(userId: String): Result<Unit> = try {
        courseDao.deleteAllCoursesForUser(userId)
        pendingDeleteDao.deleteAllForUserAndType(userId, TYPE)
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    private fun scheduleSync(owner: String) {
        try { com.katchy.focuslive.data.worker.SyncScheduler.enqueue(context, owner) }
        catch (e: Exception) { android.util.Log.e("SyncScheduler", "Local changes retained; enqueue failed", e) }
    }

    private companion object { const val TYPE = "course" }
}
