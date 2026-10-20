package com.katchy.focuslive.ui.home

import com.katchy.focuslive.data.model.Course
import com.katchy.focuslive.data.model.LearningSpace
import com.katchy.focuslive.data.model.Task
import java.util.Calendar

/** Local calendar days keep recurring tasks correct across daylight-saving changes. */
internal fun dayStart(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

internal fun buildHomeState(
    tasks: List<Task>,
    courses: List<Course>,
    spaces: List<LearningSpace>,
    today: Long
): HomeViewModel.HomeState {
    val archivedIds = spaces.filter { it.isArchived }.map { it.id }.toSet()
    val activeCourses = courses.filter { it.spaceId !in archivedIds }.associateBy { it.id }
    val day = dayStart(today)
    val weekday = Calendar.getInstance().apply { timeInMillis = day }.get(Calendar.DAY_OF_WEEK)
    val oldest = Calendar.getInstance().apply { timeInMillis = day; add(Calendar.DAY_OF_YEAR, -14) }.timeInMillis
    val agenda = tasks.filter { task ->
        // Course deliveries have their own section and never duplicate agenda tasks.
        task.kind == "AGENDA" && task.courseId.isNullOrBlank() && task.deliverableType.isNullOrBlank() && if (!task.hasDueDate) task.isMyDay else {
            val start = dayStart(task.timestamp)
            val completedToday = task.completedDates.any { dayStart(it) == day }
            when (task.repeatMode) {
                "DAILY" -> day >= start
                "WEEKLY", "CUSTOM" -> day >= start && weekday in task.repeatDays
                else -> start == day || completedToday ||
                    (!task.isCompleted && start in oldest until day) || task.isMyDay
            }
        }
    }.map { task ->
        if (task.repeatMode == "NONE") task
        else task.copy(
            isCompleted = task.completedDates.any { dayStart(it) == day },
            subtasks = task.subtasks.map { subtask ->
                subtask.copy(isCompleted = subtask.completedDates.any { dayStart(it) == day })
            }
        )
    }.sortedWith(compareBy<Task> { it.isCompleted }.thenBy { it.time ?: "23:59" })
    val pendingDeliveries = tasks.filter {
        (it.kind == "DELIVERY" || !it.courseId.isNullOrBlank()) && !it.isCompleted && it.courseId in activeCourses
    }
    val deliveries = pendingDeliveries.filter { it.hasDueDate }.sortedBy { it.dueDate }.take(3).map {
        HomeViewModel.UpcomingDelivery(it, activeCourses.getValue(it.courseId!!).name)
    }
    val pendingAgenda = tasks.filter {
        it.kind == "AGENDA" && it.courseId.isNullOrBlank() && it.deliverableType.isNullOrBlank()
    }.map {
        if (it.repeatMode == "NONE") it
        else it.copy(isCompleted = it.completedDates.any { completed -> dayStart(completed) == day })
    }.filter { !it.isCompleted }
    val focusTasks = (agenda.filter { !it.isCompleted } + pendingAgenda + pendingDeliveries
        .sortedWith(compareBy<Task> { !it.hasDueDate }.thenBy { it.dueDate })).distinctBy { it.id }
    return HomeViewModel.HomeState(tasks = agenda, upcomingDeliveries = deliveries, focusTasks = focusTasks)
}
