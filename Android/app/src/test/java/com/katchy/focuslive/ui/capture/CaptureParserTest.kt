package com.katchy.focuslive.ui.capture

import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureParserTest {
    // Lunes 19 de octubre de 2026
    private val today = LocalDate.of(2026, 10, 19)

    @Test
    fun taskWithWeekdayTimeAndArea() {
        val d = parseCapture("Pagar la luz el viernes 6pm #hogar", today)
        assertEquals(CaptureType.EVENT, d.type)
        assertEquals("Pagar la luz", d.title)
        assertEquals(LocalDate.of(2026, 10, 23), d.date)
        assertEquals(LocalTime.of(18, 0), d.time)
        assertEquals("Hogar", d.area)
    }

    @Test
    fun taskWithoutTimeStaysTask() {
        val d = parseCapture("comprar focos mañana", today)
        assertEquals(CaptureType.TASK, d.type)
        assertEquals("Comprar focos", d.title)
        assertEquals(today.plusDays(1), d.date)
        assertNull(d.time)
    }

    @Test
    fun routineFromShortWeekdays() {
        val d = parseCapture("gym lun mie vie 5:30pm", today)
        assertEquals(CaptureType.EVENT, d.type)
        assertEquals("Gym", d.title)
        assertEquals(listOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY), d.repeatDays)
        assertEquals(LocalTime.of(17, 30), d.time)
    }

    @Test
    fun notePrefix() {
        val d = parseCapture("idea: video de reparación", today)
        assertEquals(CaptureType.NOTE, d.type)
        assertEquals("Video de reparación", d.title)
    }

    @Test
    fun habitWithTimesPerDay() {
        val d = parseCapture("tomar agua 8 veces al día", today)
        assertEquals(CaptureType.HABIT, d.type)
        assertEquals("Tomar agua", d.title)
        assertEquals(8, d.timesPerDay)
    }

    @Test
    fun twentyFourHourAndExplicitDate() {
        val d = parseCapture("Dentista 23 de oct a las 15:30", today)
        assertEquals(LocalDate.of(2026, 10, 23), d.date)
        assertEquals(LocalTime.of(15, 30), d.time)
        assertEquals("Dentista", d.title)
    }

    @Test
    fun morningIsNotTomorrowWhenSaidAsPartOfDay() {
        val d = parseCapture("correr a las 6 de la mañana", today)
        assertEquals(LocalTime.of(6, 0), d.time)
        assertNull(d.date)
    }

    @Test
    fun dailyRoutine() {
        val d = parseCapture("meditar todos los días 7am", today)
        assertTrue(d.daily)
        assertEquals(LocalTime.of(7, 0), d.time)
        assertEquals("Meditar", d.title)
    }

    @Test
    fun abbreviationAloneIsNotAWeekday() {
        val d = parseCapture("ir al mar", today)
        assertEquals("Ir al mar", d.title)
        assertNull(d.date)
    }

    @Test
    fun forcedTypeWins() {
        val d = parseCapture("reunión 4pm", today, forced = CaptureType.NOTE)
        assertEquals(CaptureType.NOTE, d.type)
        assertEquals("Reunión", d.title)
    }

    @Test
    fun detectsNonOverlappingTokenSpansForHighlighting() {
        val raw = "nota: Pagar la luz el viernes 6pm #hogar"
        val draft = parseCapture(raw, today)
        val kinds = draft.spans.map { it.kind }.toSet()
        assertTrue(CaptureTokenKind.TYPE in kinds)
        assertTrue(CaptureTokenKind.DATE in kinds)
        assertTrue(CaptureTokenKind.TIME in kinds)
        assertTrue(CaptureTokenKind.AREA in kinds)
        // Verify spans are sorted and non-overlapping
        draft.spans.zipWithNext().forEach { (a, b) ->
            assertTrue("Expected non-overlapping spans: $a vs $b", a.range.last < b.range.first)
        }
    }

    @Test
    fun dailyWithoutTimeIsHabit() {
        val withAccent = parseCapture("leer 20 min todos los días", today)
        assertEquals(CaptureType.HABIT, withAccent.type)
        assertEquals("Leer 20 min", withAccent.title)
        // Sin tilde, como se escribe rápido en el teléfono.
        assertEquals(CaptureType.HABIT, parseCapture("leer 20 min todos los dias", today).type)
        // Con hora sigue siendo una rutina de la Agenda.
        assertEquals(CaptureType.EVENT, parseCapture("meditar todos los días 7am", today).type)
    }
}
