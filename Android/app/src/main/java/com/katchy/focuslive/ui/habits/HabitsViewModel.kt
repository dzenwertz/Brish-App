package com.katchy.focuslive.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.data.repository.HabitRepository
import com.katchy.focuslive.scheduler.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repository: HabitRepository,
    private val scheduler: NotificationScheduler
) : ViewModel() {
    val habits: StateFlow<List<Habit>?> = repository.getHabits()
        .map { list -> list.sortedBy { it.timestamp } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun tap(habit: Habit) = save(HabitLogic.tap(habit, System.currentTimeMillis()))

    /** Guarda cambios de un hábito y reprograma su aviso (si ya se cumplió hoy, el próximo es otro día). */
    fun save(habit: Habit, onSaved: () -> Unit = {}) = viewModelScope.launch {
        repository.updateHabit(habit)
            .onSuccess { scheduler.scheduleHabitReminder(habit); onSaved() }
            .onFailure { _error.value = "No se pudo guardar el hábito." }
    }

    fun delete(habit: Habit) = viewModelScope.launch {
        repository.deleteHabit(habit.id)
            .onSuccess { scheduler.cancelHabitReminder(habit.id) }
            .onFailure { _error.value = "No se pudo eliminar el hábito." }
    }

    fun clearError() { _error.value = null }
}
