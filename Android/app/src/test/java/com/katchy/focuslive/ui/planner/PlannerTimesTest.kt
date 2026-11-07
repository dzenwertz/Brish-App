package com.katchy.focuslive.ui.planner

import org.junit.Assert.*
import org.junit.Test

class PlannerTimesTest {
    @Test fun `requires exact HH colon mm and valid clock range`() {
        listOf("9:00", "09:0", "24:00", "23:60", " 09:00", "09:00 ", "ab:cd", "").forEach {
            assertNull(plannerTimeMinutes(it))
        }
        assertEquals(0, plannerTimeMinutes("00:00"))
        assertEquals(1439, plannerTimeMinutes("23:59"))
    }
    @Test fun `optional schedule must end after start same day`() {
        assertNull(plannerTimeError(null, null))
        assertNull(plannerTimeError("09:00", null))
        assertNull(plannerTimeError("09:00", "09:01"))
        assertNotNull(plannerTimeError(null, "10:00"))
        assertNotNull(plannerTimeError("09:00", "09:00"))
        assertNotNull(plannerTimeError("23:00", "01:00"))
    }

    @Test
    fun durationNeverShowsZeroHours() {
        org.junit.Assert.assertEquals("45 min", formatDuration(45))
        org.junit.Assert.assertEquals("2 h", formatDuration(120))
        org.junit.Assert.assertEquals("1 h 30 min", formatDuration(90))
    }
}
