package com.katchy.focuslive.ui.courses

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CourseDatesTest {
    private fun date(zone: String, day: Int, hour: Int = 0): Long = Calendar.getInstance(TimeZone.getTimeZone(zone)).apply {
        clear()
        set(2026, Calendar.SEPTEMBER, day, hour, 0)
    }.timeInMillis

    @Test fun pickerPreservesSelectedDayInLima() {
        val zone = TimeZone.getTimeZone("America/Lima")
        assertEquals(date("America/Lima", 9), pickerDateToLocalMillis(date("UTC", 9), zone))
        assertEquals(date("UTC", 9), localDateToPickerMillis(date("America/Lima", 9, 23), zone))
    }

    @Test fun yesterdayEveningIsNotToday() {
        val zone = TimeZone.getTimeZone("America/Lima")
        assertEquals(-1L, courseDaysFromToday(date("America/Lima", 8, 23), date("America/Lima", 9, 1), zone))
        assertEquals(1L, courseDaysFromToday(date("America/Lima", 10), date("America/Lima", 9, 23), zone))
    }
}
