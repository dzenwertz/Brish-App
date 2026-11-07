package com.katchy.focuslive.ui.review

import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.ui.planner.startOfDay
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyReviewTest {
    private val now = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 25, 18, 0) }.timeInMillis // domingo
    private fun day(offset: Int) = Calendar.getInstance().apply { timeInMillis = startOfDay(now); add(Calendar.DAY_OF_YEAR, offset) }.timeInMillis

    @Test
    fun countsDoneBlocksMinutesAndLeftovers() {
        val tasks = listOf(
            Task(id = "web", kind = "AGENDA", timestamp = day(-2), time = "15:00", endTime = "17:00", category = "Trabajo", isCompleted = true),
            Task(id = "gym", kind = "AGENDA", timestamp = day(-6), time = "17:30", endTime = "18:30", category = "Salud",
                repeatMode = "CUSTOM", repeatDays = listOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY), completedDates = listOf(day(-6) + 1000, day(-4) + 1000)),
            Task(id = "closet", kind = "AGENDA", timestamp = day(-3), title = "Ordenar el clóset"),
            Task(id = "future", kind = "AGENDA", timestamp = day(2), time = "09:00")
        )
        val review = buildWeeklyReview(tasks, emptyList(), now)
        // Ocurrencias: web, gym lun/mié/vie, clóset = 5; hechas: web + gym lun + gym mié = 3
        assertEquals(5, review.total)
        assertEquals(3, review.done)
        assertEquals(mapOf("Trabajo" to 120, "Salud" to 120), review.minutesByArea.toMap())
        assertEquals(listOf("closet"), review.leftovers.map { it.id })
        assertEquals(4, review.nextDays.sumOf { it.second }) // la tarea futura + 3 gym de la semana siguiente
    }
}
