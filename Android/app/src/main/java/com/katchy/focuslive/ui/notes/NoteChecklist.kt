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
    return NoteBody(items, text.joinToString("\n").trim('\n'))
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
