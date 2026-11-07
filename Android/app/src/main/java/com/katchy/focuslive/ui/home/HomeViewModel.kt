package com.katchy.focuslive.ui.home

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.manager.TimerManager
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.data.repository.CourseRepository
import com.katchy.focuslive.data.repository.TaskRepository
import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.channels.Channel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val courseRepository: CourseRepository,
    private val spaceRepository: com.katchy.focuslive.data.repository.LearningSpaceRepository,
    private val authRepository: com.katchy.focuslive.data.repository.AuthRepository,
    private val networkMonitor: com.katchy.focuslive.ui.util.NetworkMonitor,
    private val timerManager: TimerManager,
    private val appPreferencesRepository: com.katchy.focuslive.data.repository.AppPreferencesRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    // --- Aggregated State ---
    data class HomeState(
        val tasks: List<Task> = emptyList(),
        val upcomingDeliveries: List<UpcomingDelivery> = emptyList(),
        val focusTasks: List<Task> = emptyList(),
        val connectionState: com.katchy.focuslive.ui.util.ConnectionState = com.katchy.focuslive.ui.util.ConnectionState.Available,
        val userName: String? = null
    )

    data class UpcomingDelivery(val task: Task, val courseName: String)

    private val _currentDate = MutableStateFlow(getTodayMidnight())
    val currentDate: StateFlow<Long> = _currentDate.asStateFlow()

    val homeState: StateFlow<HomeState> = combine(
        taskRepository.getAllTasks(),
        courseRepository.getCourses(),
        spaceRepository.getSpaces(),
        _currentDate,
        networkMonitor.connectionState
    ) { tasks, courses, spaces, today, connection ->
        buildHomeState(tasks, courses, spaces, today).copy(connectionState = connection)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())
    // --- Timer State (Delegated to TimerManager) ---
    val timeLeft: StateFlow<Long> = timerManager.timeLeft
    val isTimerRunning: StateFlow<Boolean> = timerManager.isTimerRunning
    val timerState: StateFlow<TimerManager.TimerState> = timerManager.timerState
    val activeTaskId: StateFlow<String?> = timerManager.activeTaskId
    val ambientSound: StateFlow<TimerManager.AmbientSound> = timerManager.ambientSound
    val customSoundUri = appPreferencesRepository.customSoundUri
    val userName: StateFlow<String> = appPreferencesRepository.userName

    // Timer Settings
    val workDuration = appPreferencesRepository.workDuration
    val breakDuration = appPreferencesRepository.breakDuration

    // --- Zen Mode (DND) ---
    private val _isZenModeEnabled = MutableStateFlow(false)
    val isZenModeEnabled: StateFlow<Boolean> = _isZenModeEnabled.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _sessionCompletedEvent = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val sessionCompletedEvent: SharedFlow<Boolean> = _sessionCompletedEvent.asSharedFlow()

    private var lastPostponedTaskSnapshot: Task? = null

    private val _zenModeEffect = Channel<ZenModeEffect>(Channel.BUFFERED)
    val zenModeEffect = _zenModeEffect.receiveAsFlow()

    sealed interface ZenModeEffect {
        data object RequestDndPermission : ZenModeEffect
    }
    // Lazy Vibrator
    private val vibrator: android.os.Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
    }

    // Optimized ToneGenerator
    private val toneGenerator: android.media.ToneGenerator by lazy {
        android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
    }
    private var ownedCompletionCallback: ((Boolean) -> Unit)? = null
    private var ownedTickCallback: ((Long) -> Unit)? = null

    init {
        timerManager.onTimerFinished = { completedWorkSession ->
            if (appPreferencesRepository.isPomodoroSoundEnabled.value) {
                com.katchy.focuslive.ui.util.SoundManager.playCompletionSound(context)
            }

            if (completedWorkSession) {
                vibrateCompletion()
            }
            _sessionCompletedEvent.tryEmit(completedWorkSession)
        }
        ownedCompletionCallback = timerManager.onTimerFinished

        timerManager.onTickSound = { secondsLeft ->
            try {
                if (secondsLeft <= 5) {
                    // Countdown Tock (Distinct, louder)
                    toneGenerator.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
                }
            } catch (e: Exception) {
                // Fallback
                com.katchy.focuslive.ui.util.SoundManager.playTickSound(context)
            }

            // Vibration Feedback for Tick (Short and crisp)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = android.os.VibrationEffect.createOneShot(30, 100)
                vibrator.vibrate(effect)
            } else {
                 @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        }
        ownedTickCallback = timerManager.onTickSound

        // Sync durations with TimerManager
        viewModelScope.launch {
            combine(workDuration, breakDuration) { work, breakTime ->
                Pair(work, breakTime)
            }.collect { (work, breakTime) ->
                timerManager.updateDurations(work, breakTime)
            }
        }

        // Handle Ambient Sound Playback
        viewModelScope.launch {
            // Need combine with customSoundUri as well
            combine(ambientSound, isTimerRunning, customSoundUri) { sound, running, customUri ->
                Triple(sound, running, customUri)
            }.collect { (sound, running, customUri) ->
                if (running && sound != TimerManager.AmbientSound.NONE) {
                    if (sound == TimerManager.AmbientSound.CUSTOM && customUri != null) {
                        try {
                            com.katchy.focuslive.ui.util.SoundManager.playFocusSoundFromUri(context, customUri)
                        } catch (e: Exception) {
                            com.katchy.focuslive.ui.util.SoundManager.stopFocusSound()
                        }
                    } else {
                        val resId = when(sound) {
                            TimerManager.AmbientSound.RAIN -> com.katchy.focuslive.R.raw.rain
                            TimerManager.AmbientSound.FIRE -> com.katchy.focuslive.R.raw.fire
                            TimerManager.AmbientSound.CAFE -> com.katchy.focuslive.R.raw.cafe
                            TimerManager.AmbientSound.AMBIENT -> com.katchy.focuslive.R.raw.ambient
                            TimerManager.AmbientSound.OCEAN -> com.katchy.focuslive.R.raw.ocean
                            else -> 0
                        }
                        com.katchy.focuslive.ui.util.SoundManager.playFocusSound(context, resId)
                    }
                } else {
                    com.katchy.focuslive.ui.util.SoundManager.stopFocusSound()
                }
            }
        }
        // Initial Full Sync on functionality load
        triggerSync()

        viewModelScope.launch {
            networkMonitor.connectionState.collect { connectionState ->
                if (connectionState == com.katchy.focuslive.ui.util.ConnectionState.Available) {
                    if (authRepository.currentUser?.uid != null) {
                        triggerSync()
                    }
                }
            }
        }

        // Midnight Day Ticker: Automatically advance currentDate when midnight strikes
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val now = System.currentTimeMillis()
                val nextMidnight = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 1)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                val delayMs = (nextMidnight - now).coerceAtLeast(1000L)
                kotlinx.coroutines.delay(delayMs)
                val newToday = getTodayMidnight()
                if (_currentDate.value != newToday) {
                    _currentDate.value = newToday
                }
            }
        }
    }

    private fun triggerSync() {
        val owner = authRepository.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return
        com.katchy.focuslive.data.worker.SyncScheduler.enqueue(context, owner)
    }

    fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                triggerSync()
                taskRepository.syncPendingChanges()
                taskRepository.refreshTasks()
            } catch (_: Exception) {
            } finally {
                kotlinx.coroutines.delay(320)
                _isRefreshing.value = false
            }
        }
    }
    fun setWorkDuration(minutes: Long) {
        appPreferencesRepository.setWorkDuration(minutes)
    }

    fun setBreakDuration(minutes: Long) {
        appPreferencesRepository.setBreakDuration(minutes)
    }

    // --- Timer Logic ---
    fun toggleTimer() {
        timerManager.toggleTimer()
    }

    fun resetTimer() {
        timerManager.resetTimer()
    }

    fun skipSession() {
        timerManager.skipSession()
    }

    fun addFiveMinutes() {
        timerManager.addMinutes(5)
    }

    fun toggleSubtask(task: Task, subtaskId: String) {
        viewModelScope.launch {
            runCatching { taskRepository.toggleSubtask(task.id, subtaskId, _currentDate.value).getOrThrow() }
                .onFailure { _taskCreationError.value = "No se pudo actualizar la subtarea." }
        }
    }

    fun setActiveTask(taskId: String?) {
        timerManager.setActiveTask(taskId)
    }

    fun setAmbientSound(sound: TimerManager.AmbientSound) {
        timerManager.setAmbientSound(sound)
    }

    fun setCustomSoundUri(uri: String?) {
        appPreferencesRepository.setCustomSoundUri(uri)
    }

    // --- Task Logic ---
    private val lastTaskToggleTime = mutableMapOf<String, Long>()
    private val TOGGLE_DEBOUNCE_MS = 100L
    fun toggleTask(task: Task) {
        val now = System.currentTimeMillis()
        val lastTime = lastTaskToggleTime[task.id] ?: 0L
        if (now - lastTime < TOGGLE_DEBOUNCE_MS) return
        lastTaskToggleTime[task.id] = now
        _taskCreationError.value = null

        viewModelScope.launch {
            try {
                taskRepository.toggleTaskCompletion(task.id, _currentDate.value).getOrThrow()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _taskCreationError.value = "No se pudo actualizar la tarea. Intenta nuevamente."
                android.util.Log.e("HomeViewModel", "Error in toggleTask coroutine", e)
            }
        }
    }

    private val _isCreatingTask = MutableStateFlow(false)
    val isCreatingTask = _isCreatingTask.asStateFlow()
    private val _taskCreationError = MutableStateFlow<String?>(null)
    val taskCreationError = _taskCreationError.asStateFlow()

    fun clearTaskCreationError() { _taskCreationError.value = null }

    fun postponeTask(task: Task, onPostponed: () -> Unit = {}) {
        _taskCreationError.value = null
        lastPostponedTaskSnapshot = task
        val postponed = postponeTaskToTomorrow(task, _currentDate.value)
        viewModelScope.launch {
            try {
                taskRepository.updateTask(postponed).getOrThrow()
                onPostponed()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _taskCreationError.value = "No se pudo posponer la tarea."
            }
        }
    }

    fun undoPostponeTask() {
        val snapshot = lastPostponedTaskSnapshot ?: return
        lastPostponedTaskSnapshot = null
        _taskCreationError.value = null
        viewModelScope.launch {
            try {
                val currentRev = taskRepository.getAllTasks().firstOrNull()
                    ?.firstOrNull { it.id == snapshot.id }?.revision
                    ?: (snapshot.revision + 1)
                taskRepository.updateTask(snapshot.copy(revision = currentRev)).getOrThrow()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _taskCreationError.value = "No se pudo deshacer el cambio."
            }
        }
    }

    fun addTask(title: String, priority: String = "MEDIUM", time: String? = null, onSaved: () -> Unit = {}) {
        if (title.isBlank() || _isCreatingTask.value) return
        _isCreatingTask.value = true
        _taskCreationError.value = null
        viewModelScope.launch {
            try {
                val today = _currentDate.value
                taskRepository.addTask(Task(
                    title = title.trim(), kind = "AGENDA", timestamp = today, dueDate = today,
                    priority = priority, time = time, category = "General"
                )).getOrThrow()
                // Repository owns reminders, sync, and widget updates after persistence.
                onSaved()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _taskCreationError.value = "No se pudo guardar la tarea. Intenta nuevamente."
            } finally {
                _isCreatingTask.value = false
            }
        }
    }
    fun toggleZenMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                viewModelScope.launch { _zenModeEffect.send(ZenModeEffect.RequestDndPermission) }
                return
            }
        }

        val newValue = !_isZenModeEnabled.value
        _isZenModeEnabled.value = newValue

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (newValue) {
                // Total Concentration: Allow only Alarms + Priority (user can configure priority)
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } else {
                // Normal: Allow All
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        }
    }

    fun onResume() {
        val today = getTodayMidnight()
        if (_currentDate.value != today) {
            _currentDate.value = today
        }
        // Sync state with actual system status if user changed it outside app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                val currentFilter = notificationManager.currentInterruptionFilter
                _isZenModeEnabled.value = currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            } else {
                _isZenModeEnabled.value = false
            }
        }
    }

    private fun vibrateCompletion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = android.os.VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 200), -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    private fun getTodayMidnight(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    override fun onCleared() {
        super.onCleared()
        if (timerManager.onTimerFinished === ownedCompletionCallback) timerManager.onTimerFinished = null
        if (timerManager.onTickSound === ownedTickCallback) timerManager.onTickSound = null
        // Release ToneGenerator resources
        try {
            toneGenerator.release()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
