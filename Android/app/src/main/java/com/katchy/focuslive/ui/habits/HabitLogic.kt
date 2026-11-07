package com.katchy.focuslive.ui.habits

import com.katchy.focuslive.data.model.Habit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reglas de hábitos sin rachas ni puntos. Un hábito con meta diaria («· 8 al día» en el título,
 * como lo guarda la Captura) registra una marca por cada vez; el resto, una por día.
 */
object HabitLogic {
    private val TargetSuffix = Regex("\\s*·\\s*(\\d{1,2}) al día$")

    fun target(habit: Habit): Int = TargetSuffix.find(habit.title)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, 30) ?: 1

    fun title(habit: Habit): String = habit.title.replace(TargetSuffix, "")

    // daysOfWeek usa las constantes de Calendar: domingo=1, lunes=2 … sábado=7.
    private fun LocalDate.calendarDay(): Int = dayOfWeek.value % 7 + 1

    private fun dayOf(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun scheduledOn(habit: Habit, day: LocalDate): Boolean =
        habit.daysOfWeek.isEmpty() || day.calendarDay() in habit.daysOfWeek

    fun countOn(habit: Habit, day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int =
        habit.completedDates.count { dayOf(it, zone) == day }

    fun doneOn(habit: Habit, day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        countOn(habit, day, zone) >= target(habit)

    /** Los 7 días que terminan hoy, del más antiguo al más reciente. */
    fun lastWeek(today: LocalDate): List<LocalDate> = (6 downTo 0).map { today.minusDays(it.toLong()) }

    /** Porcentaje cumplido de lo programado en los 7 días que terminan en [end]; null si no había nada. */
    fun weekPercent(habits: List<Habit>, end: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int? {
        var scheduled = 0
        var done = 0
        lastWeek(end).forEach { day ->
            habits.forEach { habit ->
                if (scheduledOn(habit, day) && !day.isBefore(dayOf(habit.timestamp, zone))) {
                    scheduled++
                    if (doneOn(habit, day, zone)) done++
                }
            }
        }
        return if (scheduled == 0) null else (done * 100) / scheduled
    }

    /** Toque en el botón del hábito: suma una marca, o desmarca si ya estaba completo hoy. */
    fun tap(habit: Habit, now: Long, zone: ZoneId = ZoneId.systemDefault()): Habit {
        val today = dayOf(now, zone)
        val todays = habit.completedDates.filter { dayOf(it, zone) == today }
        return if (todays.size >= target(habit)) {
            habit.copy(completedDates = habit.completedDates - todays.toSet())
        } else {
            habit.copy(completedDates = habit.completedDates + now)
        }
    }

    fun scheduledThisWeek(habit: Habit, today: LocalDate): Int = lastWeek(today).count { scheduledOn(habit, it) }

    fun doneThisWeek(habit: Habit, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int =
        lastWeek(today).count { scheduledOn(habit, it) && doneOn(habit, it, zone) }
}
