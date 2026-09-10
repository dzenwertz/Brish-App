package com.katchy.focuslive.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.katchy.focuslive.scheduler.NotificationScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AlarmBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationScheduler: NotificationScheduler
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "AlarmBackupWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting periodic alarm backup and rescheduling...")
            notificationScheduler.rescheduleAllAlarms()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "AlarmBackupWorker failed", e)
            Result.retry()
        }
    }
}
