package com.katchy.focuslive.scheduler

import com.katchy.focuslive.data.model.Task
import java.util.Calendar

/** The next incomplete occurrence strictly after now, in the device's current timezone. */
internal fun nextTaskReminderAt(task: Task, now: Long): Long? {
    if (!task.hasDueDate) return null
    val time = task.time ?: return null
    if (!time.matches(Regex("\\d{1,2}:\\d{2}"))) return null
    val parts = time.split(':')
    val hour = parts[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
    val minute = parts[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
    val start = when {
        isDeliveryReminder(task) -> task.dueDate
        task.kind == "AGENDA" -> task.timestamp
        else -> return null
    }
    val startDay = reminderDay(start)
    val repeatDays = when (task.repeatMode) {
        "NONE" -> {
            if (task.isCompleted) return null
            emptySet()
        }
        "DAILY" -> (Calendar.SUNDAY..Calendar.SATURDAY).toSet()
        "WEEKLY", "CUSTOM" -> {
            if (task.repeatDays.isEmpty() || task.repeatDays.any { it !in Calendar.SUNDAY..Calendar.SATURDAY }) {
                return null
            }
            task.repeatDays.toSet()
        }
        else -> return null
    }
    val completedDays = task.completedDates.map(::reminderDay).toSet()
    val candidate = Calendar.getInstance().apply {
        timeInMillis = if (task.repeatMode == "NONE") startDay else maxOf(startDay, reminderDay(now))
    }
    while (true) {
        // Reset wall-clock time daily so a DST gap does not shift subsequent occurrences.
        candidate.set(Calendar.HOUR_OF_DAY, hour)
        candidate.set(Calendar.MINUTE, minute)
        candidate.set(Calendar.SECOND, 0)
        candidate.set(Calendar.MILLISECOND, 0)
        val occursToday = task.repeatMode == "NONE" || candidate.get(Calendar.DAY_OF_WEEK) in repeatDays
        if (occursToday && candidate.timeInMillis > now && reminderDay(candidate.timeInMillis) !in completedDays) {
            return candidate.timeInMillis
        }
        if (task.repeatMode == "NONE") return null
        candidate.add(Calendar.DAY_OF_YEAR, 1)
    }
}

internal fun isDeliveryReminder(task: Task): Boolean = task.kind == "DELIVERY" ||
    !task.courseId.isNullOrBlank() || !task.deliverableType.isNullOrBlank()

internal fun taskReminderDestination(task: Task): String = if (isDeliveryReminder(task)) "courses" else "planner"

private fun reminderDay(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
