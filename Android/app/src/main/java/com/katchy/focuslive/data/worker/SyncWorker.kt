package com.katchy.focuslive.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.katchy.focuslive.data.repository.NoteRepository
import com.katchy.focuslive.data.repository.TaskRepository
import com.katchy.focuslive.data.repository.CourseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val courseRepository: CourseRepository,
    private val spaceRepository: com.katchy.focuslive.data.repository.LearningSpaceRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result = coroutineScope {
        // Guard against infinite retries (e.g. corrupted local data that always fails to sync)
        if (runAttemptCount >= 5) {
            Log.e(TAG, "SyncWorker: max retry attempts (5) reached, giving up.")
            return@coroutineScope Result.failure()
        }
        val owner = inputData.getString(SyncScheduler.OWNER_KEY)
        // Legacy jobs without an owner are discarded; no job adopts a different login.
        if (owner.isNullOrBlank() || auth.currentUser?.uid != owner) return@coroutineScope Result.success()
        try {
            Log.d(TAG, "Starting synchronization (attempt ${runAttemptCount + 1})...")

            // 1. Upload Pending Changes
            val taskUpload = async { taskRepository.syncPendingChanges(owner) }
            val noteUpload = async { noteRepository.syncPendingChanges(owner) }
            val spaceUpload = async { spaceRepository.syncPendingChanges(owner) }
            val courseUpload = async { courseRepository.syncPendingChanges(owner) }

            val uploadResults = awaitAll(taskUpload, noteUpload, courseUpload, spaceUpload)
            val uploadsSuccessful = uploadResults.all { it.isSuccess }
            if (!uploadsSuccessful) {
                Log.w(TAG, "Upload phase failed. Retrying without destructive download.")
                return@coroutineScope Result.retry()
            }

            if (auth.currentUser?.uid != owner) return@coroutineScope Result.success()

            // 2. Download Latest Data (Refresh/Restore) to ensure local Room DB stays in sync with Firestore
            val shouldDownload = inputData.getBoolean("FORCE_REFRESH", false)
            if (shouldDownload) {
                val childResults = awaitAll(
                    async { taskRepository.refreshTasks(owner) },
                    async { noteRepository.refreshNotes(owner) }
                )
                if (childResults.any { it.isFailure }) return@coroutineScope Result.retry()
                val coursesResult = courseRepository.refreshCourses(owner)
                if (coursesResult.isFailure) return@coroutineScope Result.retry()
                val spacesResult = spaceRepository.refreshSpaces(owner)
                val downloadResults = childResults + listOf(coursesResult, spacesResult)
                val allSuccessful = downloadResults.all { it.isSuccess }

                if (!allSuccessful) {
                    Log.w(TAG, "Some synchronizations failed. Retrying...")
                    return@coroutineScope Result.retry()
                }
            }

            Log.d(TAG, "Synchronization completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker failed", e)
            Result.retry()
        }
    }
}
