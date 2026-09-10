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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course)

    @Update
    suspend fun updateCourse(course: Course)

    @Query("UPDATE courses SET isSynced = :isSynced WHERE id = :courseId AND userId = :userId")
    suspend fun updateSyncStatus(courseId: String, userId: String, isSynced: Boolean)

    @Query("DELETE FROM courses WHERE id = :courseId AND userId = :userId")
    suspend fun deleteCourse(courseId: String, userId: String)

    @Query("DELETE FROM courses WHERE userId = :userId")
    suspend fun deleteAllCoursesForUser(userId: String)

    @Query("DELETE FROM courses WHERE userId = :userId AND isSynced = 1 AND id NOT IN (:validIds)")
    suspend fun deleteSyncedCoursesNotInSet(userId: String, validIds: List<String>)

    @Query("DELETE FROM courses WHERE userId = :userId AND isSynced = 1")
    suspend fun deleteAllSyncedCoursesForUser(userId: String)

    @Transaction
    suspend fun refreshCoursesTransaction(userId: String, courses: List<Course>, pendingDeletes: Set<String>) {
        val unsyncedIds = getUnsyncedCourses(userId).map { it.id }.toSet()
        val incomingIds = courses.map { it.id }
        if (incomingIds.isEmpty()) deleteAllSyncedCoursesForUser(userId)
        else deleteSyncedCoursesNotInSet(userId, incomingIds)
        courses.filter { it.id !in pendingDeletes && it.id !in unsyncedIds }
            .forEach { insertCourse(it.copy(userId = userId, isSynced = true)) }
    }
}
