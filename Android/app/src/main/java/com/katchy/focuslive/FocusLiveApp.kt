package com.katchy.focuslive

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch

@HiltAndroidApp
class FocusLiveApp : Application(), ImageLoaderFactory, androidx.work.Configuration.Provider {


    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20) // Use 20% of available memory for caching
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .respectCacheHeaders(false) // Force caching
            .precision(coil.size.Precision.EXACT) // Load EXACT size to save RAM
            .build()
    }
    
    @javax.inject.Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        
        // --- GLOBAL CRASH HANDLER ---
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                // Report to Firebase Crashlytics explicitly
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(exception)
            } catch (e: Exception) {
                // Ignore Crashlytics logging failure
            }
            try {
                val fullTrace = android.util.Log.getStackTraceString(exception)
                val trace = if (fullTrace.length > 50000) fullTrace.take(50000) + "\n\n[TRUNCATED]" else fullTrace
                val crashFile = java.io.File(filesDir, "crash_log.txt")
                crashFile.writeText("Crash in ${thread.name}:\n$trace")
            } catch (e: Exception) {
                // Ignore file write failure
            }
            try {
                val fullTrace = android.util.Log.getStackTraceString(exception)
                val trace = if (fullTrace.length > 50000) fullTrace.take(50000) + "\n\n[TRUNCATED]" else fullTrace
                CrashActivity.start(this, "Crash in ${thread.name}:\n$trace")
            } catch (e: Exception) {
                // Fallback si falla arrancar la actividad
            }
            // CrashActivity runs in a separate :crash process so it survives this exit.
            kotlin.system.exitProcess(1)
        }
        
        // Initialize Firebase
        com.google.firebase.FirebaseApp.initializeApp(this)

        
        // Optimize Startup: Init SoundManager and schedule alarm backup in background
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.katchy.focuslive.ui.util.SoundManager.init(applicationContext)
            schedulePeriodicAlarmBackup()
        }
    }

    private fun schedulePeriodicAlarmBackup() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val backupRequest = androidx.work.PeriodicWorkRequestBuilder<com.katchy.focuslive.data.worker.AlarmBackupWorker>(
            12, java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AlarmBackupWork",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )
    }

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
