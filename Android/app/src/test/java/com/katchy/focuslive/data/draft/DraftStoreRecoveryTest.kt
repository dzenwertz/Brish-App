package com.katchy.focuslive.data.draft

import com.katchy.focuslive.data.repository.HabitRepository
import com.katchy.focuslive.data.repository.NoteRepository
import com.katchy.focuslive.data.repository.TaskRepository
import com.katchy.focuslive.scheduler.NotificationScheduler
import com.katchy.focuslive.ui.capture.CaptureType
import com.katchy.focuslive.ui.capture.CaptureViewModel
import com.katchy.focuslive.ui.capture.SaveState
import com.katchy.focuslive.ui.components.PersistentDraftText
import com.katchy.focuslive.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import java.io.File
import java.util.Properties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DraftStoreRecoveryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun captureViewModelRestoresInputAndTypeAfterAbruptCloseAndClearsOnSave() = runTest {
        val file = temporaryFile()
        val tasks = mockk<TaskRepository>(relaxed = true)
        val notes = mockk<NoteRepository>(relaxed = true)
        val habits = mockk<HabitRepository>(relaxed = true)
        val scheduler = mockk<NotificationScheduler>(relaxed = true)
        coEvery { notes.addNote(any()) } returns Result.success(Unit)

        // First process: user types and selects NOTE, then process closes abruptly after lifecycle flush.
        val firstStore = newStore(FilePersistence(file))
        val firstVm = CaptureViewModel(tasks, notes, habits, scheduler, firstStore)
        runCurrent()
        firstVm.onTextChange("dentista mañana 5 pm")
        firstVm.onTypeChange(CaptureType.NOTE)
        firstVm.flushDraft()

        // Second process: recreated over the same disk file.
        val secondStore = newStore(FilePersistence(file))
        val secondVm = CaptureViewModel(tasks, notes, habits, scheduler, secondStore)
        runCurrent()
        assertEquals("dentista mañana 5 pm", secondVm.state.value.text)
        assertEquals(CaptureType.NOTE, secondVm.state.value.forcedType)
        assertTrue(secondVm.state.value.recovered)

        // Saving clears the draft from disk and completes after the checkmark delay.
        var completed = false
        secondVm.save { completed = true }
        runCurrent()
        assertEquals(SaveState.Saved, secondVm.state.value.save)
        assertNull(secondStore.read("capture:input"))
        assertNull(secondStore.read("capture:type"))
        advanceTimeBy(700)
        runCurrent()
        assertTrue(completed)
        assertEquals("", secondVm.state.value.text)
    }

    @Test
    fun plannerTaskAndSubtaskRestoreAfterProcessRecreation() = runBlocking {
        survivesFormHolder("planner:add:title", "entregar informe")
        survivesFormHolder("planner:add:subtask:draft", "imprimir anexos")
        survivesFormHolder("planner:detail:task-42:subtask", "revisar rúbrica")
    }

    @Test
    fun habitFormRestoresAfterProcessRecreation() = runBlocking {
        survivesFormHolder("habit:new:title", "caminar 20 min")
    }

    @Test
    fun courseSpaceAndDeliverableFormsRestoreAfterProcessRecreation() = runBlocking {
        survivesFormHolder("course:new:name", "Biología Celular")
        survivesFormHolder("space:new:name", "Ciclo 2026-2")
        survivesFormHolder("deliverable:course-1:new:title", "Informe de laboratorio")
    }

    @Test
    fun flashcardFrontAndBackRestoreAfterProcessRecreation() = runBlocking {
        survivesFormHolder("flashcard:note-1:front", "¿Qué es una mitocondria?")
        survivesFormHolder("flashcard:note-1:back", "Orgánulo encargado de producir ATP.")
    }

    @Test
    fun weeklyReviewReflectionRestoresAfterProcessRecreation() = runBlocking {
        survivesFormHolder("weekly-review:answer:week-1", "Avancé en mis cursos y cumplí mis bloques.")
    }

    @Test
    fun settingsAndOnboardingNamesRestoreAfterProcessRecreation() = runBlocking {
        survivesFormHolder("settings:display-name", "Joseph", initialValue = "Usuario")
        survivesFormHolder("onboarding:name", "Joseph")
    }

    @Test
    fun clearAfterSavePreventsDisposeFlushFromResurrectingDraft() = runBlocking {
        val file = temporaryFile()
        val firstStore = newStore(FilePersistence(file))
        val holder = PersistentDraftText("course:new:name", "", firstStore)
        holder.hydrateBlocking()
        holder.value = "Física II"
        holder.flushIfDirty()

        // User saves the dialog and the Composable disposes immediately after.
        holder.clearAfterSave()
        holder.flushIfDirty()

        val reopenedStore = newStore(FilePersistence(file))
        assertNull(reopenedStore.read("course:new:name"))
    }

    @Test
    fun uneditedFormDoesNotWriteInitialValueToDraftStoreOnClose() = runBlocking {
        val file = temporaryFile()
        val store = newStore(FilePersistence(file))
        val holder = PersistentDraftText("settings:display-name", "Nombre actual", store)
        holder.hydrateBlocking()
        holder.flushIfDirty()

        val reopenedStore = newStore(FilePersistence(file))
        assertNull(reopenedStore.read("settings:display-name"))
    }

    @Test
    fun loginRestoresEmailButNeverStoresPassword() = runBlocking {
        val file = temporaryFile()
        val firstStore = newStore(FilePersistence(file))
        val emailHolder = PersistentDraftText("login:email", "", firstStore)
        emailHolder.hydrateBlocking()
        emailHolder.value = "joseph@example.com"
        emailHolder.flushIfDirty()

        val reopenedStore = newStore(FilePersistence(file))
        val reopenedEmail = PersistentDraftText("login:email", "", reopenedStore)
        reopenedEmail.hydrateBlocking()
        assertEquals("joseph@example.com", reopenedEmail.value)
        assertTrue(reopenedEmail.recovered)
        assertNull(reopenedStore.read("login:password"))
    }

    private fun survivesFormHolder(key: String, input: String, initialValue: String = "") {
        val file = temporaryFile()
        val firstStore = newStore(FilePersistence(file))
        val firstHolder = PersistentDraftText(key, initialValue, firstStore)
        firstHolder.hydrateBlocking()
        firstHolder.value = input
        firstHolder.flushIfDirty() // Simulates ON_PAUSE / ON_STOP / onDispose flush before process death.

        val reopenedStore = newStore(FilePersistence(file))
        val reopenedHolder = PersistentDraftText(key, initialValue, reopenedStore)
        reopenedHolder.hydrateBlocking()
        assertEquals(input, reopenedHolder.value)
        assertTrue("Expected recovered=true for key=$key", reopenedHolder.recovered)
    }

    private fun newStore(persistence: DraftPersistence) = DraftStore(
        persistence,
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )

    private fun temporaryFile(): File = File.createTempFile("brish-drafts-", ".properties").apply { deleteOnExit() }

    private class FilePersistence(private val file: File) : DraftPersistence {
        override suspend fun read(key: String): String? = synchronized(file.absolutePath.intern()) {
            if (!file.exists()) null else Properties().run { file.inputStream().use(::load); getProperty(key) }
        }

        override suspend fun write(values: Map<String, String?>) = synchronized(file.absolutePath.intern()) {
            val properties = Properties().apply { if (file.exists()) file.inputStream().use(::load) }
            values.forEach { (key, value) -> if (value == null) properties.remove(key) else properties.setProperty(key, value) }
            file.outputStream().use { properties.store(it, "Brish drafts test fixture") }
        }
    }
}
