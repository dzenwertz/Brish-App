package com.katchy.focuslive.ui.planner

import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.util.HabitUtils
import java.util.Calendar

/** Inicio del día local de [millis]. */
internal fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Los siete días (lunes a domingo) de la semana que contiene [date]. */
internal fun weekOf(date: Long): List<Long> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = startOfDay(date)
        firstDayOfWeek = Calendar.MONDAY
        val shift = (get(Calendar.DAY_OF_WEEK) + 5) % 7 // lunes = 0
        add(Calendar.DAY_OF_YEAR, -shift)
    }
    return (0..6).map { i -> (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }.timeInMillis }
}

/**
 * Tareas de la agenda que ocurren en [date] (inicio del día), con el estado de completado de ese día
 * para las que se repiten.
 */
internal fun tasksOnDay(tasks: List<Task>, date: Long): List<Task> {
    val weekday = Calendar.getInstance().apply { timeInMillis = date }.get(Calendar.DAY_OF_WEEK)
    return tasks.filter { isAgendaTask(it.kind, it.courseId, it.deliverableType) }.filter { task ->
        val start = startOfDay(task.timestamp)
        when (task.repeatMode) {
            "DAILY" -> date >= start
            "WEEKLY", "CUSTOM" -> date >= start && task.repeatDays.contains(weekday)
            else -> date == start
        }
    }.map { task ->
        if (task.repeatMode != "NONE") {
            task.copy(
                isCompleted = task.completedDates.any { HabitUtils.isSameDay(it, date) },
                subtasks = task.subtasks.map { s -> s.copy(isCompleted = s.completedDates.any { HabitUtils.isSameDay(it, date) }) }
            )
        } else {
            task.copy(isCompleted = task.isCompleted || task.completedDates.any { HabitUtils.isSameDay(it, date) })
        }
    }.sortedBy { it.time ?: "23:59" }
}

/** Fin de un bloque en minutos: su hora de término o una hora después del inicio. */
internal fun blockEnd(task: Task): Int? {
    val start = plannerTimeMinutes(task.time) ?: return null
    return plannerTimeMinutes(task.endTime)?.takeIf { it > start } ?: (start + 60).coerceAtMost(24 * 60)
}

/** Un bloque colocado en la línea del día. Los que se solapan se reparten en carriles. */
data class PlacedBlock(val task: Task, val start: Int, val end: Int, val lane: Int, val lanes: Int)

internal fun layoutDay(tasks: List<Task>): List<PlacedBlock> {
    val timed = tasks.mapNotNull { t -> plannerTimeMinutes(t.time)?.let { s -> Triple(t, s, blockEnd(t)!!) } }
        .sortedWith(compareBy({ it.second }, { -it.third }))
    val result = mutableListOf<PlacedBlock>()
    var group = mutableListOf<Triple<Task, Int, Int>>()
    var groupEnd = -1
    fun flush() {
        val laneEnds = mutableListOf<Int>()
        val lanes = group.map { (_, s, e) ->
            val free = laneEnds.indexOfFirst { it <= s }
            if (free >= 0) { laneEnds[free] = e; free } else { laneEnds += e; laneEnds.lastIndex }
        }
        group.forEachIndexed { i, (t, s, e) -> result += PlacedBlock(t, s, e, lanes[i], laneEnds.size) }
        group = mutableListOf()
    }
    timed.forEach { block ->
        if (group.isNotEmpty() && block.second >= groupEnd) flush()
        groupEnd = if (group.isEmpty()) block.third else maxOf(groupEnd, block.third)
        group += block
    }
    if (group.isNotEmpty()) flush()
    return result
}

/** Huecos libres de al menos [minGap] minutos entre [from] y [to], ignorando lo ya hecho. */
internal fun freeGaps(tasks: List<Task>, from: Int, to: Int, minGap: Int = 60): List<IntRange> {
    val busy = tasks.filter { !it.isCompleted }
        .mapNotNull { t -> plannerTimeMinutes(t.time)?.let { s -> s to blockEnd(t)!! } }
        .sortedBy { it.first }
    val gaps = mutableListOf<IntRange>()
    var cursor = from
    busy.forEach { (s, e) ->
        if (s - cursor >= minGap) gaps += cursor until minOf(s, to)
        cursor = maxOf(cursor, e)
    }
    if (to - cursor >= minGap) gaps += cursor until to
    return gaps.filter { it.last + 1 - it.first >= minGap }
}

internal fun hhmm(minutes: Int): String = String.format(java.util.Locale.US, "%02d:%02d", minutes / 60 % 24, minutes % 60)
