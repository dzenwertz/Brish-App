package com.katchy.focuslive.ui.notes

import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.repository.CourseRepository
import com.katchy.focuslive.data.repository.NoteRepository
import com.katchy.focuslive.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesWritesTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val repository = mockk<NoteRepository>()
    private val courses = mockk<CourseRepository>()
    @Before fun setup() {
        every { repository.getNotes() } returns flowOf(emptyList())
        every { repository.getDrafts() } returns flowOf(emptyList())
        every { repository.getCategories() } returns flowOf(emptyList())
        every { repository.getTrashNotes() } returns flowOf(emptyList())
        every { courses.getCourses() } returns flowOf(emptyList())
    }
    @Test fun `repeated folder save cannot create duplicates while write is pending`() = runTest {
        coEvery { repository.addCategory(any()) } coAnswers { kotlinx.coroutines.delay(10); Unit }
        val vm = NotesViewModel(repository, courses)
        var callbacks = 0
        vm.addCategory("Folder") { callbacks++ }
        vm.addCategory("Folder") { callbacks++ }
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.addCategory(any()) }
        assertEquals(1, callbacks)
        assertFalse(vm.busy.value)
    }
    @Test fun `folder failure does not publish success`() = runTest {
        coEvery { repository.addCategory(any()) } throws IllegalStateException("Storage error")
        val vm = NotesViewModel(repository, courses)
        vm.addCategory("Folder") { fail("Cannot report success") }
        runCurrent()
        assertEquals("Storage error", vm.error.value)
        assertFalse(vm.busy.value)
    }
    @Test fun `failed permanent deletion keeps confirmation open`() = runTest {
        coEvery { repository.deletePermanently("note") } returns Result.failure(IllegalStateException("Storage error"))
        val vm = NotesViewModel(repository, courses)
        vm.deletePermanentlyFromTrash("note") { fail("Cannot close after failure") }
        runCurrent()
        assertEquals("Storage error", vm.error.value)
    }
    @Test fun `clearing cards reads latest note content and only closes after success`() = runTest {
        val old = Note(id = "note", content = "old", revision = 1)
        val current = old.copy(content = "new", revision = 2)
        coEvery { repository.getNote("note") } returns current
        coEvery { repository.updateNote(any()) } returns Result.success(Unit)
        val vm = NotesViewModel(repository, courses)
        var closed = false
        vm.clearFlashcards(old) { closed = true }
        runCurrent()
        assertTrue(closed)
        coVerify { repository.updateNote(match { it.content == "new" && it.revision == 2L }) }
    }
}
