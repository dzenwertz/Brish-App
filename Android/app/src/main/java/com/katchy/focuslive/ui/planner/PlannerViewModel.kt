package com.katchy.focuslive.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.data.model.SubtaskItem
import com.katchy.focuslive.data.service.AIService
import com.katchy.focuslive.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import com.katchy.focuslive.util.HabitUtils
import javax.inject.Inject

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PlannerViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val mascotRepository: com.katchy.focuslive.data.repository.MascotRepository,
    private val appPreferencesRepository: com.katchy.focuslive.data.repository.AppPreferencesRepository,
    private val notificationScheduler: com.katchy.focuslive.scheduler.NotificationScheduler,
    private val userStatsRepository: com.katchy.focuslive.data.repository.UserStatsRepository,
    private val aiService: AIService
) : ViewModel() {

    init {
        viewModelScope.launch {
            try {
                taskRepository.refreshTasks()
            } catch (e: Exception) {
                // Ignore offline error
            }
        }
    }

    val selectedMascot = mascotRepository.selectedMascot
    val isPremium = userStatsRepository.isPremium

    // AI Brain Dump States
    private val _isBrainDumpLoading = MutableStateFlow(false)
    val isBrainDumpLoading: StateFlow<Boolean> = _isBrainDumpLoading.asStateFlow()

    private val _brainDumpError = MutableStateFlow<String?>(null)
    val brainDumpError: StateFlow<String?> = _brainDumpError.asStateFlow()

    // AI Prioritization States
    private val _isPrioritizing = MutableStateFlow(false)
    val isPrioritizing: StateFlow<Boolean> = _isPrioritizing.asStateFlow()

    // AI Subtasks Loading States (map of task ID to loading boolean)
    private val _subtaskLoadingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val subtaskLoadingStates: StateFlow<Map<String, Boolean>> = _subtaskLoadingStates.asStateFlow()

    fun generateBrainDump(text: String, onSuccess: () -> Unit) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isBrainDumpLoading.value = true
            _brainDumpError.value = null
            try {
                val extractedTasks = aiService.generateBrainDumpTasks(text)
                if (extractedTasks.isNotEmpty()) {
                    extractedTasks.forEach { dumpTask ->
                        val subtaskItems = dumpTask.description.let { desc ->
                            if (desc.isNotBlank()) listOf(SubtaskItem(title = desc)) else emptyList()
                        }
                        val taskDate = dumpTask.dueDateTimestamp ?: _selectedDate.value
                        val newTask = Task(
                            title = dumpTask.title,
                            timestamp = getStartOfDay(taskDate),
                            dueDate = getStartOfDay(taskDate),
                            isCompleted = false,
                            time = dumpTask.time,
                            subtasks = subtaskItems,
                            priority = dumpTask.priority,
                            category = "General"
                        )
                        taskRepository.addTask(newTask)
                        if (newTask.time != null) {
                            notificationScheduler.scheduleTaskReminder(newTask)
                        }
                    }
                    onSuccess()
                } else {
                    _brainDumpError.value = "No se pudieron extraer tareas. Intenta escribir de otra forma."
                }
            } catch (e: Exception) {
                _brainDumpError.value = "Error al procesar: ${e.localizedMessage}"
            } finally {
                _isBrainDumpLoading.value = false
            }
        }
    }

    fun prioritizeTasksWithAI() {
        viewModelScope.launch {
            val currentTasks = tasksForSelectedDate.value
            if (currentTasks.isEmpty()) return@launch
            _isPrioritizing.value = true
            try {
                val prioritized = aiService.prioritizeTasks(currentTasks)
                prioritized.forEach { prioritizedTask ->
                    val taskToUpdate = currentTasks.find { it.id == prioritizedTask.taskId }
                    if (taskToUpdate != null) {
                        taskRepository.updateTask(taskToUpdate.copy(priority = prioritizedTask.newPriority))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PlannerViewModel", "Exception caught", e)
            } finally {
                _isPrioritizing.value = false
            }
        }
    }

    fun generateSubtasksWithAI(task: Task) {
        viewModelScope.launch {
            _subtaskLoadingStates.update { it + (task.id to true) }
            try {
                val steps = aiService.generateSubtasks(task.title)
                if (steps.isNotEmpty()) {
                    val newSubtasks = steps.map { SubtaskItem(title = it) }
                    taskRepository.updateTask(task.copy(subtasks = task.subtasks + newSubtasks))
                }
            } catch (e: Exception) {
                android.util.Log.e("PlannerViewModel", "Exception caught", e)
            } finally {
                _subtaskLoadingStates.update { it + (task.id to false) }
            }
        }
    }

    suspend fun getAISubtasks(title: String): List<String> {
        if (title.isBlank()) return emptyList()
        return try {
            aiService.generateSubtasks(title)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getStartOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply { 
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private var lastKnownDeviceDay = getStartOfDay(System.currentTimeMillis())
    private val _selectedDate = MutableStateFlow(lastKnownDeviceDay)
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    val tasksForSelectedDate: StateFlow<List<Task>> = _selectedDate
        .flatMapLatest { date ->
            taskRepository.getTasksForDate(date).map { tasks ->
                val calendar = Calendar.getInstance()
                val selectedDayOfWeek = calendar.let {
                    it.timeInMillis = date
                    it.get(Calendar.DAY_OF_WEEK)
                }

                tasks.filter { task ->
                    // Use a fresh calendar specifically for this task to avoid any state reuse issues
                    val taskCal = Calendar.getInstance()
                    taskCal.timeInMillis = task.timestamp
                    taskCal.set(Calendar.HOUR_OF_DAY, 0)
                    taskCal.set(Calendar.MINUTE, 0)
                    taskCal.set(Calendar.SECOND, 0)
                    taskCal.set(Calendar.MILLISECOND, 0)
                    val taskStartOfDay = taskCal.timeInMillis

                    when (task.repeatMode) {
                        "DAILY" -> date >= taskStartOfDay
                        "WEEKLY", "CUSTOM" -> {
                            val isAfterStart = date >= taskStartOfDay
                            // Robust check: Ensure repeatDays is not empty and verify content
                            val isCorrectDay = task.repeatDays.isNotEmpty() && task.repeatDays.contains(selectedDayOfWeek)
                            isAfterStart && isCorrectDay
                        }
                        else -> {
                            // "NONE" or others
                            date == taskStartOfDay
                        }
                    }
                }.map { task ->
                    // Override isCompleted for recurring tasks based on completedDates
                    if (task.repeatMode != "NONE") {
                        val isCompletedToday = task.completedDates.any { HabitUtils.isSameDay(it, date) }
                        
                        // Handle SUBTASKS for recurring tasks
                        val updatedSubtasks = task.subtasks.map { subtask ->
                            val isSubtaskCompletedToday = subtask.completedDates.any { HabitUtils.isSameDay(it, date) }
                            subtask.copy(isCompleted = isSubtaskCompletedToday)
                        }
                        
                        task.copy(isCompleted = isCompletedToday, subtasks = updatedSubtasks)
                    } else {
                        val isCompletedOnDate = task.isCompleted || task.completedDates.any { HabitUtils.isSameDay(it, date) }
                        task.copy(isCompleted = isCompletedOnDate)
                    }
                }.sortedBy { it.time ?: "23:59" } 
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(timestamp: Long) {
        _selectedDate.value = getStartOfDay(timestamp)
    }

    /**
     * Refreshes "today" from the device clock after returning to the app or
     * changing its time zone. A date the user selected manually is preserved.
     */
    fun syncWithDeviceDate() {
        val currentDeviceDay = getStartOfDay(System.currentTimeMillis())
        if (_selectedDate.value == lastKnownDeviceDay) {
            _selectedDate.value = currentDeviceDay
        }
        lastKnownDeviceDay = currentDeviceDay
    }

    // Debounce map
    private val lastToggleTime = mutableMapOf<String, Long>()
    private val DEBOUNCE_DELAY = 100L

    fun addTask(
        title: String, 
        repeatMode: String = "NONE", 
        repeatDays: List<Int> = emptyList(),
        time: String? = null,
        endTime: String? = null,
        category: String = "Personal", 
        priority: String = "MEDIUM",
        initialSubtasks: List<String> = emptyList()
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val subtaskItems = initialSubtasks.map { com.katchy.focuslive.data.model.SubtaskItem(title = it) }
            val newTask = Task(
                title = title,
                timestamp = _selectedDate.value, // Already start of day
                dueDate = _selectedDate.value,
                isCompleted = false,
                repeatMode = repeatMode,
                repeatDays = repeatDays,
                time = time,
                endTime = endTime,
                category = category,
                priority = priority,
                subtasks = subtaskItems
            )
            taskRepository.addTask(newTask)
            
            // Schedule notification if time is set
            if (time != null) {
                notificationScheduler.scheduleTaskReminder(newTask)
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
            
            // Reschedule notification
            notificationScheduler.cancelTaskReminder(task.id)
            if (task.time != null && !task.isCompleted) {
                notificationScheduler.scheduleTaskReminder(task)
            }
        }
    }

    fun toggleTask(task: Task) {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastToggleTime[task.id] ?: 0L
        if (currentTime - lastTime < DEBOUNCE_DELAY) return
        lastToggleTime[task.id] = currentTime

        viewModelScope.launch {
            try {
                val result = taskRepository.toggleTaskCompletion(task.id, _selectedDate.value)
                result.onSuccess { isCompleted ->
                    if (isCompleted) {
                        com.katchy.focuslive.ui.util.MascotEventBus.emit(com.katchy.focuslive.ui.util.MascotEvent.TASK_DONE)
                    }
                }.onFailure { e ->
                    android.util.Log.e("PlannerViewModel", "Failed to toggle task completion", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("PlannerViewModel", "Error in toggleTask coroutine", e)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task.id)
            notificationScheduler.cancelTaskReminder(task.id)
        }
    }

    fun restoreTask(task: Task) {
        viewModelScope.launch {
            taskRepository.addTask(task)
            if (task.time != null && !task.isCompleted) {
                notificationScheduler.scheduleTaskReminder(task)
            }
        }
    }


    fun toggleSubtask(task: Task, subtaskId: String) {
        val currentTime = System.currentTimeMillis()
        // Use composite key for safety
        val key = "${task.id}_$subtaskId"
        val lastTime = lastToggleTime[key] ?: 0L
        if (currentTime - lastTime < DEBOUNCE_DELAY) return
        lastToggleTime[key] = currentTime

        viewModelScope.launch {
            try {
                taskRepository.toggleSubtask(task.id, subtaskId, _selectedDate.value)
            } catch (e: Exception) {
                android.util.Log.e("PlannerViewModel", "Error in toggleSubtask", e)
            }
        }
    }

    fun addSubtask(task: Task, subtaskTitle: String) {
        if (subtaskTitle.isBlank()) return
        viewModelScope.launch {
            val newSubtask = com.katchy.focuslive.data.model.SubtaskItem(title = subtaskTitle)
            val updatedTask = task.copy(subtasks = task.subtasks + newSubtask)
            taskRepository.updateTask(updatedTask)
        }
    }

    fun updateSubtask(task: Task, subtaskId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            val updatedSubtasks = task.subtasks.map {
                if (it.id == subtaskId) it.copy(title = newTitle) else it
            }
            taskRepository.updateTask(task.copy(subtasks = updatedSubtasks))
        }
    }

    fun deleteSubtask(task: Task, subtaskId: String) {
         viewModelScope.launch {
            val updatedSubtasks = task.subtasks.filter { it.id != subtaskId }
            taskRepository.updateTask(task.copy(subtasks = updatedSubtasks))
        }
    }


}
