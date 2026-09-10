package com.katchy.focuslive.ui.notes

import com.katchy.focuslive.data.model.FlashcardItem
import com.katchy.focuslive.data.model.MascotType
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.model.NoteCategory
import com.katchy.focuslive.data.repository.AppPreferencesRepository
import com.katchy.focuslive.data.repository.MascotRepository
import com.katchy.focuslive.data.repository.NoteRepository
import com.katchy.focuslive.data.repository.TaskRepository
import com.katchy.focuslive.data.repository.UserStatsRepository
import com.katchy.focuslive.data.service.AIService
import com.katchy.focuslive.util.MainDispatcherRule
import com.katchy.focuslive.util.OcrManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: NotesViewModel

    private val noteRepository: NoteRepository = mockk(relaxed = true)
    private val mascotRepository: MascotRepository = mockk(relaxed = true)
    private val appPreferencesRepository: AppPreferencesRepository = mockk(relaxed = true)
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val userStatsRepository: UserStatsRepository = mockk(relaxed = true)
    private val aiService: AIService = mockk(relaxed = true)
    private val ocrManager: OcrManager = mockk(relaxed = true)

    // Flows compartidos: combine() captura el flow al construir el ViewModel,
    // por eso exponemos MutableStateFlow mutables desde setup.
    private val notesState = MutableStateFlow<List<Note>>(emptyList())
    private val categoriesState = MutableStateFlow<List<NoteCategory>>(emptyList())

    @Before
    fun setup() {
        every { mascotRepository.selectedMascot } returns MutableStateFlow(MascotType.POPPIN)
        every { userStatsRepository.isPremium } returns MutableStateFlow(false)
        every { noteRepository.getNotes() } returns notesState
        every { noteRepository.getCategories() } returns categoriesState

        viewModel = NotesViewModel(
            noteRepository,
            mascotRepository,
            appPreferencesRepository,
            taskRepository,
            userStatsRepository,
            aiService,
            ocrManager
        )
    }

    private fun note(
        id: String,
        title: String,
        content: String = "",
        isPinned: Boolean = false,
        priority: String = "LOW",
        timestamp: Long = System.currentTimeMillis(),
        colorHex: String = "#FFFFFF"
    ) = Note(
        id = id, title = title, content = content, isPinned = isPinned,
        priority = priority, timestamp = timestamp, colorHex = colorHex
    )

    private fun collectNotes(scope: TestScope): List<Note> {
        val job = scope.backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.notes.collect { }
        }
        scope.advanceUntilIdle()
        runCatching { job.cancel() }
        scope.advanceUntilIdle()
        return viewModel.notes.value
    }

    private suspend fun awaitNonEmptyNotes(predicate: (List<Note>) -> Boolean): List<Note> {
        return viewModel.notes.first { predicate(it) }
    }

    @Test
    fun `searchQuery filtra notas por titulo y contenido`() = runTest {
        notesState.value = listOf(
            note("1", "Comprar leche"),
            note("2", "Estudiar Kotlin", content = "repaso de interfaces"),
            note("3", "Pasear al perro")
        )

        val all = awaitNonEmptyNotes { it.size == 3 }
        assertEquals(3, all.size)

        viewModel.setSearchQuery("kotlin")
        val filtered = awaitNonEmptyNotes { it.size == 1 }
        assertEquals(1, filtered.size)
        assertEquals("Estudiar Kotlin", filtered[0].title)
    }

    @Test
    fun `notas ordenadas pinned primero luego HIGH MEDIUM LOW y timestamp desc`() = runTest {
        notesState.value = listOf(
            note("a", "Low no pinned", priority = "LOW", timestamp = 1000),
            note("b", "High no pinned", priority = "HIGH", timestamp = 500),
            note("c", "Medium pinned", isPinned = true, priority = "MEDIUM", timestamp = 200),
            note("d", "High pinned", isPinned = true, priority = "HIGH", timestamp = 10),
            note("e", "Low no pinned 2", priority = "LOW", timestamp = 3000)
        )

        val sorted = awaitNonEmptyNotes { it.size == 5 }
        // Orden esperado: d (pinned HIGH), c (pinned MEDIUM), b (HIGH no pin),
        // e (LOW timestamp mayor), a (LOW timestamp menor)
        assertEquals(listOf("d", "c", "b", "e", "a"), sorted.map { it.id })
    }

    @Test
    fun `addNote con titulo y contenido incrementa contador de notas`() = runTest {
        viewModel.addNote(
            title = "Nueva",
            content = "contenido",
            colorHex = "#FFFFFF",
            isPinned = false,
            priority = "MEDIUM",
            categoryId = null
        )

        coVerify { noteRepository.addNote(any()) }
        coVerify { userStatsRepository.incrementNotes() }
    }

    @Test
    fun `addNote con titulo y contenido vacios no hace nada`() = runTest {
        viewModel.addNote(
            title = "",
            content = "",
            colorHex = "#FFFFFF",
            isPinned = false,
            priority = "MEDIUM",
            categoryId = null
        )

        coVerify(exactly = 0) { noteRepository.addNote(any()) }
        coVerify(exactly = 0) { userStatsRepository.incrementNotes() }
    }

    @Test
    fun `convertToTask crea una Task con el titulo de la nota y elimina la nota`() = runTest {
        val n = note("1", "Recordar llamar", content = "Contenido extra")
        viewModel.convertToTask(n)

        coVerify {
            taskRepository.addTask(match {
                it.title == "Recordar llamar" && !it.isCompleted
            })
        }
        coVerify { noteRepository.deleteNote("1") }
    }

    @Test
    fun `convertToTask de nota sin titulo usa los primeros 50 chars del contenido`() = runTest {
        val longContent = "A".repeat(120)
        val n = note("1", "", content = longContent)
        viewModel.convertToTask(n)

        coVerify {
            taskRepository.addTask(match { it.title.length == 50 && it.title == "A".repeat(50) })
        }
    }

    @Test
    fun `generateFlashcardsForNote falla cuando el contenido esta vacio`() = runTest {
        coEvery { userStatsRepository.getRemainingFlashcardGenerations() } returns 5
        val n = note("1", "Nota", content = "")
        var errMsg: String? = null
        viewModel.generateFlashcardsForNote(n, onSuccess = {}, onError = { errMsg = it })
        advanceUntilIdle()

        coVerify(exactly = 0) { aiService.generateFlashcards(any()) }
        assertEquals("La nota está vacía. Agrega contenido para generar tarjetas.", errMsg)
    }

    @Test
    fun `generateFlashcardsForNote bloquea cuando no quedan generaciones gratuitas`() = runTest {
        coEvery { userStatsRepository.getRemainingFlashcardGenerations() } returns 0
        val n = note("1", "Nota", content = "contenido válido")
        var errMsg: String? = null

        viewModel.generateFlashcardsForNote(n, onSuccess = {}, onError = { errMsg = it })
        advanceUntilIdle()

        coVerify(exactly = 0) { aiService.generateFlashcards(any()) }
        assertTrue(errMsg?.contains("Premium") == true)
    }

    @Test
    fun `generateFlashcardsForNote persiste flashcards yumenta contador cuando IA responde`() = runTest {
        coEvery { userStatsRepository.getRemainingFlashcardGenerations() } returns 5
        val cards = listOf(
            FlashcardItem(question = "Q1", answer = "A1"),
            FlashcardItem(question = "Q2", answer = "A2")
        )
        coEvery { aiService.generateFlashcards(any()) } returns cards
        val n = note("1", "Nota", content = "contenido con teoría")
        var successCalled = false

        viewModel.generateFlashcardsForNote(n, onSuccess = { successCalled = true }, onError = { })
        advanceUntilIdle()

        coVerify { userStatsRepository.incrementFlashcardGenerationCount() }
        coVerify { noteRepository.updateNote(match { it.flashcards.size == 2 }) }
        assertTrue(successCalled)
    }

    @Test
    fun `summarizeNoteText llama a la IA aunque el usuario no sea premium`() = runTest {
        every { userStatsRepository.isPremium } returns MutableStateFlow(false)
        coEvery { aiService.summarizeText(any()) } returns "Resumen"
        val result = viewModel.summarizeNoteText("Texto de prueba")
        assertEquals("Resumen", result)
        coVerify { aiService.summarizeText("Texto de prueba") }
    }

    @Test
    fun `updateFlashcardLeitner sube de caja con respuesta correcta hasta 5`() = runTest {
        val n = note(
            "1", "Tema",
            content = "x",
        ).copy(
            flashcards = listOf(
                FlashcardItem(id = "c1", question = "q", answer = "a", box = 4)
            )
        )
        viewModel.updateFlashcardLeitner(n, "c1", wasCorrect = true)

        coVerify {
            noteRepository.updateNote(match {
                it.flashcards.first().box == 5
            })
        }
    }

    @Test
    fun `updateFlashcardLeitner resetea a caja 1 con respuesta incorrecta`() = runTest {
        val n = note("1", "Tema", content = "x").copy(
            flashcards = listOf(
                FlashcardItem(id = "c1", question = "q", answer = "a", box = 3)
            )
        )
        viewModel.updateFlashcardLeitner(n, "c1", wasCorrect = false)

        coVerify {
            noteRepository.updateNote(match {
                it.flashcards.first().box == 1
            })
        }
    }

    @Test
    fun `organizeNotesWithAI bloquea cuando el usuario no es premium`() = runTest {
        every { userStatsRepository.isPremium } returns MutableStateFlow(false)
        var err: String? = null
        viewModel.organizeNotesWithAI(onSuccess = {}, onError = { err = it })

        coVerify(exactly = 0) { aiService.organizeNotes(any(), any()) }
        assertEquals("Premium required", err)
    }

    @Test
    fun `deleteCategory delega al repositorio`() = runTest {
        val cat = NoteCategory(name = "Estudios")
        viewModel.deleteCategory(cat)
        coVerify { noteRepository.deleteCategory(cat) }
    }
}