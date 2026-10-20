package com.katchy.focuslive.ui.planner

internal fun plannerTimeMinutes(time: String?): Int? {
    if (time == null || !Regex("[0-9]{2}:[0-9]{2}").matches(time)) return null
    val hour = time.substring(0, 2).toInt()
    val minute = time.substring(3, 5).toInt()
    return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
}

internal fun plannerTimeError(start: String?, end: String?): String? {
    if (end != null && start == null) return "Elige primero una hora de inicio."
    if (start != null && plannerTimeMinutes(start) == null) return "La hora de inicio debe tener formato HH:mm."
    if (end != null && plannerTimeMinutes(end) == null) return "La hora de fin debe tener formato HH:mm."
    val startMinutes = plannerTimeMinutes(start)
    val endMinutes = plannerTimeMinutes(end)
    return if (startMinutes != null && endMinutes != null && endMinutes <= startMinutes) {
        "La hora de fin debe ser posterior al inicio en el mismo día."
    } else null
}
