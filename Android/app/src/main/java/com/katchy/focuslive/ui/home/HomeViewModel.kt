package com.katchy.focuslive.ui.home

import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.manager.TimerManager
import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.data.repository.CourseRepository
import com.katchy.focuslive.data.repository.HabitRepository
import com.katchy.focuslive.data.repository.TaskRepository
import com.katchy.focuslive.util.HabitUtils
import com.katchy.focuslive.service.TimerService
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
    private val habitRepository: HabitRepository,
    private val financeRepository: com.katchy.focuslive.data.repository.FinanceRepository,
    private val authRepository: com.katchy.focuslive.data.repository.AuthRepository,
    private val noteRepository: com.katchy.focuslive.data.repository.NoteRepository,
    private val mascotRepository: com.katchy.focuslive.data.repository.MascotRepository,
    private val networkMonitor: com.katchy.focuslive.ui.util.NetworkMonitor,
    private val timerManager: TimerManager,
    private val appPreferencesRepository: com.katchy.focuslive.data.repository.AppPreferencesRepository,
    private val userStatsRepository: com.katchy.focuslive.data.repository.UserStatsRepository,
    private val widgetManager: com.katchy.focuslive.data.manager.WidgetManager,
    private val notificationSchedulerProvider: javax.inject.Provider<com.katchy.focuslive.scheduler.NotificationScheduler>,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    // --- Aggregated State ---
    data class HomeState(
        val tasks: List<Task> = emptyList(),
        val todayHabits: List<Habit> = emptyList(),
        val upcomingDeliveries: List<UpcomingDelivery> = emptyList(),
        val connectionState: com.katchy.focuslive.ui.util.ConnectionState = com.katchy.focuslive.ui.util.ConnectionState.Available,
        val userName: String? = null
    )

    data class UpcomingDelivery(val task: Task, val courseName: String)

    private val _currentDate = MutableStateFlow(getTodayMidnight())
    val currentDate: StateFlow<Long> = _currentDate.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val _tasks = _currentDate.flatMapLatest { today ->
        taskRepository.getTasksForDate(today).map { list ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = today }
            val selectedDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)

            list.filter { task ->
                val taskCal = java.util.Calendar.getInstance().apply { timeInMillis = task.timestamp }
                taskCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                taskCal.set(java.util.Calendar.MINUTE, 0)
                taskCal.set(java.util.Calendar.SECOND, 0)
                taskCal.set(java.util.Calendar.MILLISECOND, 0)
                val taskStartOfDay = taskCal.timeInMillis

                if (task.repeatMode == "NONE") {
                    val isCompletedToday = task.completedDates.any { HabitUtils.isSameDay(it, today) }
                    val isToday = taskStartOfDay == today
                    // Include today's tasks, tasks completed today, and uncompleted overdue tasks up to 14 days old
                    val isRecentOverdue = taskStartOfDay < today && !task.isCompleted && (today - taskStartOfDay <= 14L * 24 * 60 * 60 * 1000L)
                    isToday || isCompletedToday || isRecentOverdue
                } else {
                    when (task.repeatMode) {
                        "DAILY" -> today >= taskStartOfDay
                        "WEEKLY", "CUSTOM" -> {
                            val isAfterStart = today >= taskStartOfDay
                            val isCorrectDay = task.repeatDays.isNotEmpty() && task.repeatDays.contains(selectedDayOfWeek)
                            isAfterStart && isCorrectDay
                        }
                        else -> today == taskStartOfDay
                    }
                }
            }.map { task ->
                if (task.repeatMode != "NONE") {
                    val isCompletedToday = task.completedDates.any { HabitUtils.isSameDay(it, today) }
                    val updatedSubtasks = task.subtasks.map { subtask ->
                        val isSubtaskCompletedToday = subtask.completedDates.any { HabitUtils.isSameDay(it, today) }
                        subtask.copy(isCompleted = isSubtaskCompletedToday)
                    }
                    task.copy(isCompleted = isCompletedToday, subtasks = updatedSubtasks)
                } else {
                    val isCompletedToday = task.isCompleted || task.completedDates.any { HabitUtils.isSameDay(it, today) }
                    task.copy(isCompleted = isCompletedToday)
                }
            }.sortedWith(
                compareBy<Task> { it.isCompleted } // Incomplete first, completed at the bottom
                    .thenBy {
                        val isOverdue = (it.repeatMode == "NONE" && it.timestamp < today && !it.isCompleted)
                        if (isOverdue) 1 else 0 // Today's pending first, overdue pending second
                    }
                    .thenBy { it.time ?: "23:59" } // Chronological order by time
            )
        }
    }
    private val _habits = habitRepository.getHabits()
    private val _connection = networkMonitor.connectionState
    private val _user = authRepository.authStateFlow

    private val _todayHabits = combine(_habits, _currentDate) { habits, today ->
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = today }
        val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
        habits.filter { it.daysOfWeek.isEmpty() || it.daysOfWeek.contains(dayOfWeek) }
    }.distinctUntilChanged()

    private val _userNameFlow = _user.map { user ->
        user?.displayName ?: user?.email?.substringBefore("@")
    }.distinctUntilChanged()

    private val _upcomingDeliveries = combine(courseRepository.getCourses(), taskRepository.getAllTasks()) { courses, tasks ->
        val courseNames = courses.associate { it.id to it.name }
        tasks.filter { !it.isCompleted && it.courseId != null && courseNames.containsKey(it.courseId) }
            .sortedBy { it.dueDate }
            .take(3)
            .map { UpcomingDelivery(it, courseNames.getValue(it.courseId!!)) }
    }

    val homeState: StateFlow<HomeState> = combine(
        _tasks,
        _todayHabits,
        _upcomingDeliveries,
        _connection,
        _userNameFlow
    ) { tasks, todayHabits, upcomingDeliveries, connection, name ->
        HomeState(
            tasks = tasks,
            todayHabits = todayHabits,
            upcomingDeliveries = upcomingDeliveries,
            connectionState = connection,
            userName = name
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    // --- Timer State (Delegated to TimerManager) ---
    val timeLeft: StateFlow<Long> = timerManager.timeLeft
    val isTimerRunning: StateFlow<Boolean> = timerManager.isTimerRunning
    val timerState: StateFlow<TimerManager.TimerState> = timerManager.timerState
    val activeTaskId: StateFlow<String?> = timerManager.activeTaskId
    val ambientSound: StateFlow<TimerManager.AmbientSound> = timerManager.ambientSound
    val customSoundUri = appPreferencesRepository.customSoundUri
    
    // Timer Settings
    val workDuration = appPreferencesRepository.workDuration
    val breakDuration = appPreferencesRepository.breakDuration
    
    // Module Visibility
    val isPlannerEnabled = appPreferencesRepository.isPlannerEnabled
    val isNotesEnabled = appPreferencesRepository.isNotesEnabled
    val isFinanceEnabled = appPreferencesRepository.isFinanceEnabled
    val isHabitsEnabled = appPreferencesRepository.isHabitsEnabled

    val userStats = userStatsRepository.userStats
    val pendingDailyReward = userStatsRepository.pendingDailyReward

    fun dismissDailyReward() {
        userStatsRepository.dismissPendingDailyReward()
    }

    // --- Zen Mode (DND) ---
    private val _isZenModeEnabled = MutableStateFlow(false)
    val isZenModeEnabled: StateFlow<Boolean> = _isZenModeEnabled.asStateFlow()

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

    init {
        timerManager.onTimerFinished = { completedWorkSession ->
            if (appPreferencesRepository.isPomodoroSoundEnabled.value) {
                com.katchy.focuslive.ui.util.SoundManager.playCompletionSound(context)
            }
            com.katchy.focuslive.ui.util.NotificationHelper.showPomodoroFinishedNotification(context)

            if (completedWorkSession) {
                val multiplier = userStatsRepository.getStreakMultiplier()
                val xpAwarded = (25 * multiplier).toInt()
                userStatsRepository.addXp(xpAwarded)
                mascotRepository.addXpToSelectedMascot(xpAwarded)
                userStatsRepository.addFocusMinutes(appPreferencesRepository.workDuration.value.toInt())
                userStatsRepository.addCoins((15 * multiplier).toInt())
                vibrateCompletion()
            }
        }
        
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
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
        val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.katchy.focuslive.data.worker.SyncWorker>()
            .setConstraints(constraints)
            .build()
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncWork",
            androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncRequest
        )
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
        val intent = Intent(context, TimerService::class.java)
        
        // Start foreground service if timer is running to ensure it keeps going in background
        if (timerManager.isTimerRunning.value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun resetTimer() {
        timerManager.resetTimer()
    }

    fun skipSession() {
        timerManager.skipSession()
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
    fun isHabitDoneToday(habit: Habit): Boolean {
        return habit.completedDates.any { HabitUtils.isSameDay(it, _currentDate.value) }
    }

    private val lastHabitToggleTime = mutableMapOf<String, Long>()
    private val lastTaskToggleTime = mutableMapOf<String, Long>()
    private val TOGGLE_DEBOUNCE_MS = 100L

    fun toggleHabitToday(habit: Habit) {
        val now = System.currentTimeMillis()
        val lastTime = lastHabitToggleTime[habit.id] ?: 0L
        if (now - lastTime < TOGGLE_DEBOUNCE_MS) return
        lastHabitToggleTime[habit.id] = now

        viewModelScope.launch {
            val today = _currentDate.value
            val dates = habit.completedDates.toMutableList()
            val existing = dates.find { HabitUtils.isSameDay(it, today) }
            val isCompleting = (existing == null)

            val multiplier = userStatsRepository.getStreakMultiplier()
            val xpAwarded = (15 * multiplier).toInt()
            val coinsAwarded = (8 * multiplier).toInt()

            if (isCompleting) {
                dates.add(System.currentTimeMillis())
                userStatsRepository.addXp(xpAwarded)
                mascotRepository.addXpToSelectedMascot(xpAwarded)
                userStatsRepository.addCoins(coinsAwarded)
                userStatsRepository.incrementHabits()
                com.katchy.focuslive.ui.util.MascotEventBus.emit(com.katchy.focuslive.ui.util.MascotEvent.HABIT_DONE)
            } else {
                dates.remove(existing)
                userStatsRepository.deductXp(xpAwarded)
                mascotRepository.deductXpFromSelectedMascot(xpAwarded)
                userStatsRepository.deductCoins(coinsAwarded)
                userStatsRepository.decrementHabits()
            }

            val newStreak = HabitUtils.calculateStreak(dates)
            habitRepository.updateHabit(
                habit.copy(
                    completedDates = dates,
                    currentStreak = newStreak
                )
            )
        }
    }

    fun toggleTask(task: Task) {
        val now = System.currentTimeMillis()
        val lastTime = lastTaskToggleTime[task.id] ?: 0L
        if (now - lastTime < TOGGLE_DEBOUNCE_MS) return
        lastTaskToggleTime[task.id] = now

        viewModelScope.launch {
            try {
                val result = taskRepository.toggleTaskCompletion(task.id, _currentDate.value)
                result.onSuccess { isCompleted ->
                    if (isCompleted) {
                        com.katchy.focuslive.ui.util.MascotEventBus.emit(com.katchy.focuslive.ui.util.MascotEvent.TASK_DONE)
                    }
                }.onFailure { e ->
                    android.util.Log.e("HomeViewModel", "Failed to toggle task completion", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error in toggleTask coroutine", e)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task.id)
            updateTasksWidget()
        }
    }

    fun addTask(title: String, priority: String = "MEDIUM", time: String? = null) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val today = _currentDate.value
            val newTask = Task(
                title = title.trim(),
                timestamp = today,
                dueDate = today,
                priority = priority,
                time = time,
                isCompleted = false,
                category = "General"
            )
            taskRepository.addTask(newTask)
            if (time != null) {
                try {
                    notificationSchedulerProvider.get().scheduleTaskReminder(newTask)
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Failed to schedule task reminder", e)
                }
            }
            updateTasksWidget()
        }
    }
    
    private suspend fun updateTasksWidget() {
        widgetManager.updateTasksWidget()
    }

    fun setMascot(mascot: com.katchy.focuslive.data.model.MascotType) {
        mascotRepository.setMascot(mascot)
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
        timerManager.onTimerFinished = null
        timerManager.onTickSound = null
        // Release ToneGenerator resources
        try {
            toneGenerator.release()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
