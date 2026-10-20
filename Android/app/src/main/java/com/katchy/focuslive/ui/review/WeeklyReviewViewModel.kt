package com.katchy.focuslive.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.data.repository.HabitRepository
import com.katchy.focuslive.data.repository.TaskRepository
import com.katchy.focuslive.ui.planner.startOfDay
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WeeklyReviewViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    habitRepository: HabitRepository
) : ViewModel() {
    val review: StateFlow<WeeklyReview?> = combine(taskRepository.getAgendaTasks(), habitRepository.getHabits()) { tasks, habits ->
        buildWeeklyReview(tasks, habits, System.currentTimeMillis())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Pasa un pendiente a hoy, conservando su hora. */
    fun moveToToday(task: Task) = viewModelScope.launch {
        val today = startOfDay(System.currentTimeMillis())
        taskRepository.updateTask(task.copy(timestamp = today, dueDate = today))
            .onFailure { _error.value = "No se pudo mover «${task.title}»." }
    }

    /** Suelta un pendiente: se elimina de la agenda. */
    fun letGo(task: Task) = viewModelScope.launch {
        taskRepository.deleteTask(task.id).onFailure { _error.value = "No se pudo soltar «${task.title}»." }
    }
}
