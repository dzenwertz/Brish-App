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
    fun markdownCheckboxesAreRecognized() {
        val body = parseNoteBody("- [ ] Pan\n- [x] Leche")
        assertEquals(listOf(ChecklistItem("Pan", false), ChecklistItem("Leche", true)), body.items)
        assertEquals("☐ Pan\n☑ Leche", serializeNoteBody(body))
    }
}
