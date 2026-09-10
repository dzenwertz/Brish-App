package com.katchy.focuslive.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class HabitUtilsTest {

    private fun getStartOfDayMillis(daysOffset: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun testCalculateStreak_emptyList() {
        val completedDates = emptyList<Long>()
        val result = HabitUtils.calculateStreak(completedDates)
        assertEquals(0, result)
    }

    @Test
    fun testCalculateStreak_completedTodayOnly() {
        val today = getStartOfDayMillis(0)
        // Add completion with time of day to verify normalization
        val completedDates = listOf(today + 50000)
        val result = HabitUtils.calculateStreak(completedDates)
        assertEquals(1, result)
    }

    @Test
    fun testCalculateStreak_completedYesterdayOnly() {
        val yesterday = getStartOfDayMillis(-1)
        val completedDates = listOf(yesterday + 10000)
        val result = HabitUtils.calculateStreak(completedDates)
        assertEquals(1, result)
    }

    @Test
    fun testCalculateStreak_completedTodayAndYesterday() {
        val today = getStartOfDayMillis(0)
        val yesterday = getStartOfDayMillis(-1)
        val completedDates = listOf(today + 50000, yesterday + 12000)
        val result = HabitUtils.calculateStreak(completedDates)
        assertEquals(2, result)
    }

    @Test
    fun testCalculateStreak_completedYesterdayAndTwoDaysAgo() {
        val yesterday = getStartOfDayMillis(-1)
        val twoDaysAgo = getStartOfDayMillis(-2)
        val completedDates = listOf(yesterday + 10000, twoDaysAgo + 25000)
        val result = HabitUtils.calculateStreak(completedDates)
        assertEquals(2, result)
    }

    @Test
    fun testCalculateStreak_missedYesterday_resetsStreak() {
        val today = getStartOfDayMillis(0)
        val twoDaysAgo = getStartOfDayMillis(-2)
        val completedDates = listOf(today + 30000, twoDaysAgo + 15000)
        val result = HabitUtils.calculateStreak(completedDates)
        assertEquals(1, result)
    }

    @Test
    fun testCalculateStreak_missedYesterdayAndToday_zeroStreak() {
        val twoDaysAgo = getStartOfDayMillis(-2)
        val threeDaysAgo = getStartOfDayMillis(-3)
        val completedDates = listOf(twoDaysAgo + 12000, threeDaysAgo + 40000)
        val result = HabitUtils.calculateStreak(completedDates)
        assertEquals(0, result)
    }

    @Test
    fun testCalculateStreak_multipleCompletionsOnSameDay() {
        val today = getStartOfDayMillis(0)
        val yesterday = getStartOfDayMillis(-1)
        // Multiple entries for the same day
        val completedDates = listOf(today + 50000, today + 10000, yesterday + 2000, yesterday + 4000)
        val result = HabitUtils.calculateStreak(completedDates)
        assertEquals(2, result)
    }

    @Test
    fun testIsSameDay_sameDayDifferentHours_returnsTrue() {
        val todayStart = getStartOfDayMillis(0)
        val morning = todayStart + 9 * 3600 * 1000L // 09:00 AM
        val night = todayStart + 23 * 3600 * 1000L + 59 * 60 * 1000L // 23:59 PM
        org.junit.Assert.assertTrue(HabitUtils.isSameDay(morning, night))
    }

    @Test
    fun testIsSameDay_differentDays_returnsFalse() {
        val today = getStartOfDayMillis(0)
        val yesterday = getStartOfDayMillis(-1)
        org.junit.Assert.assertFalse(HabitUtils.isSameDay(today, yesterday))
    }

    @Test
    fun testIsSameDay_zeroTimestamp_returnsFalse() {
        val today = getStartOfDayMillis(0)
        org.junit.Assert.assertFalse(HabitUtils.isSameDay(0L, today))
        org.junit.Assert.assertFalse(HabitUtils.isSameDay(today, 0L))
        org.junit.Assert.assertFalse(HabitUtils.isSameDay(0L, 0L))
    }
}
