package com.katchy.focuslive.data.draft

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal interface DraftPersistence {
    suspend fun read(key: String): String?
    suspend fun write(values: Map<String, String?>)
}

private class PreferencesDraftPersistence(
    private val dataStore: DataStore<Preferences>
) : DraftPersistence {
    override suspend fun read(key: String): String? = dataStore.data.first()[stringPreferencesKey(key)]

    override suspend fun write(values: Map<String, String?>) {
        dataStore.edit { preferences ->
            values.forEach { (key, value) ->
                val preferenceKey = stringPreferencesKey(key)
                if (value == null) preferences.remove(preferenceKey) else preferences[preferenceKey] = value
            }
        }
    }
}

/** Persistent, debounced storage for unfinished form input. */
@Singleton
class DraftStore internal constructor(
    private val persistence: DraftPersistence,
    private val scope: CoroutineScope
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        createPersistence(context),
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )

    private val lock = Any()
    private val persistenceMutex = Mutex()
    private val pending = linkedMapOf<String, String?>()
    private val jobs = mutableMapOf<String, Job>()

    suspend fun read(key: String): String? = persistence.read(storageKey(key))

    fun scheduleWrite(key: String, value: String?) {
        val storageKey = storageKey(key)
        synchronized(lock) {
            pending[storageKey] = value
            jobs.remove(storageKey)?.cancel()
            jobs[storageKey] = scope.launch {
                delay(WRITE_DEBOUNCE_MS)
                flushOne(storageKey)
            }
        }
    }

    suspend fun writeNow(key: String, value: String?) {
        val storageKey = storageKey(key)
        synchronized(lock) {
            pending[storageKey] = value
            jobs.remove(storageKey)?.cancel()
        }
        flushPending()
    }

    fun writeBlocking(key: String, value: String?) {
        runBlocking(Dispatchers.IO) { writeNow(key, value) }
    }

    suspend fun flush() = flushPending()

    fun flushBlocking() {
        synchronized(lock) { jobs.values.forEach(Job::cancel); jobs.clear() }
        runBlocking(Dispatchers.IO) { flushPending() }
    }

    internal fun stopForTest() = scope.cancel()

    private suspend fun flushOne(key: String) {
        val write = synchronized(lock) {
            jobs.remove(key)
            if (!pending.containsKey(key)) null else key to pending.remove(key)
        } ?: return
        persistenceMutex.withLock { persistence.write(mapOf(write.first to write.second)) }
    }

    private suspend fun flushPending() {
        while (true) {
            val writes = synchronized(lock) {
                if (pending.isEmpty()) null else pending.toMap().also { pending.clear(); jobs.clear() }
            }
            if (writes == null) break
            persistenceMutex.withLock { persistence.write(writes) }
        }
        persistenceMutex.withLock { }
    }

    private fun storageKey(key: String): String = "draft_" +
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(key.toByteArray(Charsets.UTF_8))

    companion object {
        const val WRITE_DEBOUNCE_MS = 320L

        private fun createPersistence(context: Context): DraftPersistence {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
                context.applicationContext.preferencesDataStoreFile("form_drafts.preferences_pb")
            }
            return PreferencesDraftPersistence(dataStore)
        }
    }
}
