package com.katchy.focuslive.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.data.repository.HabitRepository
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
    private val repository: HabitRepository
) : ViewModel() {
    val habits: StateFlow<List<Habit>?> = repository.getHabits()
        .map { list -> list.sortedBy { it.timestamp } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun tap(habit: Habit) = viewModelScope.launch {
        repository.updateHabit(HabitLogic.tap(habit, System.currentTimeMillis()))
            .onFailure { _error.value = "No se pudo guardar el hábito." }
    }

    fun delete(habit: Habit) = viewModelScope.launch {
        repository.deleteHabit(habit.id).onFailure { _error.value = "No se pudo eliminar el hábito." }
    }

    fun clearError() { _error.value = null }
}
