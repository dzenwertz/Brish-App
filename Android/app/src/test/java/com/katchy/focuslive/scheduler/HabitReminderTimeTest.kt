package com.katchy.focuslive.scheduler

import com.katchy.focuslive.data.model.Habit
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HabitReminderTimeTest {
    private val zone = ZoneOffset.UTC
    private val monday = LocalDate.of(2026, 10, 19)
    private fun at(day: LocalDate, h: Int, m: Int = 0) = day.atTime(h, m).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun remindsLaterToday() {
        val habit = Habit(title = "Leer", reminderTime = "21:30")
        assertEquals(at(monday, 21, 30), nextHabitReminderAt(habit, at(monday, 9), zone))
    }

    @Test
    fun skipsTodayWhenAlreadyDone() {
        val habit = Habit(title = "Leer", reminderTime = "21:30", completedDates = listOf(at(monday, 8)))
        assertEquals(at(monday.plusDays(1), 21, 30), nextHabitReminderAt(habit, at(monday, 9), zone))
    }

    @Test
    fun onlyOnScheduledDays() {
        val gym = Habit(title = "Gym", reminderTime = "17:00", daysOfWeek = listOf(Calendar.WEDNESDAY))
        assertEquals(at(monday.plusDays(2), 17), nextHabitReminderAt(gym, at(monday, 18), zone))
    }

    @Test
    fun noTimeMeansNoReminder() {
        assertNull(nextHabitReminderAt(Habit(title = "Agua"), at(monday, 9), zone))
    }
}
