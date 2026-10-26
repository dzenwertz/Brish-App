package com.katchy.focuslive.ui.courses

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarEdgeCasesTest {
    private fun date(zone: String, year: Int, month: Int, day: Int, hour: Int = 0): Long =
        Calendar.getInstance(TimeZone.getTimeZone(zone)).apply {
            clear()
            set(year, month - 1, day, hour, 0)
        }.timeInMillis

    @Test fun nextCalendarDayAcrossSpringClockChangeIsOneDayAway() {
        val zone = TimeZone.getTimeZone("America/New_York")
        val now = date(zone.id, 2026, 3, 8)
        val tomorrow = date(zone.id, 2026, 3, 9)
        assertEquals(1L, courseDaysFromToday(tomorrow, now, zone))
    }

    @Test fun nextCalendarDayAcrossAutumnClockChangeIsOneDayAway() {
        val zone = TimeZone.getTimeZone("America/New_York")
        val now = date(zone.id, 2026, 11, 1)
        val tomorrow = date(zone.id, 2026, 11, 2)
        assertEquals(1L, courseDaysFromToday(tomorrow, now, zone))
    }

    @Test fun leapDaySurvivesPickerRoundTripInLima() {
        val zone = TimeZone.getTimeZone("America/Lima")
        val selected = date("UTC", 2028, 2, 29)
        assertEquals(selected, localDateToPickerMillis(pickerDateToLocalMillis(selected, zone), zone))
    }

    @Test fun yearBoundaryUsesCalendarDaysInLima() {
        val zone = TimeZone.getTimeZone("America/Lima")
        assertEquals(1L, courseDaysFromToday(
            date(zone.id, 2027, 1, 1), date(zone.id, 2026, 12, 31, 23), zone
        ))
    }
}
