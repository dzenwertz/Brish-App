package com.katchy.focuslive.ui.home

import com.katchy.focuslive.data.model.Course
import com.katchy.focuslive.data.model.LearningSpace
import com.katchy.focuslive.data.model.Task
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class HomeAgendaTest {
    private val today = dayStart(System.currentTimeMillis())
    private val course = Course(id = "course", name = "Matemática", spaceId = "active")
    private fun delivery(id: String, due: Long = today, hasDate: Boolean = true, courseId: String = course.id) =
        Task(id = id, title = id, courseId = courseId, dueDate = due, hasDueDate = hasDate)

    @Test fun upcoming_excludesUndatedCompletedArchivedAndUnknownCourses_beforeLimiting() {
        val archived = Course(id = "archived-course", spaceId = "archived")
        val tasks = listOf(
            delivery("undated", due = 0, hasDate = false),
            delivery("archived", due = 1, courseId = archived.id),
            delivery("unknown", due = 2, courseId = "missing"),
            delivery("complete", due = 3).copy(isCompleted = true),
            delivery("third", due = today + 3), delivery("first", due = today + 1),
            delivery("fourth", due = today + 4), delivery("second", due = today + 2)
        )
        val state = buildHomeState(tasks, listOf(course, archived), listOf(LearningSpace(id = "archived", isArchived = true)), today)
        assertEquals(listOf("first", "second", "third"), state.upcomingDeliveries.map { it.task.id })
        assertTrue(state.tasks.isEmpty())
    }

    @Test fun archiveChange_removesDelivery_andLegacyUnassignedCourseRemainsVisible() {
        val legacy = Course(id = "legacy", name = "Curso anterior")
        val tasks = listOf(delivery("active"), delivery("legacy", courseId = legacy.id))
        val before = buildHomeState(tasks, listOf(course, legacy), emptyList(), today)
        val after = buildHomeState(tasks, listOf(course, legacy), listOf(LearningSpace(id = "active", isArchived = true)), today)
        assertEquals(2, before.upcomingDeliveries.size)
        assertEquals(listOf("legacy"), after.upcomingDeliveries.map { it.task.id })
    }

    @Test fun agenda_includesTodayRecentOverdueAndExplicitMyDay_butSeparatesDeliveries() {
        val tasks = listOf(
            Task(id = "today", timestamp = today),
            Task(id = "overdue", timestamp = today - 86_400_000),
            Task(id = "old", timestamp = today - 20 * 86_400_000L),
            Task(id = "future", timestamp = today + 86_400_000),
            Task(id = "undated", timestamp = today, hasDueDate = false),
            Task(id = "my-day", hasDueDate = false, isMyDay = true),
            delivery("delivery")
        )
        val state = buildHomeState(tasks, listOf(course), emptyList(), today)
        assertEquals(setOf("today", "overdue", "my-day"), state.tasks.map { it.id }.toSet())
        assertEquals("delivery", state.upcomingDeliveries.single().task.id)
    }

    @Test fun recurringCompletion_isScopedToToday_andRespectsRepeatDays() {
        val weekday = Calendar.getInstance().apply { timeInMillis = today }.get(Calendar.DAY_OF_WEEK)
        val state = buildHomeState(listOf(
            Task(id = "daily", timestamp = today - 86_400_000, repeatMode = "DAILY", isCompleted = true, completedDates = listOf(today - 86_400_000)),
            Task(id = "weekly", timestamp = today, repeatMode = "WEEKLY", repeatDays = listOf(weekday), completedDates = listOf(today + 1)),
            Task(id = "other-day", timestamp = today, repeatMode = "CUSTOM", repeatDays = listOf(weekday % 7 + 1))
        ), emptyList(), emptyList(), today)
        assertEquals(setOf("daily", "weekly"), state.tasks.map { it.id }.toSet())
        assertFalse(state.tasks.first { it.id == "daily" }.isCompleted)
        assertTrue(state.tasks.first { it.id == "weekly" }.isCompleted)
        assertFalse(state.focusTasks.any { it.id == "weekly" })
    }

    @Test fun legacyCourseTask_belongsOnlyToDeliveries_andOrphanDeliveryNeverBecomesAgenda() {
        val state = buildHomeState(listOf(
            delivery("legacy").copy(kind = "AGENDA"),
            Task(id = "orphan", kind = "DELIVERY", timestamp = today)
        ), listOf(course), emptyList(), today)
        assertTrue(state.tasks.isEmpty())
        assertEquals(listOf("legacy"), state.upcomingDeliveries.map { it.task.id })
    }

    @Test fun focusIncludesUndatedAndBeyondUpcomingLimit_butExcludesArchivedCompletedAndMissingCourses() {
        val archived = Course(id = "archived-course", spaceId = "archived")
        val tasks = listOf(
            delivery("undated", hasDate = false),
            delivery("first", due = today + 1), delivery("second", due = today + 2),
            delivery("third", due = today + 3), delivery("fourth", due = today + 4),
            delivery("completed").copy(isCompleted = true),
            delivery("archived", courseId = archived.id), delivery("missing", courseId = "missing"),
            Task(id = "undated-agenda", hasDueDate = false)
        )
        val state = buildHomeState(tasks, listOf(course, archived), listOf(LearningSpace(id = "archived", isArchived = true)), today)
        assertEquals(setOf("undated", "first", "second", "third", "fourth", "undated-agenda"), state.focusTasks.map { it.id }.toSet())
        assertEquals(listOf("first", "second", "third"), state.upcomingDeliveries.map { it.task.id })
    }
}
