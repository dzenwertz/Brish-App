package com.katchy.focuslive.ui.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NoteChecklistTest {
    @Test
    fun plainNoteStaysUntouched() {
        val content = "Ir temprano al mercado.\n\nLlevar bolsas."
        val body = parseNoteBody(content)
        assertFalse(body.isChecklist)
        assertEquals(content, serializeNoteBody(body))
    }

    @Test
    fun checklistRoundTrips() {
        val content = "☐ Arroz 5 kg\n☑ Huevos\n☐ Detergente\n\nIr temprano."
        val body = parseNoteBody(content)
        assertEquals(listOf(ChecklistItem("Arroz 5 kg", false), ChecklistItem("Huevos", true), ChecklistItem("Detergente", false)), body.items)
        assertEquals("Ir temprano.", body.text)
        assertEquals(1, body.doneCount)
        assertEquals(content, serializeNoteBody(body))
    }

    @Test
    fun enterAtTheEndIsKept() {
        // El editor vuelve a leer el texto en cada tecla: el salto de línea recién escrito no puede perderse.
        val plain = "Primera línea\n"
        assertEquals(plain, serializeNoteBody(parseNoteBody(plain)))
        val withList = "☐ Pan\n\nComprar temprano\n"
        assertEquals("Comprar temprano\n", parseNoteBody(withList).text)
        assertEquals(withList, serializeNoteBody(parseNoteBody(withList)))
    }

    @Test
    fun markdownCheckboxesAreRecognized() {
        val body = parseNoteBody("- [ ] Pan\n- [x] Leche")
        assertEquals(listOf(ChecklistItem("Pan", false), ChecklistItem("Leche", true)), body.items)
        assertEquals("☐ Pan\n☑ Leche", serializeNoteBody(body))
    }

    @Test
    fun reorderMovesItemsAndClampsTarget() {
        val items = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "c", "a", "d"), items.moved(0, 2))
        assertEquals(listOf("d", "a", "b", "c"), items.moved(3, 0))
        assertEquals(2, reorderTarget(from = 0, offsetPx = 230f, rowPx = 112f, size = 4))
        assertEquals(3, reorderTarget(from = 1, offsetPx = 9000f, rowPx = 112f, size = 4))
        assertEquals(0, reorderTarget(from = 2, offsetPx = -9000f, rowPx = 112f, size = 4))
    }
}
