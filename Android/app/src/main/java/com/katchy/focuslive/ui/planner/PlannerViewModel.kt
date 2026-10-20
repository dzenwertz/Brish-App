package com.katchy.focuslive.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.data.model.SubtaskItem
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
    private val notificationScheduler: com.katchy.focuslive.scheduler.NotificationScheduler,
    private val timerManager: com.katchy.focuslive.data.manager.TimerManager,
    noteRepository: com.katchy.focuslive.data.repository.NoteRepository
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

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    fun clearError() { _error.value = null }

    private fun write(onSuccess: () -> Unit = {}, action: suspend () -> Unit) {
        if (_isSaving.value) return
        _isSaving.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                action()
                onSuccess()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "No se pudo guardar el cambio. Intenta nuevamente."
            } finally {
                _isSaving.value = false
            }
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
        .flatMapLatest { date -> taskRepository.getAgendaTasks().map { tasks -> tasksOnDay(tasks, date) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Notas enlazadas al día elegido. */
    val notesForSelectedDate: StateFlow<List<com.katchy.focuslive.data.model.Note>> = combine(_selectedDate, noteRepository.getNotes()) { date, notes ->
        notes.filter { it.deletedAt == null && it.linkedDate?.let { d -> startOfDay(d) } == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Las tareas de cada día de la semana del día elegido (lunes a domingo). */
    val weekTasks: StateFlow<List<Pair<Long, List<Task>>>> = _selectedDate
        .map { weekOf(it) }
        .distinctUntilChanged()
        .flatMapLatest { week -> taskRepository.getAgendaTasks().map { tasks -> week.map { day -> day to tasksOnDay(tasks, day) } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Mueve un bloque que no se repite a otra hora, conservando su duración. */
    fun moveTask(task: Task, newStart: Int) {
        val start = plannerTimeMinutes(task.time) ?: return
        val length = (blockEnd(task) ?: (start + 60)) - start
        val clamped = newStart.coerceIn(0, 24 * 60 - length.coerceAtLeast(15))
        val hasEnd = plannerTimeMinutes(task.endTime) != null
        write {
            taskRepository.updateTask(
                task.copy(time = hhmm(clamped), endTime = if (hasEnd) hhmm(clamped + length) else null)
            ).getOrThrow()
        }
    }

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
        initialSubtasks: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        if (title.isBlank()) { _error.value = "Escribe un título."; return }
        plannerTimeError(time, endTime)?.let { _error.value = it; return }
        if (repeatMode in listOf("WEEKLY", "CUSTOM") && (repeatDays.isEmpty() || repeatDays.any { it !in 1..7 })) {
            _error.value = "Selecciona los días de repetición."; return
        }
        write(onSuccess) {
            val subtaskItems = initialSubtasks.map { com.katchy.focuslive.data.model.SubtaskItem(title = it) }
            val newTask = Task(
                kind = "AGENDA",
                title = title.trim(),
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
            taskRepository.addTask(newTask).getOrThrow()
            
            // Schedule notification if time is set
            if (time != null) {
                notificationScheduler.scheduleTaskReminder(newTask)
            }
        }
    }

    fun updateTask(task: Task, onSuccess: () -> Unit = {}) {
        if (task.title.isBlank()) { _error.value = "Escribe un título."; return }
        plannerTimeError(task.time, task.endTime)?.let { _error.value = it; return }
        if (task.repeatMode in listOf("WEEKLY", "CUSTOM") && (task.repeatDays.isEmpty() || task.repeatDays.any { it !in 1..7 })) {
            _error.value = "Selecciona los días de repetición."; return
        }
        write(onSuccess) {
            taskRepository.updateTask(task.copy(kind = "AGENDA")).getOrThrow()
            
            // Reschedule notification
            notificationScheduler.cancelTaskReminder(task.id)
            if (task.time != null && !task.isCompleted) {
                notificationScheduler.scheduleTaskReminder(task)
            }
        }
    }

    fun beginFocus(task: Task) {
        timerManager.setActiveTask(task.id)
        timerManager.startTimer()
    }

    fun toggleTask(task: Task) {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastToggleTime[task.id] ?: 0L
        if (currentTime - lastTime < DEBOUNCE_DELAY) return
        lastToggleTime[task.id] = currentTime

        write { taskRepository.toggleTaskCompletion(task.id, _selectedDate.value).getOrThrow() }
    }

    fun deleteTask(task: Task, onSuccess: () -> Unit = {}) {
        write(onSuccess) {
            taskRepository.deleteTask(task.id).getOrThrow()
            notificationScheduler.cancelTaskReminder(task.id)
        }
    }

    fun restoreTask(task: Task) {
        write {
            taskRepository.addTask(task).getOrThrow()
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

        write { taskRepository.toggleSubtask(task.id, subtaskId, _selectedDate.value).getOrThrow() }
    }

    fun addSubtask(task: Task, subtaskTitle: String) {
        if (subtaskTitle.isBlank()) return
        write {
            val newSubtask = com.katchy.focuslive.data.model.SubtaskItem(title = subtaskTitle)
            val updatedTask = task.copy(subtasks = task.subtasks + newSubtask)
            taskRepository.updateTask(updatedTask).getOrThrow()
        }
    }

    fun updateSubtask(task: Task, subtaskId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        write {
            val updatedSubtasks = task.subtasks.map {
                if (it.id == subtaskId) it.copy(title = newTitle) else it
            }
            taskRepository.updateTask(task.copy(subtasks = updatedSubtasks)).getOrThrow()
        }
    }

    fun deleteSubtask(task: Task, subtaskId: String) {
         write {
            val updatedSubtasks = task.subtasks.filter { it.id != subtaskId }
            taskRepository.updateTask(task.copy(subtasks = updatedSubtasks)).getOrThrow()
        }
    }


}
