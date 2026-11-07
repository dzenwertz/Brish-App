package com.katchy.focuslive.ui.courses

import java.util.Calendar

data class DeliverableDraft(
    val title: String,
    val description: String = "",
    val date: Long? = null,
    val time: String? = null
) {
    fun validationError(): String? = when {
        title.isBlank() -> "Escribe un título."
        time != null && date == null -> "Selecciona una fecha antes de indicar una hora."
        time != null && deliveryTimeMinutes(time) == null -> "La hora debe tener formato HH:mm."
        else -> null
    }
}

internal fun deliveryTimeMinutes(time: String): Int? {
    if (!Regex("[0-9]{2}:[0-9]{2}").matches(time)) return null
    val hour = time.substring(0, 2).toInt()
    val minute = time.substring(3, 5).toInt()
    return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
}

internal fun deliveryDueMillis(date: Long, time: String?): Long = Calendar.getInstance().apply {
    timeInMillis = date
    val minutes = time?.let { requireNotNull(deliveryTimeMinutes(it)) }
    set(Calendar.HOUR_OF_DAY, minutes?.div(60) ?: 0)
    set(Calendar.MINUTE, minutes?.rem(60) ?: 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
