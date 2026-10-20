package com.katchy.focuslive.ui.capture

import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.Calendar

enum class CaptureType(val label: String) {
    TASK("Tarea"), EVENT("Evento"), NOTE("Nota"), HABIT("Hábito")
}

/** Lo que Brish entendió de una frase de captura. Los días repetidos usan las constantes de Calendar. */
data class CaptureDraft(
    val type: CaptureType,
    val title: String,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val area: String? = null,
    val repeatDays: List<Int> = emptyList(),
    val daily: Boolean = false,
    val timesPerDay: Int? = null
)

private val Areas = mapOf("trabajo" to "Trabajo", "salud" to "Salud", "personal" to "Personal", "hogar" to "Hogar", "casa" to "Hogar")

private val FullDays = mapOf(
    "lunes" to DayOfWeek.MONDAY, "martes" to DayOfWeek.TUESDAY, "miercoles" to DayOfWeek.WEDNESDAY,
    "jueves" to DayOfWeek.THURSDAY, "viernes" to DayOfWeek.FRIDAY, "sabado" to DayOfWeek.SATURDAY,
    "domingo" to DayOfWeek.SUNDAY
)
private val ShortDays = mapOf(
    "lun" to DayOfWeek.MONDAY, "mar" to DayOfWeek.TUESDAY, "mie" to DayOfWeek.WEDNESDAY,
    "jue" to DayOfWeek.THURSDAY, "vie" to DayOfWeek.FRIDAY, "sab" to DayOfWeek.SATURDAY,
    "dom" to DayOfWeek.SUNDAY
)
private val Months = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** Quita tildes conservando la longitud, para que los índices del texto normalizado sirvan en el original. */
private fun fold(text: String): String = text.map { c ->
    Normalizer.normalize(c.toString(), Normalizer.Form.NFD).firstOrNull() ?: c
}.joinToString("").lowercase()

private fun DayOfWeek.toCalendarDay(): Int = when (this) {
    DayOfWeek.SUNDAY -> Calendar.SUNDAY
    DayOfWeek.MONDAY -> Calendar.MONDAY
    DayOfWeek.TUESDAY -> Calendar.TUESDAY
    DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
    DayOfWeek.THURSDAY -> Calendar.THURSDAY
    DayOfWeek.FRIDAY -> Calendar.FRIDAY
    DayOfWeek.SATURDAY -> Calendar.SATURDAY
}

/**
 * Interpreta frases como «pagar la luz el viernes 6pm #hogar», «gym lun mie vie 5:30pm»,
 * «idea: video de reparación» o «tomar agua 8 veces al día». [forced] fija el tipo elegido a mano.
 */
fun parseCapture(input: String, today: LocalDate, forced: CaptureType? = null): CaptureDraft {
    val text = input.trim()
    val low = fold(text)
    val removed = BooleanArray(text.length)
    fun cut(range: IntRange) { for (i in range) if (i in removed.indices) removed[i] = true }
    // Solo cuentan coincidencias sobre texto aún no usado («de la mañana» no puede ser también «mañana»).
    fun free(match: MatchResult) = match.range.none { removed[it] }
    fun findAll(regex: String) = Regex(regex).findAll(low).filter(::free).toList()
    fun find(regex: String) = findAll(regex).firstOrNull()

    var type: CaptureType? = null
    find("^\\s*(idea|nota|apunte)\\s*:\\s*")?.let { type = CaptureType.NOTE; cut(it.range) }
    find("^\\s*habito\\s*:\\s*")?.let { type = CaptureType.HABIT; cut(it.range) }

    var timesPerDay: Int? = null
    find("(\\d{1,2})\\s*veces\\s*(al|por)\\s*dia")?.let {
        timesPerDay = it.groupValues[1].toInt()
        if (type == null) type = CaptureType.HABIT
        cut(it.range)
    }

    var area: String? = null
    find("#(trabajo|salud|personal|hogar|casa)\\b")?.let { area = Areas[it.groupValues[1]]; cut(it.range) }

    var daily = false
    find("\\b(todos los dias|cada dia|a diario|diario)\\b")?.let { daily = true; cut(it.range) }

    // Hora: primero las que llevan am/pm o "de la tarde", luego 24 h, luego "a las 7".
    var time: LocalTime? = null
    var periodHint: String? = null
    find("\\bde la (manana|tarde|noche)\\b")?.let { periodHint = it.groupValues[1]; cut(it.range) }
    val withMeridiem = find("\\b(?:a las?\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(a\\.?\\s?m\\.?|p\\.?\\s?m\\.?)(?=\\s|$|[,.;])")
    val twentyFour = find("\\b(?:a las?\\s+)?([01]?\\d|2[0-3]):([0-5]\\d)\\b")
    val bare = find("\\ba las?\\s+(\\d{1,2})\\b")
    when {
        withMeridiem != null -> {
            var hour = withMeridiem.groupValues[1].toInt()
            val minute = withMeridiem.groupValues[2].toIntOrNull() ?: 0
            val pm = withMeridiem.groupValues[3].startsWith("p")
            if (hour in 1..12 && minute in 0..59) {
                if (pm && hour < 12) hour += 12
                if (!pm && hour == 12) hour = 0
                time = LocalTime.of(hour, minute)
                cut(withMeridiem.range)
            }
        }
        twentyFour != null -> {
            var hour = twentyFour.groupValues[1].toInt()
            if (periodHint in setOf("tarde", "noche") && hour in 1..11) hour += 12
            time = LocalTime.of(hour, twentyFour.groupValues[2].toInt())
            cut(twentyFour.range)
        }
        bare != null -> {
            var hour = bare.groupValues[1].toInt()
            if (hour in 0..23) {
                val evening = periodHint in setOf("tarde", "noche") || (periodHint == null && hour in 1..6)
                if (evening && hour in 1..11) hour += 12
                time = LocalTime.of(hour, 0)
                cut(bare.range)
            }
        }
    }

    var date: LocalDate? = null
    find("\\bpasado manana\\b")?.let { date = today.plusDays(2); cut(it.range) }
    if (date == null) find("\\bmanana\\b")?.let { date = today.plusDays(1); cut(it.range) }
    if (date == null) find("\\bhoy\\b")?.let { date = today; cut(it.range) }
    if (date == null) {
        val months = Months.joinToString("|")
        find("\\b(\\d{1,2})\\s*(?:de\\s+)?($months)[a-z]*\\.?")?.let {
            val day = it.groupValues[1].toInt()
            val month = Months.indexOf(it.groupValues[2]) + 1
            runCatching { LocalDate.of(today.year, month, day) }.getOrNull()?.let { d ->
                date = if (d.isBefore(today)) d.plusYears(1) else d
                cut(it.range)
            }
        }
    }
    if (date == null) {
        find("\\b(\\d{1,2})/(\\d{1,2})\\b")?.let {
            runCatching { LocalDate.of(today.year, it.groupValues[2].toInt(), it.groupValues[1].toInt()) }.getOrNull()?.let { d ->
                date = if (d.isBefore(today)) d.plusYears(1) else d
                cut(it.range)
            }
        }
    }

    // Días de la semana: los nombres completos siempre cuentan; las abreviaturas solo en listas («lun mie vie»).
    val full = findAll("\\b(?:(el|los|cada)\\s+)?(lunes|martes|miercoles|jueves|viernes|sabado|domingo)\\b")
    val short = findAll("\\b(lun|mar|mie|jue|vie|sab|dom)\\b")
    val dayMatches = full.map { it to FullDays.getValue(it.groupValues[2]) } +
        (if (short.size >= 2) short.map { it to ShortDays.getValue(it.groupValues[1]) } else emptyList())
    val days = dayMatches.map { it.second }.distinct()
    val plural = full.any { it.groupValues[1] in setOf("los", "cada") }
    var repeatDays = emptyList<Int>()
    if (days.size >= 2 || (days.size == 1 && plural)) {
        repeatDays = days.sortedBy { it.value }.map { it.toCalendarDay() }
        dayMatches.forEach { cut(it.first.range) }
    } else if (days.size == 1 && date == null) {
        date = today.with(TemporalAdjusters.next(days.first()))
        dayMatches.forEach { cut(it.first.range) }
    }

    val resolved = forced ?: type ?: if (time != null) CaptureType.EVENT else CaptureType.TASK
    val title = text.indices.filterNot { removed[it] }.map { text[it] }.joinToString("")
        .replace(Regex("(?i)\\b(el|la|los|las|a las|para el|para|a)\\s*$"), "")
        .replace(Regex("(?i)^\\s*(el|la|para)\\s+"), "")
        .replace(Regex("\\s+"), " ")
        .trim(' ', ',', '.', ';', '-', ':')
        .replaceFirstChar { it.uppercase() }

    return CaptureDraft(
        type = resolved,
        title = title,
        date = date,
        time = time,
        area = area,
        repeatDays = if (daily) emptyList() else repeatDays,
        daily = daily,
        timesPerDay = timesPerDay
    )
}
