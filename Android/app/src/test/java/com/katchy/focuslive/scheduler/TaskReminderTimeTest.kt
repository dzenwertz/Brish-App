package com.katchy.focuslive.scheduler

import com.katchy.focuslive.data.model.Task
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TaskReminderTimeTest {
    private lateinit var originalZone: TimeZone

    @Before fun setZone() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"))
    }

    @After fun restoreZone() {
        TimeZone.setDefault(originalZone)
    }

    private fun date(day: Int, hour: Int = 0, minute: Int = 0): Long = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.OCTOBER, day, hour, minute)
    }.timeInMillis

    private fun agenda() = Task(
        id = "task", userId = "owner", kind = "AGENDA", timestamp = date(2),
        dueDate = date(9), time = "09:00"
    )

    @Test fun deliveryUsesDeadlineInsteadOfCreationDate() {
        assertEquals(date(9, 9), nextTaskReminderAt(agenda().copy(kind = "DELIVERY"), date(2, 8)))
    }

    @Test fun agendaUsesScheduledDateInsteadOfDeadline() {
        assertEquals(date(5, 9), nextTaskReminderAt(agenda().copy(timestamp = date(5)), date(2, 8)))
    }

    @Test fun legacyCourseAndDeliverableRowsUseDeadline() {
        listOf(agenda().copy(courseId = "course"), agenda().copy(deliverableType = "EXAM")).forEach {
            assertEquals(date(9, 9), nextTaskReminderAt(it, date(2, 8)))
            assertEquals("courses", taskReminderDestination(it))
        }
    }

    @Test fun destinationsMatchAgendaAndDeliveryTabs() {
        assertEquals("planner", taskReminderDestination(agenda()))
        assertEquals("courses", taskReminderDestination(agenda().copy(kind = "DELIVERY")))
    }

    @Test fun weeklyUsesEverySelectedWeekday() {
        val task = agenda().copy(repeatMode = "WEEKLY", repeatDays = listOf(Calendar.MONDAY, Calendar.WEDNESDAY))
        assertEquals(date(5, 9), nextTaskReminderAt(task, date(2, 10)))
        assertEquals(date(7, 9), nextTaskReminderAt(task, date(5, 10)))
    }

    @Test fun weeklyDoesNotFireOnAnUnselectedFutureStartDay() {
        val task = agenda().copy(timestamp = date(8), repeatMode = "WEEKLY", repeatDays = listOf(Calendar.MONDAY))
        assertEquals(date(12, 9), nextTaskReminderAt(task, date(2, 8)))
    }

    @Test fun customCannotFireBeforeFutureStart() {
        val task = agenda().copy(timestamp = date(5), repeatMode = "CUSTOM", repeatDays = listOf(Calendar.FRIDAY))
        assertEquals(date(9, 9), nextTaskReminderAt(task, date(2, 8)))
    }

    @Test fun dailyStartsOnItsFutureDate() {
        val task = agenda().copy(timestamp = date(9), repeatMode = "DAILY")
        assertEquals(date(9, 9), nextTaskReminderAt(task, date(2, 8)))
    }

    @Test fun nextOccurrenceIsStrictlyAfterNow() {
        assertEquals(date(3, 9), nextTaskReminderAt(agenda().copy(repeatMode = "DAILY"), date(2, 9)))
        assertNull(nextTaskReminderAt(agenda(), date(2, 9)))
    }

    @Test fun skipsCompletedRecurringDatesWithoutStoppingFutureReminders() {
        val task = agenda().copy(repeatMode = "DAILY", completedDates = listOf(date(2, 14), date(3)))
        assertEquals(date(4, 9), nextTaskReminderAt(task, date(2, 8)))
    }

    @Test fun pastAndCompletedOneOffTasksHaveNoReminder() {
        assertNull(nextTaskReminderAt(agenda(), date(3)))
        assertNull(nextTaskReminderAt(agenda().copy(isCompleted = true), date(2, 8)))
        assertNull(nextTaskReminderAt(agenda().copy(completedDates = listOf(date(2, 14))), date(2, 8)))
    }

    @Test fun validatesTimesAndSupportsLegacySingleDigitHour() {
        assertEquals(date(2, 9), nextTaskReminderAt(agenda().copy(time = "9:00"), date(2, 8)))
        listOf("24:00", "12:60", "-1:30", "09:0", "09:00:00", "", "no time").forEach {
            assertNull(nextTaskReminderAt(agenda().copy(time = it), date(2, 8)))
        }
        assertNull(nextTaskReminderAt(agenda().copy(time = null), date(2, 8)))
        assertNull(nextTaskReminderAt(agenda().copy(hasDueDate = false), date(2, 8)))
    }

    @Test fun invalidRecurrenceDoesNotCreateImplicitReminders() {
        listOf("WEEKLY", "CUSTOM").forEach { mode ->
            assertNull(nextTaskReminderAt(agenda().copy(repeatMode = mode), date(2, 8)))
            assertNull(nextTaskReminderAt(agenda().copy(repeatMode = mode, repeatDays = listOf(0, 8)), date(2, 8)))
        }
        assertNull(nextTaskReminderAt(agenda().copy(repeatMode = "UNKNOWN"), date(2, 8)))
    }
}
