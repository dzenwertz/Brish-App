package com.katchy.focuslive.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.katchy.focuslive.data.repository.FinanceRepository
import com.katchy.focuslive.data.repository.HabitRepository
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
    private val financeRepository: FinanceRepository,
    private val noteRepository: NoteRepository,
    private val habitRepository: HabitRepository,
    private val courseRepository: CourseRepository
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
        try {
            Log.d(TAG, "Starting synchronization (attempt ${runAttemptCount + 1})...")

            // 1. Upload Pending Changes
            val taskUpload = async { taskRepository.syncPendingChanges() }
            val financeUpload = async { financeRepository.syncPendingChanges() }
            val noteUpload = async { noteRepository.syncPendingChanges() }
            val habitUpload = async { habitRepository.syncPendingChanges() }
            val courseUpload = async { courseRepository.syncPendingChanges() }

            val uploadResults = awaitAll(taskUpload, financeUpload, noteUpload, habitUpload, courseUpload)
            val uploadsSuccessful = uploadResults.all { it.isSuccess }
            if (!uploadsSuccessful) {
                Log.w(TAG, "Upload phase failed. Retrying without destructive download.")
                return@coroutineScope Result.retry()
            }

            // 2. Download Latest Data (Refresh/Restore) to ensure local Room DB stays in sync with Firestore
            val shouldDownload = inputData.getBoolean("FORCE_REFRESH", true)
            if (shouldDownload) {
                val taskDownload = async { taskRepository.refreshTasks() }
                val financeDownload = async { financeRepository.refreshTransactions() }
                val noteDownload = async { noteRepository.refreshNotes() }
                val habitDownload = async { habitRepository.refreshHabits() }
                val courseDownload = async { courseRepository.refreshCourses() }

                val downloadResults = awaitAll(taskDownload, financeDownload, noteDownload, habitDownload, courseDownload)
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
