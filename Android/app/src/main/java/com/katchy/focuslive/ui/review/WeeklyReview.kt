package com.katchy.focuslive.ui.review

import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.ui.components.areaStyle
import com.katchy.focuslive.ui.habits.HabitLogic
import com.katchy.focuslive.ui.planner.blockEnd
import com.katchy.focuslive.ui.planner.plannerTimeMinutes
import com.katchy.focuslive.ui.planner.startOfDay
import com.katchy.focuslive.ui.planner.tasksOnDay
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

/** Resumen de los 7 días que terminan hoy, más lo que quedó pendiente y la carga de la semana siguiente. */
data class WeeklyReview(
    val days: List<Long>,
    val done: Int,
    val total: Int,
    val habitPercent: Int?,
    val minutesByArea: List<Pair<String, Int>>,
    val leftovers: List<Task>,
    val nextDays: List<Pair<Long, Int>>
)

private fun shift(day: Long, by: Int) = Calendar.getInstance().apply { timeInMillis = day; add(Calendar.DAY_OF_YEAR, by) }.timeInMillis

internal fun buildWeeklyReview(tasks: List<Task>, habits: List<Habit>, now: Long, zone: ZoneId = ZoneId.systemDefault()): WeeklyReview {
    val today = startOfDay(now)
    val days = (6 downTo 0).map { shift(today, -it) }
    val occurrences = days.flatMap { day -> tasksOnDay(tasks, day) }
    val minutes = occurrences.filter { it.isCompleted }
        .mapNotNull { t -> plannerTimeMinutes(t.time)?.let { s -> areaStyle(t.category).name to (blockEnd(t)!! - s) } }
        .groupBy({ it.first }, { it.second })
        .map { (area, list) -> area to list.sum() }
        .sortedByDescending { it.second }
    // Pendientes: tareas únicas de días anteriores que nunca se marcaron.
    val leftovers = tasks.filter { t ->
        t.repeatMode == "NONE" && !t.isCompleted && t.completedDates.isEmpty() &&
            startOfDay(t.timestamp) in days.first() until today &&
            t.kind == "AGENDA" && t.courseId.isNullOrBlank()
    }.sortedBy { it.timestamp }
    val nextDays = (1..7).map { shift(today, it) }.map { day -> day to tasksOnDay(tasks, day).size }
    val habitDay = Instant.ofEpochMilli(today).atZone(zone).toLocalDate()
    return WeeklyReview(
        days = days,
        done = occurrences.count { it.isCompleted },
        total = occurrences.size,
        habitPercent = HabitLogic.weekPercent(habits, habitDay, zone),
        minutesByArea = minutes,
        leftovers = leftovers,
        nextDays = nextDays
    )
}
