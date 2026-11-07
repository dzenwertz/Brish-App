package com.katchy.focuslive.ui.notes

/** Un elemento de lista dentro de una nota. Se guarda como línea «☐ texto» o «☑ texto». */
data class ChecklistItem(val text: String, val checked: Boolean)

/** El contenido de una nota separado en su lista y su texto libre. */
data class NoteBody(val items: List<ChecklistItem>, val text: String) {
    val isChecklist: Boolean get() = items.isNotEmpty()
    val doneCount: Int get() = items.count { it.checked }
}

private val Unchecked = Regex("^\\s*(☐|- \\[ \\])\\s?")
private val Checked = Regex("^\\s*(☑|✓|- \\[[xX]\\])\\s?")

fun parseNoteBody(content: String): NoteBody {
    val items = mutableListOf<ChecklistItem>()
    val text = mutableListOf<String>()
    content.lines().forEach { line ->
        when {
            Checked.containsMatchIn(line) -> items += ChecklistItem(line.replace(Checked, ""), true)
            Unchecked.containsMatchIn(line) -> items += ChecklistItem(line.replace(Unchecked, ""), false)
            else -> text += line
        }
    }
    // Solo se quita la línea en blanco que separa la lista del texto. Recortar el salto final
    // borraba cada Enter pulsado al final de la nota, porque este texto se vuelve a leer en cada tecla.
    val joined = text.joinToString("\n")
    return NoteBody(items, if (items.isEmpty()) joined else joined.trimStart('\n'))
}

/** Lista primero y luego el texto, separados por una línea en blanco. Una nota sin lista queda igual. */
fun serializeNoteBody(body: NoteBody): String {
    val list = body.items.joinToString("\n") { (if (it.checked) "☑ " else "☐ ") + it.text }
    return when {
        body.items.isEmpty() -> body.text
        body.text.isBlank() -> list
        else -> list + "\n\n" + body.text
    }
}

/** Posición a la que cae un elemento arrastrado [offsetPx] desde [from], con filas de [rowPx] de alto. */
fun reorderTarget(from: Int, offsetPx: Float, rowPx: Float, size: Int): Int =
    (from + kotlin.math.round(offsetPx / rowPx).toInt()).coerceIn(0, (size - 1).coerceAtLeast(0))

/** Mueve el elemento de [from] a [to] manteniendo el orden del resto. */
fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from == to || from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}
