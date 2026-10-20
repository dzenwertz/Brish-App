package com.katchy.focuslive.ui.habits

import com.katchy.focuslive.data.model.Habit
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitLogicTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 19) // lunes
    private fun at(day: LocalDate, hour: Int = 9) = day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    private val created = at(today.minusDays(30))

    @Test
    fun targetAndTitleComeFromCaptureSuffix() {
        val habit = Habit(title = "Tomar agua · 8 al día")
        assertEquals(8, HabitLogic.target(habit))
        assertEquals("Tomar agua", HabitLogic.title(habit))
        assertEquals(1, HabitLogic.target(Habit(title = "Leer")))
    }

    @Test
    fun tapTogglesSingleHabit() {
        val habit = Habit(title = "Leer", timestamp = created)
        val done = HabitLogic.tap(habit, at(today), zone)
        assertTrue(HabitLogic.doneOn(done, today, zone))
        val undone = HabitLogic.tap(done, at(today, 20), zone)
        assertFalse(HabitLogic.doneOn(undone, today, zone))
    }

    @Test
    fun tapCountsUpToTargetThenResets() {
        var habit = Habit(title = "Agua · 3 al día", timestamp = created)
        repeat(3) { habit = HabitLogic.tap(habit, at(today, 8 + it), zone) }
        assertEquals(3, HabitLogic.countOn(habit, today, zone))
        assertTrue(HabitLogic.doneOn(habit, today, zone))
        habit = HabitLogic.tap(habit, at(today, 15), zone)
        assertEquals(0, HabitLogic.countOn(habit, today, zone))
    }

    @Test
    fun weekPercentOnlyCountsScheduledDays() {
        val gym = Habit(
            title = "Gym",
            timestamp = created,
            daysOfWeek = listOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY),
            completedDates = listOf(at(today), at(today.minusDays(3))) // lunes y viernes
        )
        // Últimos 7 días (mar 13 … lun 19): mié 14, vie 16, lun 19 → 2 de 3
        assertEquals(66, HabitLogic.weekPercent(listOf(gym), today, zone))
        assertEquals(3, HabitLogic.scheduledThisWeek(gym, today))
    }

    @Test
    fun weekPercentIsNullWithoutHabits() {
        assertNull(HabitLogic.weekPercent(emptyList(), today, zone))
    }
}
