package com.katchy.focuslive.util

import java.util.Calendar

/** Shared calendar-day comparison for recurring agenda activities. */
object HabitUtils {
    fun isSameDay(first: Long, second: Long): Boolean {
        if (first == 0L || second == 0L) return false
        val a = Calendar.getInstance().apply { timeInMillis = first }
        val b = Calendar.getInstance().apply { timeInMillis = second }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }
}
