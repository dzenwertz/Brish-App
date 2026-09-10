package com.katchy.focuslive.data.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerManager @Inject constructor(
    private val appPreferencesRepository: com.katchy.focuslive.data.repository.AppPreferencesRepository
) {

    enum class TimerState {
        WORK, BREAK
    }

    private val _timeLeft = MutableStateFlow(25 * 60L) // 25 minutes default
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.WORK)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    enum class AmbientSound {
        NONE, RAIN, FIRE, CAFE, AMBIENT, CUSTOM, OCEAN
    }

    private val _ambientSound = MutableStateFlow(AmbientSound.NONE)
    val ambientSound: StateFlow<AmbientSound> = _ambientSound.asStateFlow()

    private val _activeTaskId = MutableStateFlow<String?>(null)
    val activeTaskId: StateFlow<String?> = _activeTaskId.asStateFlow()

    // Configuration
    private var workDurationSeconds = 25 * 60L
    private var breakDurationSeconds = 5 * 60L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null

    /** Fired when a session actually reaches 0. Param is true if the finished session was WORK. */
    var onTimerFinished: ((completedWorkSession: Boolean) -> Unit)? = null
    var onTickSound: ((Long) -> Unit)? = null // Callback with seconds left

    fun updateDurations(workMinutes: Long, breakMinutes: Long) {
        val wasWork = workDurationSeconds
        val wasBreak = breakDurationSeconds
        
        workDurationSeconds = workMinutes * 60L
        breakDurationSeconds = breakMinutes * 60L
        
        // If timer is not running, reset to reflect new settings immediately if visible
        if (!_isTimerRunning.value) {
            // Only reset if the current duration differs or just safe to reset
             if (_timerState.value == TimerState.WORK && _timeLeft.value == wasWork) {
                 _timeLeft.value = workDurationSeconds
             } else if (_timerState.value == TimerState.BREAK && _timeLeft.value == wasBreak) {
                 _timeLeft.value = breakDurationSeconds
             }
        }
    }

    fun toggleTimer() {
        if (_isTimerRunning.value) pauseTimer() else startTimer()
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        
        _isTimerRunning.value = true
        val targetEndTime = android.os.SystemClock.elapsedRealtime() + _timeLeft.value * 1000L
        
        timerJob = scope.launch {
            while (_isTimerRunning.value) {
                delay(500) // check frequently to keep UI responsive
                val currentRealtime = android.os.SystemClock.elapsedRealtime()
                val remainingSeconds = ((targetEndTime - currentRealtime + 999L) / 1000L).coerceAtLeast(0L)
                
                if (remainingSeconds != _timeLeft.value) {
                    _timeLeft.value = remainingSeconds
                    // Play tick sound for last 5 seconds if enabled
                    if (_timeLeft.value in 1..5 && appPreferencesRepository.isPomodoroSoundEnabled.value) {
                        onTickSound?.invoke(_timeLeft.value)
                    }
                }
                
                if (remainingSeconds <= 0L) {
                    break
                }
            }
            if (_timeLeft.value == 0L) {
                handleTimerFinished()
            }
        }
    }

    private fun handleTimerFinished(awardRewards: Boolean = true) {
        _isTimerRunning.value = false
        val finishedWork = _timerState.value == TimerState.WORK

        if (awardRewards) {
            onTimerFinished?.invoke(finishedWork)
        }

        if (finishedWork) {
            _timerState.value = TimerState.BREAK
            _timeLeft.value = breakDurationSeconds
        } else {
            _timerState.value = TimerState.WORK
            _timeLeft.value = workDurationSeconds
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        // Reset to full duration of CURRENT state
        _timeLeft.value = if (_timerState.value == TimerState.WORK) workDurationSeconds else breakDurationSeconds
    }
    
    fun skipSession() {
        pauseTimer()
        handleTimerFinished(awardRewards = false)
    }

    fun setActiveTask(taskId: String?) {
        _activeTaskId.value = taskId
    }

    fun setAmbientSound(sound: AmbientSound) {
        _ambientSound.value = sound
    }
}
