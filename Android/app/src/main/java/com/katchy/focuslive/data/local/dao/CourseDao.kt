package com.katchy.focuslive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.katchy.focuslive.data.model.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses WHERE userId = :userId ORDER BY name COLLATE NOCASE")
    fun getAllCourses(userId: String): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE isSynced = 0 AND userId = :userId")
    suspend fun getUnsyncedCourses(userId: String): List<Course>

    @Query("SELECT * FROM courses WHERE id = :courseId AND userId = :userId")
    suspend fun getCourseById(courseId: String, userId: String): Course?

    @Query("UPDATE courses SET isSynced = 0, revision = revision + 1 WHERE id = :id AND userId = :userId")
    suspend fun markDirty(id: String, userId: String)

    @Query("SELECT userId FROM courses WHERE id = :id")
    suspend fun getOwner(id: String): String?

    @Query("SELECT payload FROM note_drafts WHERE userId = :userId")
    suspend fun getDraftPayloads(userId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course)

    @Update
    suspend fun updateCourse(course: Course)

    @Query("UPDATE courses SET isSynced = 1 WHERE id = :courseId AND userId = :userId AND revision = :revision")
    suspend fun acknowledgeRevision(courseId: String, userId: String, revision: Long): Int

    @Query("UPDATE courses SET isSynced = :isSynced WHERE id = :courseId AND userId = :userId")
    suspend fun updateSyncStatus(courseId: String, userId: String, isSynced: Boolean)

    @Query("SELECT (SELECT COUNT(*) FROM tasks WHERE courseId = :courseId AND userId = :userId) + (SELECT COUNT(*) FROM notes WHERE courseId = :courseId AND userId = :userId)")
    suspend fun countCourseContents(courseId: String, userId: String): Int

    @Query("DELETE FROM courses WHERE id = :courseId AND userId = :userId")
    suspend fun deleteCourse(courseId: String, userId: String)

    @Query("DELETE FROM courses WHERE userId = :userId")
    suspend fun deleteAllCoursesForUser(userId: String)

    @Query("DELETE FROM courses WHERE userId = :userId AND isSynced = 1 AND id NOT IN (:validIds) AND NOT EXISTS (SELECT 1 FROM tasks child WHERE child.userId = :userId AND child.courseId = courses.id) AND NOT EXISTS (SELECT 1 FROM notes child WHERE child.userId = :userId AND child.courseId = courses.id)")
    suspend fun deleteSyncedCoursesNotInSet(userId: String, validIds: List<String>)

    @Query("DELETE FROM courses WHERE userId = :userId AND isSynced = 1")
    suspend fun deleteAllSyncedCoursesForUser(userId: String)

    @Transaction
    suspend fun refreshCoursesTransaction(userId: String, courses: List<Course>, pendingDeletes: Set<String>) {
        // Preserve references owned by durable drafts, including drafts with no committed note.
        val draftReferences = getDraftPayloads(userId).mapNotNull { payload ->
            com.google.gson.Gson().fromJson(payload, com.katchy.focuslive.data.model.Note::class.java).courseId
        }
        deleteSyncedCoursesNotInSet(userId, (courses.map { it.id } + draftReferences).distinct())
        val unsyncedIds = getUnsyncedCourses(userId).map { it.id }.toSet()
        courses.filter { it.id !in pendingDeletes && it.id !in unsyncedIds }
            .forEach { remote ->
                val owner = getOwner(remote.id)
                if (owner != null && owner != userId) return@forEach
                val local = getCourseById(remote.id, userId)
                if (local == null || remote.revision >= local.revision) {
                    insertCourse(remote.copy(userId = userId, isSynced = true))
                }
            }
    }
}
