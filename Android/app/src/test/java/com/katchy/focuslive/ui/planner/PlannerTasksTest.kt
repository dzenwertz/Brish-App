package com.katchy.focuslive.ui.planner

import org.junit.Assert.*
import org.junit.Test

class PlannerTasksTest {
    @Test fun `only agenda kind without legacy delivery links appears`() {
        assertTrue(isAgendaTask("AGENDA", null, null))
        assertTrue(isAgendaTask("AGENDA", "", ""))
        assertFalse(isAgendaTask("DELIVERY", null, null))
        assertFalse(isAgendaTask("AGENDA", "course", null))
        assertFalse(isAgendaTask("AGENDA", null, "DELIVERY"))
        assertFalse(isAgendaTask("UNKNOWN", null, null))
    }
}
