package com.katchy.focuslive.data.worker

import android.content.Context
import androidx.work.*

/** Every queued job keeps its original account; it must never adopt a later login. */
object SyncScheduler {
    const val OWNER_KEY = "SYNC_OWNER"
    private fun workName(owner: String) = "SyncWork:$owner"

    fun enqueue(context: Context, owner: String, forceRefresh: Boolean = false) {
        require(owner.isNotBlank()) { "Sync requires an account owner" }
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(workDataOf(OWNER_KEY to owner, "FORCE_REFRESH" to forceRefresh))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(owner), ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun cancel(context: Context, owner: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(owner))
    }
}
