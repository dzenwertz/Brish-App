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

/** Minutos desde medianoche de una hora «HH:mm»; null si no es válida. */
internal fun minutesOf(time: String?): Int? {
    val parts = time?.split(':')?.takeIf { it.size == 2 } ?: return null
    val h = parts[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
    val m = parts[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
    return h * 60 + m
}

/** Fin de un bloque: su hora de término o, si no tiene, una hora después del inicio. */
internal fun endMinutesOf(task: Task): Int? {
    val start = minutesOf(task.time) ?: return null
    return minutesOf(task.endTime)?.takeIf { it > start } ?: (start + 60).coerceAtMost(24 * 60)
}

/** Lo que muestra Hoy: el bloque de ahora, lo que sigue, lo pendiente y lo ya hecho. */
data class TodayPlan(
    val current: Task? = null,
    val currentProgress: Float = 0f,
    val minutesLeft: Int = 0,
    val next: List<Task> = emptyList(),
    val pending: List<Task> = emptyList(),
    val done: List<Task> = emptyList()
)

internal fun planToday(
    agenda: List<Task>,
    nowMinutes: Int,
    nowSecondOfMinute: Int = 0,
    todayMillis: Long? = null
): TodayPlan {
    // Una tarea sin hacer de un día anterior se arrastra a Hoy como pendiente: su hora era de ese día,
    // así que nunca cuenta como "ahora" ni como "después".
    val todayStart = todayMillis?.let(::dayStart)
    fun carried(t: Task) = todayStart != null && t.repeatMode == "NONE" && dayStart(t.timestamp) < todayStart
    val open = agenda.filter { !it.isCompleted }
    val schedulable = open.filterNot(::carried)
    val current = schedulable
        .filter { t -> minutesOf(t.time)?.let { s -> s <= nowMinutes && nowMinutes < (endMinutesOf(t) ?: s) } == true }
        .minByOrNull { minutesOf(it.time)!! }
    val next = schedulable.filter { it.id != current?.id && (minutesOf(it.time) ?: -1) > nowMinutes }
        .sortedBy { minutesOf(it.time) }
    // Sin hora, o con hora que ya pasó sin marcarse: siguen pendientes hoy.
    val pending = open.filter { it.id != current?.id && it !in next }
        .sortedWith(compareBy<Task> { minutesOf(it.time) == null }.thenBy { minutesOf(it.time) ?: 0 })
    val exactMinutes = nowMinutes + (nowSecondOfMinute.coerceIn(0, 59) / 60f)
    val progress = current?.let {
        val s = minutesOf(it.time)!!
        val e = endMinutesOf(it)!!
        ((exactMinutes - s) / (e - s)).coerceIn(0f, 1f)
    } ?: 0f
    val left = current?.let { (endMinutesOf(it)!! - nowMinutes).coerceAtLeast(0) } ?: 0
    return TodayPlan(current, progress, left, next, pending, agenda.filter { it.isCompleted })
}

/** Posponer una tarea para mañana conservando su hora y demás atributos. */
internal fun postponeTaskToTomorrow(task: Task, todayMillis: Long): Task {
    val tomorrow = Calendar.getInstance().apply {
        timeInMillis = dayStart(todayMillis)
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
    return task.copy(
        timestamp = tomorrow,
        dueDate = tomorrow,
        hasDueDate = true,
        isMyDay = false
    )
}

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
