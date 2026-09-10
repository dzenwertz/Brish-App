package com.katchy.focuslive.ui.courses

import java.util.Calendar
import java.util.TimeZone

// Material DatePicker represents calendar dates at midnight UTC, not local instants.
internal fun localDateToPickerMillis(value: Long, zone: TimeZone = TimeZone.getDefault()): Long =
    copyDate(value, zone, TimeZone.getTimeZone("UTC"))

internal fun pickerDateToLocalMillis(value: Long, zone: TimeZone = TimeZone.getDefault()): Long =
    copyDate(value, TimeZone.getTimeZone("UTC"), zone)

private fun copyDate(value: Long, from: TimeZone, to: TimeZone): Long {
    val source = Calendar.getInstance(from).apply { timeInMillis = value }
    return Calendar.getInstance(to).apply {
        clear()
        set(source.get(Calendar.YEAR), source.get(Calendar.MONTH), source.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

internal fun courseDaysFromToday(value: Long, now: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): Long =
    (localDateToPickerMillis(value, zone) - localDateToPickerMillis(now, zone)) / 86_400_000L
