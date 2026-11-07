package com.katchy.focuslive.ui.courses

import java.util.Calendar
import org.junit.Assert.*
import org.junit.Test

class DeliverableDraftTest {
    @Test fun `date and time optional but time requires date and strict HH colon mm`() {
        assertNull(DeliverableDraft("Entrega").validationError())
        assertNotNull(DeliverableDraft(" ").validationError())
        assertNotNull(DeliverableDraft("Entrega", time = "09:00").validationError())
        listOf("9:00", "24:00", "12:60", " 09:00").forEach {
            assertNotNull(DeliverableDraft("Entrega", date = 1L, time = it).validationError())
        }
        assertNull(DeliverableDraft("Entrega", date = 1L, time = "09:00").validationError())
    }
    @Test fun `due timestamp uses chosen local day and clock time`() {
        val date = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 2, 17, 47) }.timeInMillis
        val due = Calendar.getInstance().apply { timeInMillis = deliveryDueMillis(date, "14:30") }
        assertEquals(2, due.get(Calendar.DAY_OF_MONTH))
        assertEquals(14, due.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, due.get(Calendar.MINUTE))
        assertEquals(0, due.get(Calendar.SECOND))
    }
}
