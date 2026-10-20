package com.katchy.focuslive.data.manager

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

internal const val MAX_TIMER_SECONDS = 120 * 60L
internal const val TIMER_CHECKPOINT_MS = 30_000L

/** One bounded, private snapshot; switching accounts discards the previous owner's session. */
internal data class TimerSnapshot(
    val owner: String,
    val state: String,
    val remainingSeconds: Long,
    val running: Boolean,
    val deadlineElapsedMs: Long,
    val savedElapsedMs: Long,
    val bootCount: Int?,
    val activeTaskId: String? = null,
    val sound: String = "NONE",
    val workSeconds: Long = 25 * 60L,
    val breakSeconds: Long = 5 * 60L
)

/** A reboot invalidates a monotonic deadline: keep the last checkpoint paused, never guess from wall time. */
internal fun restoreTimerSnapshot(snapshot: TimerSnapshot, owner: String, elapsedMs: Long, bootCount: Int?): TimerSnapshot? {
    if (snapshot.owner != owner || owner.isBlank() || owner.length > 256 ||
        snapshot.state !in setOf("WORK", "BREAK") || snapshot.sound !in TimerManager.AmbientSound.entries.map { it.name } ||
        snapshot.remainingSeconds !in 1..MAX_TIMER_SECONDS || snapshot.workSeconds !in 60..MAX_TIMER_SECONDS ||
        snapshot.breakSeconds !in 60..MAX_TIMER_SECONDS || (snapshot.activeTaskId?.length ?: 0) > 256 ||
        snapshot.savedElapsedMs < 0 || elapsedMs < 0) return null
    if (!snapshot.running) return snapshot.copy(deadlineElapsedMs = 0, savedElapsedMs = elapsedMs, bootCount = bootCount)
    val sameBoot = bootCount != null && snapshot.bootCount == bootCount && elapsedMs >= snapshot.savedElapsedMs
    if (!sameBoot) return snapshot.copy(running = false, deadlineElapsedMs = 0, savedElapsedMs = elapsedMs, bootCount = bootCount)
    if (snapshot.deadlineElapsedMs <= snapshot.savedElapsedMs ||
        snapshot.deadlineElapsedMs - snapshot.savedElapsedMs > MAX_TIMER_SECONDS * 1000) return null
    val remaining = ((snapshot.deadlineElapsedMs - elapsedMs).coerceAtLeast(0) + 999) / 1000
    if (remaining == 0L) {
        val next = if (snapshot.state == "WORK") "BREAK" else "WORK"
        return snapshot.copy(state = next, remainingSeconds = if (next == "WORK") snapshot.workSeconds else snapshot.breakSeconds,
            running = false, deadlineElapsedMs = 0, savedElapsedMs = elapsedMs, bootCount = bootCount)
    }
    return snapshot.copy(remainingSeconds = remaining, savedElapsedMs = elapsedMs)
}

@Singleton
class TimerStateStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("focus_timer", Context.MODE_PRIVATE)

    internal fun load(): TimerSnapshot? = runCatching {
        if (prefs.getInt("version", 0) != 1) return@runCatching null
        TimerSnapshot(
            owner = prefs.getString("owner", null) ?: return@runCatching null,
            state = prefs.getString("state", "")!!,
            remainingSeconds = prefs.getLong("remaining", 0),
            running = prefs.getBoolean("running", false),
            deadlineElapsedMs = prefs.getLong("deadline", 0),
            savedElapsedMs = prefs.getLong("saved_elapsed", -1),
            bootCount = prefs.getInt("boot", -1).takeIf { it >= 0 },
            activeTaskId = prefs.getString("task", null), sound = prefs.getString("sound", "NONE")!!,
            workSeconds = prefs.getLong("work", 1500), breakSeconds = prefs.getLong("break", 300)
        )
    }.getOrNull()

    internal fun save(snapshot: TimerSnapshot) {
        // Atomic synchronous checkpoint so a process restart cannot resurrect a preceding start/pause.
        check(prefs.edit().clear().putInt("version", 1).putString("owner", snapshot.owner)
            .putString("state", snapshot.state).putLong("remaining", snapshot.remainingSeconds)
            .putBoolean("running", snapshot.running).putLong("deadline", snapshot.deadlineElapsedMs)
            .putLong("saved_elapsed", snapshot.savedElapsedMs).putInt("boot", snapshot.bootCount ?: -1)
            .putString("task", snapshot.activeTaskId).putString("sound", snapshot.sound)
            .putLong("work", snapshot.workSeconds).putLong("break", snapshot.breakSeconds).commit())
    }

    internal fun clear() { check(prefs.edit().clear().commit()) }
}
