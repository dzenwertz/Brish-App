package com.katchy.focuslive.scheduler

import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.ui.habits.HabitLogic
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Próximo aviso de un hábito estrictamente después de [now]: a su hora, en un día programado
 * y saltando el día de hoy si ya se cumplió. Null si no tiene hora de aviso.
 */
internal fun nextHabitReminderAt(habit: Habit, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
    val time = habit.reminderTime?.takeIf { it.matches(Regex("\\d{1,2}:\\d{2}")) } ?: return null
    val (h, m) = time.split(':').map { it.toInt() }
    if (h !in 0..23 || m !in 0..59) return null
    val reminder = LocalTime.of(h, m)
    var day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    repeat(8) {
        if (HabitLogic.scheduledOn(habit, day) && !HabitLogic.doneOn(habit, day, zone)) {
            val at = day.atTime(reminder).atZone(zone).toInstant().toEpochMilli()
            if (at > now) return at
        }
        day = day.plusDays(1)
    }
    return null
}
