package com.katchy.focuslive.ui.notes

import androidx.lifecycle.SavedStateHandle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.repository.CourseRepository
import com.katchy.focuslive.data.repository.NoteRepository
import com.katchy.focuslive.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val repository = mockk<NoteRepository>(relaxed = true)
    private val courses = mockk<CourseRepository>(relaxed = true)
    private val auth = mockk<FirebaseAuth>()
    private val user = mockk<FirebaseUser>()
    private var stored: Note? = null
    @Before fun setup() {
        every { user.uid } returns "owner"
        every { auth.currentUser } returns user
        every { repository.getCategories() } returns flowOf(emptyList())
        every { courses.getCourses() } returns flowOf(emptyList())
        coEvery { repository.getNote(any()) } answers { stored }
        coEvery { repository.getDraft(any()) } returns null
        coEvery { repository.saveDraft(any()) } returns Result.success(Unit)
        coEvery { repository.clearDraft(any()) } returns Result.success(Unit)
        coEvery { repository.addNote(any()) } answers { stored = firstArg<Note>().copy(revision = 1); Result.success(Unit) }
        coEvery { repository.updateNote(any()) } answers {
            val note = firstArg<Note>()
            if (note.revision != stored?.revision) Result.failure(IllegalStateException("Revision mismatch"))
            else { stored = note.copy(revision = note.revision + 1); Result.success(Unit) }
        }
    }
    private fun editor(vararg args: Pair<String, String>) = NoteEditorViewModel(SavedStateHandle(mapOf(*args)), repository, courses, auth)

    @Test fun `new note inherits folder course and stable id without creating empty record`() = runTest {
        val vm = editor("folderId" to "folder", "courseId" to "course")
        runCurrent()
        assertEquals("folder", vm.state.value.note.categoryId)
        assertEquals("course", vm.state.value.note.courseId)
        var closed = false
        vm.close { closed = true }
        runCurrent()
        assertTrue(closed)
        coVerify(exactly = 0) { repository.addNote(any()) }
    }
    @Test fun `typing saves durable draft immediately and note after 500 ms`() = runTest {
        val vm = editor()
        runCurrent()
        val id = vm.state.value.note.id
        vm.change { it.copy(content = "First paragraph") }
        runCurrent()
        coVerify { repository.saveDraft(match { it.content == "First paragraph" && it.id == id }) }
        coVerify(exactly = 0) { repository.addNote(any()) }
        advanceTimeBy(500); runCurrent()
        assertEquals("First paragraph", stored?.content)
        assertEquals("Guardado en este dispositivo", vm.state.value.status)
    }
    @Test fun `consecutive autosaves use acknowledged revision and do not duplicate note`() = runTest {
        val vm = editor()
        runCurrent()
        vm.change { it.copy(content = "one") }
        advanceTimeBy(500); runCurrent()
        vm.change { it.copy(content = "two") }
        advanceTimeBy(500); runCurrent()
        assertEquals("two", stored?.content)
        assertEquals(2L, stored?.revision)
        coVerify(exactly = 1) { repository.addNote(any()) }
        coVerify(exactly = 1) { repository.updateNote(any()) }
    }
    @Test fun `back flushes without waiting for debounce`() = runTest {
        val vm = editor("folderId" to "folder")
        runCurrent()
        vm.change { it.copy(content = "Content without title") }
        var closed = false
        vm.close { closed = true }
        runCurrent()
        assertTrue(closed)
        assertEquals("Content without title", stored?.content)
        assertEquals("folder", stored?.categoryId)
    }
    @Test fun `failed write retains text and draft and prevents closing until retry`() = runTest {
        coEvery { repository.addNote(any()) } returns Result.failure(IllegalStateException("Disk full"))
        val vm = editor()
        runCurrent()
        vm.change { it.copy(content = "Keep me") }
        var closed = false
        vm.close { closed = true }
        runCurrent()
        assertFalse(closed)
        assertEquals("Keep me", vm.state.value.note.content)
        assertEquals("No se pudo guardar", vm.state.value.status)
        coVerify(exactly = 0) { repository.clearDraft(any()) }
        coEvery { repository.addNote(any()) } answers { stored = firstArg<Note>().copy(revision = 1); Result.success(Unit) }
        vm.close { closed = true }; runCurrent()
        assertTrue(closed)
    }
    @Test fun `opening saved note from course retains same id course and folder`() = runTest {
        stored = Note(id = "note", content = "old", courseId = "course", categoryId = "folder", userId = "owner", revision = 4)
        val vm = editor("noteId" to "note")
        runCurrent()
        vm.change { it.copy(content = "updated", categoryId = "other") }
        vm.close { }; runCurrent()
        assertEquals("note", stored?.id)
        assertEquals("course", stored?.courseId)
        assertEquals("other", stored?.categoryId)
        coVerify(exactly = 0) { repository.addNote(any()) }
    }
    @Test fun `account change refuses writes to new account`() = runTest {
        val vm = editor(); runCurrent()
        vm.change { it.copy(content = "belongs to owner") }; runCurrent()
        every { auth.currentUser } returns null
        vm.close { fail("Must not close as saved") }; runCurrent()
        coVerify(exactly = 0) { repository.addNote(any()) }
        assertEquals("No se pudo guardar", vm.state.value.status)
    }
    @Test fun `reopening recovers draft content and current stored revision`() = runTest {
        stored = Note(id = "note", userId = "owner", content = "saved", revision = 7)
        coEvery { repository.getDraft("note") } returns stored!!.copy(content = "recovered", revision = 6)
        val vm = editor("noteId" to "note"); runCurrent()
        assertEquals("recovered", vm.state.value.note.content)
        assertEquals(7L, vm.state.value.note.revision)
        vm.close {}; runCurrent()
        assertEquals("recovered", stored?.content)
        assertEquals(8L, stored?.revision)
    }
    @Test fun `failed draft write keeps content and prevents successful close`() = runTest {
        coEvery { repository.saveDraft(any()) } returns Result.failure(IllegalStateException("Storage unavailable"))
        val vm = editor(); runCurrent()
        vm.change { it.copy(content = "still here") }
        vm.close { fail("Draft failure must not close the editor") }; runCurrent()
        assertEquals("still here", vm.state.value.note.content)
        assertEquals("No se pudo guardar", vm.state.value.status)
        coVerify(exactly = 0) { repository.addNote(any()) }
    }
}
