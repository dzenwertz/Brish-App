package com.katchy.focuslive.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: com.katchy.focuslive.data.repository.HabitRepository,
    private val mascotRepository: com.katchy.focuslive.data.repository.MascotRepository,
    private val notificationScheduler: com.katchy.focuslive.scheduler.NotificationScheduler,
    private val widgetUpdateHelper: com.katchy.focuslive.widget.WidgetUpdateHelper,
    private val userStatsRepository: com.katchy.focuslive.data.repository.UserStatsRepository
) : ViewModel() {

    val selectedMascot = mascotRepository.selectedMascot

    val userStats = userStatsRepository.userStats

    val habits: StateFlow<List<com.katchy.focuslive.data.model.Habit>> = habitRepository.getHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _monthOffset = kotlinx.coroutines.flow.MutableStateFlow(0)
    val monthOffset = _monthOffset.asStateFlow()

    val currentMonthName: StateFlow<String> = _monthOffset.map { offset ->
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, offset)
        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, java.util.Locale("es", "ES"))
            ?.replaceFirstChar { it.uppercase() } ?: ""
        "$month ${calendar.get(Calendar.YEAR)}"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val monthlyStats: StateFlow<List<DayStats>> = kotlinx.coroutines.flow.combine(habits, _monthOffset) { currentHabits, offset ->
        val stats = mutableListOf<DayStats>()
        val calendar = Calendar.getInstance()
        
        calendar.add(Calendar.MONTH, offset)
        
        val targetMonth = calendar.get(Calendar.MONTH)
        val targetYear = calendar.get(Calendar.YEAR)
        
        val todayCalendar = Calendar.getInstance()
        val todayYear = todayCalendar.get(Calendar.YEAR)
        val todayDayOfYear = todayCalendar.get(Calendar.DAY_OF_YEAR)

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        for (day in 1..maxDays) {
            val dayTimestamp = calendar.timeInMillis
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            val year = calendar.get(Calendar.YEAR)
            
            val isFuture = (year > todayYear) || (year == todayYear && dayOfYear > todayDayOfYear)

            val completedCount = currentHabits.count { habit ->
                habit.completedDates.any { date -> com.katchy.focuslive.util.HabitUtils.isSameDay(date, dayTimestamp) }
            }
            
            val totalHabits = currentHabits.size
            val rawScore = if (totalHabits > 0) completedCount.toFloat() / totalHabits else 0f
            
            val status = when {
                isFuture -> DayStatus.FUTURE
                totalHabits == 0 -> DayStatus.EMPTY
                rawScore == 1f -> DayStatus.COMPLETED 
                rawScore > 0f -> DayStatus.PARTIAL
                else -> DayStatus.MISSED
            }
            
            stats.add(DayStats(dayTimestamp, status, rawScore))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        stats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun nextMonth() {
        _monthOffset.value += 1
    }

    fun prevMonth() {
        _monthOffset.value -= 1
    }

    private val lastToggleTime = mutableMapOf<String, Long>()
    private val DEBOUNCE_DELAY = 250L

    fun toggleHabitDate(habitId: String, dateTimestamp: Long) {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastToggleTime[habitId] ?: 0L
        if (currentTime - lastTime < DEBOUNCE_DELAY) return
        lastToggleTime[habitId] = currentTime

        viewModelScope.launch {
            val currentList = habits.value
            val habit = currentList.find { it.id == habitId } ?: return@launch
            
            val dates = habit.completedDates.toMutableList()
            val existingDate = dates.find { com.katchy.focuslive.util.HabitUtils.isSameDay(it, dateTimestamp) }
            
            if (existingDate != null) {
                dates.remove(existingDate)
                // Deduct gamification rewards when unchecking
                val multiplier = userStatsRepository.getStreakMultiplier()
                val xpAwarded = (15 * multiplier).toInt()
                userStatsRepository.deductXp(xpAwarded)
                mascotRepository.deductXpFromSelectedMascot(xpAwarded)
                userStatsRepository.deductCoins((8 * multiplier).toInt())
                userStatsRepository.decrementHabits()
            } else {
                dates.add(dateTimestamp)
                // Add gamification rewards
                val multiplier = userStatsRepository.getStreakMultiplier()
                val xpAwarded = (15 * multiplier).toInt()
                userStatsRepository.addXp(xpAwarded)
                mascotRepository.addXpToSelectedMascot(xpAwarded)
                userStatsRepository.addCoins((8 * multiplier).toInt())
                userStatsRepository.incrementHabits()
                
                // Notify mascot to celebrate
                com.katchy.focuslive.ui.util.MascotEventBus.emit(com.katchy.focuslive.ui.util.MascotEvent.HABIT_DONE)
            }
            
            // Recalculate streak with the UPDATED list of dates
            val newStreak = com.katchy.focuslive.util.HabitUtils.calculateStreak(dates)
            
            val updatedHabit = habit.copy(
                completedDates = dates,
                currentStreak = newStreak
            )
            
            habitRepository.updateHabit(updatedHabit)

            // Update Widget
            val maxStreak = (currentList.map { if(it.id == habitId) updatedHabit else it }.maxOfOrNull { it.currentStreak } ?: 0)
            widgetUpdateHelper.updateStreak(maxStreak)
        }
    }

    fun updateHabitDetails(habitId: String, newTitle: String, newReminder: String?, newDaysOfWeek: List<Int>) {
        viewModelScope.launch {
            val currentList = habits.value
            val habit = currentList.find { it.id == habitId } ?: return@launch

            val updatedHabit = habit.copy(
                title = newTitle,
                reminderTime = newReminder,
                daysOfWeek = newDaysOfWeek
            )
            
            habitRepository.updateHabit(updatedHabit)
            
            if (newReminder != null) {
                // Cancel previous just in case logic changed or time changed
                notificationScheduler.cancelHabitReminder(habit)
                notificationScheduler.scheduleHabitReminder(updatedHabit)
            } else {
                notificationScheduler.cancelHabitReminder(habit)
            }
        }
    }

    fun deleteHabit(habit: com.katchy.focuslive.data.model.Habit) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit.id)
            notificationScheduler.cancelHabitReminder(habit)
        }
    }

    fun restoreHabit(habit: com.katchy.focuslive.data.model.Habit) {
        viewModelScope.launch {
            habitRepository.addHabit(habit)
            if (habit.reminderTime != null) {
                notificationScheduler.scheduleHabitReminder(habit)
            }
        }
    }

    fun addHabit(title: String, reminderTime: String?, daysOfWeek: List<Int>) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val newHabit = com.katchy.focuslive.data.model.Habit(
                title = title,
                currentStreak = 0,
                completedDates = emptyList(),
                reminderTime = reminderTime,
                daysOfWeek = daysOfWeek,
                timestamp = System.currentTimeMillis()
            )
            habitRepository.addHabit(newHabit)
            if (reminderTime != null) {
                notificationScheduler.scheduleHabitReminder(newHabit)
            }
        }
    }
}

enum class DayStatus {
    COMPLETED, // Green (100%)
    PARTIAL,   // Orange/Yellow (>0%)
    MISSED,    // Red (0% in past)
    FUTURE,    // Gray
    EMPTY      // No habits exists
}

data class DayStats(
    val timestamp: Long,
    val status: DayStatus,
    val ratio: Float
)
