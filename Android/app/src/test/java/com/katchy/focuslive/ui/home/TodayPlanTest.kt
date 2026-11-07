package com.katchy.focuslive.ui.home

import com.katchy.focuslive.data.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayPlanTest {
    private val web = Task(id = "web", title = "Web", time = "15:00", endTime = "17:00")
    private val gym = Task(id = "gym", title = "Gym", time = "17:30", endTime = "18:30")
    private val call = Task(id = "call", title = "Llamar", time = "19:00")
    private val bills = Task(id = "bills", title = "Pagar luz")
    private val morning = Task(id = "am", title = "Correo", time = "09:00")
    private val done = Task(id = "done", title = "Hecho", time = "08:00", isCompleted = true)

    @Test
    fun currentBlockWithProgressAndTimeLeft() {
        val plan = planToday(listOf(web, gym, call, bills, morning, done), nowMinutes = 16 * 60)
        assertEquals("web", plan.current?.id)
        assertEquals(0.5f, plan.currentProgress, 0.001f)
        assertEquals(60, plan.minutesLeft)
        assertEquals(listOf("gym", "call"), plan.next.map { it.id })
        assertEquals(listOf("am", "bills"), plan.pending.map { it.id }) // pasadas primero, luego sin hora
        assertEquals(listOf("done"), plan.done.map { it.id })
    }

    @Test
    fun blockWithoutEndLastsOneHour() {
        val plan = planToday(listOf(call), nowMinutes = 19 * 60 + 59)
        assertEquals("call", plan.current?.id)
        assertNull(planToday(listOf(call), nowMinutes = 20 * 60).current)
    }

    @Test
    fun gapBetweenBlocksHasNoCurrent() {
        val plan = planToday(listOf(web, gym), nowMinutes = 17 * 60 + 10)
        assertNull(plan.current)
        assertEquals(listOf("gym"), plan.next.map { it.id })
        assertEquals(listOf("web"), plan.pending.map { it.id })
    }

    @Test
    fun parsesTimes() {
        assertEquals(930, minutesOf("15:30"))
        assertNull(minutesOf("25:00"))
        assertNull(minutesOf(null))
    }

    @Test
    fun subMinuteProgressAdvancesSmoothlyWithSeconds() {
        val start = planToday(listOf(gym), nowMinutes = 17 * 60 + 30, nowSecondOfMinute = 0)
        val midMinute = planToday(listOf(gym), nowMinutes = 17 * 60 + 30, nowSecondOfMinute = 30)
        assertTrue(midMinute.currentProgress > start.currentProgress)
        assertEquals(0.5f / 60f, midMinute.currentProgress, 0.0005f)
    }

    @Test
    fun overdueTaskFromYesterdayIsPendingNotLater() {
        val today = java.util.Calendar.getInstance().apply { set(2026, 10, 7, 12, 0) }.timeInMillis
        val yesterday = today - 24L * 60 * 60 * 1000
        val overdue = com.katchy.focuslive.data.model.Task(id = "old", title = "Revisar propuesta", time = "21:00", timestamp = yesterday)
        val plan = planToday(listOf(overdue), nowMinutes = 12 * 60, todayMillis = today)
        assertNull(plan.current)
        assertTrue(plan.next.isEmpty())
        assertEquals(listOf("old"), plan.pending.map { it.id })
    }

    @Test
    fun pendingOrderIsStableRegardlessOfInputOrder() {
        val a = Task(id = "1", title = "Comprar frutas")
        val b = Task(id = "2", title = "Enviar factura")
        val c = Task(id = "3", title = "Revisar correos")
        val one = planToday(listOf(c, a, b), nowMinutes = 600).pending.map { it.id }
        val two = planToday(listOf(b, c, a), nowMinutes = 600).pending.map { it.id }
        assertEquals(listOf("1", "2", "3"), one)
        assertEquals(one, two)
    }
}
