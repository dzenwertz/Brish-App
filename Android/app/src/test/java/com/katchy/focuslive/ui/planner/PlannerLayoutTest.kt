package com.katchy.focuslive.ui.planner

import com.katchy.focuslive.data.model.Task
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class PlannerLayoutTest {
    private fun t(id: String, start: String?, end: String? = null, done: Boolean = false) =
        Task(id = id, title = id, kind = "AGENDA", time = start, endTime = end, isCompleted = done)

    @Test
    fun overlappingBlocksShareLanes() {
        val placed = layoutDay(listOf(t("a", "09:00", "11:00"), t("b", "10:00", "10:30"), t("c", "11:00", "12:00"), t("d", "10:30", "11:30")))
            .associateBy { it.task.id }
        assertEquals(0, placed.getValue("a").lane)
        assertEquals(1, placed.getValue("b").lane)
        assertEquals(1, placed.getValue("d").lane) // reutiliza el carril de b
        assertEquals(2, placed.getValue("a").lanes)
        assertEquals(0, placed.getValue("c").lane)  // c empieza cuando a termina, pero d sigue: mismo grupo
        assertEquals(2, placed.getValue("c").lanes)
    }

    @Test
    fun separateBlocksUseOneLane() {
        val placed = layoutDay(listOf(t("a", "09:00", "10:00"), t("b", "13:00")))
        assertEquals(listOf(1, 1), placed.map { it.lanes })
        assertEquals(14 * 60, placed.last().end)
    }

    @Test
    fun freeGapsSkipDoneAndShortHoles() {
        val gaps = freeGaps(
            listOf(t("lunch", "13:00", "14:00"), t("web", "15:00", "17:00"), t("gym", "17:30", "18:30"), t("old", "19:00", "20:00", done = true)),
            from = 12 * 60, to = 22 * 60
        )
        assertEquals(listOf(12 * 60 until 13 * 60, 14 * 60 until 15 * 60, (18 * 60 + 30) until 22 * 60), gaps)
    }

    @Test
    fun weekStartsOnMonday() {
        val wed = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 21, 15, 0) }.timeInMillis
        val week = weekOf(wed)
        val first = Calendar.getInstance().apply { timeInMillis = week.first() }
        assertEquals(Calendar.MONDAY, first.get(Calendar.DAY_OF_WEEK))
        assertEquals(19, first.get(Calendar.DAY_OF_MONTH))
        assertEquals(7, week.size)
    }

    @Test
    fun recurringTaskAppearsOnItsDays() {
        val monday = weekOf(Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 21) }.timeInMillis).first()
        val gym = Task(id = "gym", kind = "AGENDA", timestamp = monday, repeatMode = "CUSTOM", repeatDays = listOf(Calendar.MONDAY, Calendar.FRIDAY))
        val week = weekOf(monday)
        assertEquals(listOf(true, false, false, false, true, false, false), week.map { tasksOnDay(listOf(gym), it).isNotEmpty() })
    }
}
