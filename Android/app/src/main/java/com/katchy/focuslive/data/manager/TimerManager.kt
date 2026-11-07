package com.katchy.focuslive.data.manager

import com.katchy.focuslive.data.repository.AppPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerManager @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val store: TimerStateStore,
    private val runtime: TimerRuntime
) {
    enum class TimerState { WORK, BREAK }
    enum class AmbientSound { NONE, RAIN, FIRE, CAFE, AMBIENT, CUSTOM, OCEAN }

    private var workDurationSeconds = appPreferencesRepository.workDuration.value.coerceIn(1, 120) * 60
    private var breakDurationSeconds = appPreferencesRepository.breakDuration.value.coerceIn(1, 120) * 60
    private val _timeLeft = MutableStateFlow(workDurationSeconds)
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning = _isTimerRunning.asStateFlow()
    private val _timerState = MutableStateFlow(TimerState.WORK)
    val timerState = _timerState.asStateFlow()
    private val _ambientSound = MutableStateFlow(AmbientSound.NONE)
    val ambientSound = _ambientSound.asStateFlow()
    private val _activeTaskId = MutableStateFlow<String?>(null)
    val activeTaskId = _activeTaskId.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timerJob: Job? = null
    private var sessionOwner = runtime.owner()
    private var deadlineElapsedMs = 0L
    private var lastCheckpointMs = 0L

    /** Called only when an active session reaches zero, never for skips or account changes. */
    var onTimerFinished: ((completedWorkSession: Boolean) -> Unit)? = null
    var onTickSound: ((Long) -> Unit)? = null

    init {
        val now = runtime.elapsedMs()
        val restored = store.load()?.let { restoreTimerSnapshot(it, sessionOwner, now, runtime.bootCount()) }
        if (restored != null) {
            workDurationSeconds = restored.workSeconds
            breakDurationSeconds = restored.breakSeconds
            _timerState.value = TimerState.valueOf(restored.state)
            _timeLeft.value = restored.remainingSeconds
            _activeTaskId.value = restored.activeTaskId
            _ambientSound.value = AmbientSound.valueOf(restored.sound)
            _isTimerRunning.value = restored.running
            deadlineElapsedMs = restored.deadlineElapsedMs
        }
        persist()
        if (_isTimerRunning.value) {
            launchTicker()
            runtime.startForegroundService()
        }
        scope.launch { runtime.ownerChanges.collect { synchronized(this@TimerManager) { ensureOwner() } } }
    }

    /** Guard every operation and every tick, so an auth callback delay cannot leak the preceding session. */
    private fun ensureOwner() {
        val owner = runtime.owner()
        if (owner == sessionOwner) return
        timerJob?.cancel()
        timerJob = null
        _isTimerRunning.value = false
        deadlineElapsedMs = 0
        sessionOwner = owner
        workDurationSeconds = appPreferencesRepository.workDuration.value.coerceIn(1, 120) * 60
        breakDurationSeconds = appPreferencesRepository.breakDuration.value.coerceIn(1, 120) * 60
        _timerState.value = TimerState.WORK
        _timeLeft.value = workDurationSeconds
        _activeTaskId.value = null
        _ambientSound.value = AmbientSound.NONE
        runCatching { store.clear() }
        runtime.stopForegroundService()
        persist()
    }

    private fun persist() {
        val now = runtime.elapsedMs()
        runCatching {
            store.save(TimerSnapshot(sessionOwner, _timerState.value.name, _timeLeft.value,
                _isTimerRunning.value, deadlineElapsedMs, now, runtime.bootCount(),
                _activeTaskId.value, _ambientSound.value.name, workDurationSeconds, breakDurationSeconds))
        }.onFailure { android.util.Log.e("TimerManager", "Unable to persist timer checkpoint", it) }
        lastCheckpointMs = now
    }

    @Synchronized
    fun updateDurations(workMinutes: Long, breakMinutes: Long) {
        ensureOwner()
        val previous = if (_timerState.value == TimerState.WORK) workDurationSeconds else breakDurationSeconds
        workDurationSeconds = workMinutes.coerceIn(1, 120) * 60
        breakDurationSeconds = breakMinutes.coerceIn(1, 120) * 60
        if (!_isTimerRunning.value && _timeLeft.value == previous) {
            _timeLeft.value = if (_timerState.value == TimerState.WORK) workDurationSeconds else breakDurationSeconds
        }
        persist()
    }

    @Synchronized
    fun toggleTimer() {
        ensureOwner()
        if (_isTimerRunning.value) pauseTimer() else startTimer()
    }

    @Synchronized
    fun startTimer() {
        ensureOwner()
        if (_isTimerRunning.value) {
            runtime.startForegroundService()
            return
        }
        deadlineElapsedMs = runtime.elapsedMs() + _timeLeft.value.coerceIn(1, MAX_TIMER_SECONDS) * 1000
        _isTimerRunning.value = true
        persist()
        launchTicker()
        // Centralized here: direct Agenda/Course starts and widget starts get the same foreground lifetime.
        runtime.startForegroundService()
    }

    private fun launchTicker() {
        timerJob?.cancel()
        val owner = sessionOwner
        timerJob = scope.launch {
            while (true) {
                delay(500)
                if (!tick(owner)) break
            }
        }
    }

    @Synchronized
    private fun tick(owner: String): Boolean {
        ensureOwner()
        if (sessionOwner != owner || !_isTimerRunning.value) return false
        val now = runtime.elapsedMs()
        val remaining = ((deadlineElapsedMs - now).coerceAtLeast(0) + 999) / 1000
        if (_timeLeft.value != remaining) {
            _timeLeft.value = remaining
            if (remaining in 1..5 && appPreferencesRepository.isPomodoroSoundEnabled.value) onTickSound?.invoke(remaining)
        }
        if (remaining == 0L) {
            handleTimerFinished(notifyCompletion = true)
            return false
        }
        if (now - lastCheckpointMs >= TIMER_CHECKPOINT_MS) persist()
        return true
    }

    private fun handleTimerFinished(notifyCompletion: Boolean) {
        timerJob?.cancel()
        timerJob = null
        _isTimerRunning.value = false
        deadlineElapsedMs = 0
        val completedWork = _timerState.value == TimerState.WORK
        _timerState.value = if (completedWork) TimerState.BREAK else TimerState.WORK
        _timeLeft.value = if (completedWork) breakDurationSeconds else workDurationSeconds
        persist()
        runtime.stopForegroundService()
        if (notifyCompletion) {
            runtime.notifyCompletion(completedWork)
            onTimerFinished?.invoke(completedWork)
        }
    }

    @Synchronized
    fun pauseTimer() {
        ensureOwner()
        if (_isTimerRunning.value) {
            _timeLeft.value = ((deadlineElapsedMs - runtime.elapsedMs()).coerceAtLeast(0) + 999) / 1000
            if (_timeLeft.value == 0L) { handleTimerFinished(notifyCompletion = true); return }
        }
        timerJob?.cancel()
        timerJob = null
        _isTimerRunning.value = false
        deadlineElapsedMs = 0
        persist()
        runtime.stopForegroundService()
    }

    @Synchronized
    fun resetTimer() {
        ensureOwner()
        timerJob?.cancel()
        timerJob = null
        _isTimerRunning.value = false
        deadlineElapsedMs = 0
        _timeLeft.value = if (_timerState.value == TimerState.WORK) workDurationSeconds else breakDurationSeconds
        persist()
        runtime.stopForegroundService()
    }

    /** Alarga la sesión actual sin tocar la duración configurada (botón «+5» de Enfoque). */
    @Synchronized
    fun addMinutes(minutes: Long) {
        ensureOwner()
        val extra = minutes.coerceIn(1, 60) * 60
        if (_isTimerRunning.value) {
            val remaining = ((deadlineElapsedMs - runtime.elapsedMs()).coerceAtLeast(0) + 999) / 1000
            val target = (remaining + extra).coerceAtMost(MAX_TIMER_SECONDS)
            deadlineElapsedMs = runtime.elapsedMs() + target * 1000
            _timeLeft.value = target
        } else {
            _timeLeft.value = (_timeLeft.value + extra).coerceAtMost(MAX_TIMER_SECONDS)
        }
        persist()
    }

    @Synchronized
    fun skipSession() {
        ensureOwner()
        handleTimerFinished(notifyCompletion = false)
    }

    @Synchronized
    fun setActiveTask(taskId: String?) {
        ensureOwner()
        _activeTaskId.value = taskId?.takeIf { it.isNotBlank() && it.length <= 256 }
        persist()
    }

    @Synchronized
    fun setAmbientSound(sound: AmbientSound) {
        ensureOwner()
        _ambientSound.value = sound
        persist()
    }

    /** Release process-local observers without changing the durable session checkpoint. */
    internal fun dispose() { scope.cancel() }
}
