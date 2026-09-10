package com.katchy.focuslive.util

import java.util.Calendar

object HabitUtils {

    fun calculateStreak(completedDates: List<Long>): Int {
        if (completedDates.isEmpty()) return 0

        // 1. Sort descending
        // 2. Normalize to start of day (Local Time) to avoid time-of-day issues
        val uniqueDays = completedDates.map {
            val c = Calendar.getInstance().apply { timeInMillis = it }
            Calendar.getInstance().apply {
                set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.distinct().sortedDescending()

        if (uniqueDays.isEmpty()) return 0

        // Get "Today" start of day
        val todayFn = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterdayFn = Calendar.getInstance().apply {
            timeInMillis = todayFn
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis

        // Start checking from the most recent completion
        val lastCompletion = uniqueDays.first()

        // If the last completion was BEFORE yesterday (and not today), the streak is broken/frozen.
        // We allow a 1-day gap "grace period" where the streak doesn't reset to 0 immediately if you missed yesterday but haven't logged today yet.
        // BUT, if you log Today, the streak continues.
        // If you missed Yesterday AND Today, strictly speaking streak is 0 or frozen at previous value?
        // Standard logic: Streak is active if last completion was Today or Yesterday.
        
        // Strict Check:
        // if (lastCompletion < yesterdayFn) return 0 
        
        // "Forgiving" Check (requested): 
        // If I missed yesterday, my streak is technically broken, but maybe we want to show the OLD streak until I miss 2 days?
        // Or does "bajar" mean it went from 5 to 1 because I logged today after missing yesterday?
        // Let's implement standard "Current Streak" logic:
        // - Count backwards from Today (if completed) or Yesterday (if not completed today).
        
        // Valid anchors to start counting:
        val anchorDate = if (lastCompletion == todayFn) todayFn else yesterdayFn
        
        // If the newest completion is older than yesterday, the active streak is 0.
        if (lastCompletion < yesterdayFn) {
            return 0
        }

        var streak = 0
        var currentCheckDate = if (lastCompletion == todayFn) todayFn else yesterdayFn
        
        // We iterate through uniqueDays to find continuity
        // However, a simpler approach for streak is:
        // Check if we have completion for 'currentCheckDate', then 'currentCheckDate - 1 day', etc.
        
        for (day in uniqueDays) {
            if (day == currentCheckDate) {
                streak++
                // Move check to previous day
                currentCheckDate = Calendar.getInstance().apply {
                    timeInMillis = currentCheckDate
                    add(Calendar.DAY_OF_YEAR, -1)
                }.timeInMillis
            } else if (day > currentCheckDate) {
                // Should not happen if sorted descending and we started from max, 
                // but if we started check from Yesterday and we actually have Today logged, we just skipped Today in the loop?
                // Wait, if lastCompletion == todayFn, currentCheckDate starts at Today.
                // If lastCompletion == yesterdayFn, currentCheckDate starts at Yesterday.
                continue 
            } else {
                // day < currentCheckDate
                // We missed a day!
                // Streak ends.
                break
            }
        }
        
        return streak
    }

    fun isSameDay(ms1: Long, ms2: Long): Boolean {
        if (ms1 == 0L || ms2 == 0L) return false
        val c1 = Calendar.getInstance().apply { timeInMillis = ms1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = ms2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
               c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
}
