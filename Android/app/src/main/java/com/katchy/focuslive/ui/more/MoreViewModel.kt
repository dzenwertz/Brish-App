package com.katchy.focuslive.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.repository.FinanceRepository
import com.katchy.focuslive.data.repository.HabitRepository
import com.katchy.focuslive.data.repository.TaskRepository
import com.katchy.focuslive.util.HabitUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class MoreTodayState(
    val pendingTasks: Int = 0,
    val completedTasks: Int = 0,
    val habitsDone: Int = 0,
    val habitsTotal: Int = 0,
    val expensesToday: Double = 0.0
)

@HiltViewModel
class MoreViewModel @Inject constructor(
    taskRepository: TaskRepository,
    habitRepository: HabitRepository,
    financeRepository: FinanceRepository
) : ViewModel() {
    private val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val state: StateFlow<MoreTodayState> = combine(
        taskRepository.getAllTasks(),
        habitRepository.getHabits(),
        financeRepository.getTransactions()
    ) { tasks, habits, transactions ->
        val todayTasks = tasks.filter { HabitUtils.isSameDay(it.dueDate, today) || it.isMyDay }
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val todayHabits = habits.filter { it.daysOfWeek.isEmpty() || dayOfWeek in it.daysOfWeek }

        MoreTodayState(
            pendingTasks = todayTasks.count { !it.isCompleted && it.completedDates.none { date -> HabitUtils.isSameDay(date, today) } },
            completedTasks = todayTasks.count { it.isCompleted || it.completedDates.any { date -> HabitUtils.isSameDay(date, today) } },
            habitsDone = todayHabits.count { habit -> habit.completedDates.any { HabitUtils.isSameDay(it, today) } },
            habitsTotal = todayHabits.size,
            expensesToday = transactions
                .filter { it.type == "EXPENSE" && HabitUtils.isSameDay(it.timestamp, today) }
                .sumOf { kotlin.math.abs(it.amount) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoreTodayState())
}
