package com.katchy.focuslive.ui.chat

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.katchy.focuslive.data.model.MascotType
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.model.NoteCategory
import com.katchy.focuslive.data.model.Transaction
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.data.local.dao.ChatDao
import com.katchy.focuslive.data.repository.AppPreferencesRepository
import com.katchy.focuslive.data.repository.FinanceRepository
import com.katchy.focuslive.data.repository.CourseRepository
import com.katchy.focuslive.data.repository.HabitRepository
import com.katchy.focuslive.data.repository.MascotRepository
import com.katchy.focuslive.data.repository.NoteRepository
import com.katchy.focuslive.data.repository.OpenCodeRepository
import com.katchy.focuslive.data.repository.TaskRepository
import com.katchy.focuslive.data.repository.UserStatsRepository
import com.katchy.focuslive.util.AnalyticsHelper
import com.katchy.focuslive.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: ChatViewModel

    private val openCodeRepository: OpenCodeRepository = mockk(relaxed = true)
    private val chatDao: ChatDao = mockk(relaxed = true)
    private val mascotRepository: MascotRepository = mockk(relaxed = true)
    private val noteRepository: NoteRepository = mockk(relaxed = true)
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val courseRepository: CourseRepository = mockk(relaxed = true)
    private val financeRepository: FinanceRepository = mockk(relaxed = true)
    private val habitRepository: HabitRepository = mockk(relaxed = true)
    private val appPreferencesRepository: AppPreferencesRepository = mockk(relaxed = true)
    private val userStatsRepository: UserStatsRepository = mockk(relaxed = true)
    private val firebaseAuth: FirebaseAuth = mockk(relaxed = true)
    private val firestore: FirebaseFirestore = mockk(relaxed = true)
    private val spaceRepository: com.katchy.focuslive.data.repository.LearningSpaceRepository = mockk(relaxed = true)
    private val analyticsHelper: AnalyticsHelper = mockk(relaxed = true)

    private val mockUser: FirebaseUser = mockk(relaxed = true)

    @Before
    fun setup() {
        every { mockUser.uid } returns "test-uid"
        every { firebaseAuth.currentUser } returns mockUser
        every { mascotRepository.selectedMascot } returns MutableStateFlow(MascotType.POPPIN)
        every { userStatsRepository.isPremium } returns MutableStateFlow(false)
        coEvery { openCodeRepository.checkAccess() } returns Result.success(true)

        // Empty data sources to avoid Firestore listener firing
        every { noteRepository.getNotes() } returns flowOf(emptyList<Note>())
        every { noteRepository.getCategories() } returns flowOf(emptyList<NoteCategory>())
        every { taskRepository.getAllTasks() } returns flowOf(emptyList<Task>())
        every { courseRepository.getCourses() } returns flowOf(emptyList())
        every { spaceRepository.getSpaces() } returns flowOf(emptyList())
        every { spaceRepository.getActiveSpaces() } returns flowOf(emptyList())
        every { habitRepository.getHabits() } returns flowOf(emptyList<Habit>())
        every { chatDao.getAllSessions() } returns flowOf(emptyList())
        every { chatDao.getMessagesForSession(any()) } returns flowOf(emptyList())
        every { chatDao.getUserMemory() } returns flowOf(null)

        viewModel = ChatViewModel(
            openCodeRepository,
            chatDao,
            mascotRepository,
            noteRepository,
            taskRepository,
            courseRepository,
            financeRepository,
            habitRepository,
            appPreferencesRepository,
            userStatsRepository,
            firebaseAuth,
            firestore,
            spaceRepository,
            analyticsHelper
        )
    }

    private fun payloadJson(vararg actionJson: String): String =
        """{"intent":"AGENT_ACTION","actions":[${actionJson.joinToString(",")}]}"""

    @Test
    fun `executeActions CREATE_TASK llama a taskRepository con titulo y categoria`() = runTest {
        val payload = payloadJson(
            """{"type":"CREATE_TASK","title":"Estudiar Kotlin","category":"Estudios","dueDateOffsetDays":2}"""
        )
        coEvery { taskRepository.addTask(any()) } returns Result.success(Unit)

        viewModel.executeActions("msg-1", payload)

        coVerify(timeout = 3000L) {
            taskRepository.addTask(match {
                it.title == "Estudiar Kotlin" &&
                    it.category == "Estudios" &&
                    !it.isCompleted
            })
        }
    }

    @Test
    fun `executeActions CREATE_NOTE sin categoria existente crea la categoria y luego la nota`() = runTest {
        every { noteRepository.getCategories() } returns flowOf(emptyList())
        val payload = payloadJson(
            """{"type":"CREATE_NOTE","title":"Idea","content":"Anotar","categoryName":"Personal","colorHex":"#FFF9C4"}"""
        )

        viewModel.executeActions("msg-2", payload)

        coVerify(timeout = 3000L) { noteRepository.addCategory(match { it.name == "Personal" }) }
        coVerify(timeout = 3000L) {
            noteRepository.addNote(match {
                it.title == "Idea" &&
                    it.content == "Anotar" &&
                    it.colorHex == "#FFF9C4" &&
                    it.priority == "MEDIUM"
            })
        }
    }

    @Test
    fun `executeActions CREATE_NOTE reusa categoria existente cuando coincide el nombre`() = runTest {
        val existing = NoteCategory(id = "cat-9", name = "Estudios")
        every { noteRepository.getCategories() } returns flowOf(listOf(existing))
        val payload = payloadJson(
            """{"type":"CREATE_NOTE","title":"Apunte","content":"x","categoryName":"Estudios"}"""
        )

        viewModel.executeActions("msg-3", payload)

        coVerify(timeout = 3000L, exactly = 0) {
            noteRepository.addCategory(match { it.name.equals("estudios", ignoreCase = true) && it.id != "cat-9" })
        }
        coVerify(timeout = 3000L) {
            noteRepository.addNote(match { it.categoryId == "cat-9" })
        }
    }

    @Test
    fun `executeActions DELETE_NOTE llama a deleteNote con el id recibido`() = runTest {
        val payload = payloadJson("""{"type":"DELETE_NOTE","id":"note-123"}""")

        viewModel.executeActions("msg-4", payload)

        coVerify(timeout = 3000L) { noteRepository.deleteNote("note-123") }
    }

    @Test
    fun `executeActions SET_POMODORO actualiza work y break durations`() = runTest {
        val payload = payloadJson(
            """{"type":"SET_POMODORO","focusMinutes":50,"breakMinutes":10}"""
        )

        viewModel.executeActions("msg-5", payload)

        coVerify(timeout = 3000L) { appPreferencesRepository.setWorkDuration(50L) }
        coVerify(timeout = 3000L) { appPreferencesRepository.setBreakDuration(10L) }
    }

    @Test
    fun `executeActions SET_POMODORO usa defaults cuando faltan campos`() = runTest {
        val payload = payloadJson("""{"type":"SET_POMODORO"}""")

        viewModel.executeActions("msg-5b", payload)

        coVerify(timeout = 3000L) { appPreferencesRepository.setWorkDuration(25L) }
        coVerify(timeout = 3000L) { appPreferencesRepository.setBreakDuration(5L) }
    }

    @Test
    fun `executeActions CREATE_TRANSACTION llama a financeRepository con montos y tipo`() = runTest {
        val payload = payloadJson(
            """{"type":"CREATE_TRANSACTION","title":"Almuerzo","amount":15.5,"transactionType":"EXPENSE","category":"Comida"}"""
        )
        coEvery { financeRepository.addTransaction(any()) } returns Result.success(Unit)

        viewModel.executeActions("msg-6", payload)

        coVerify(timeout = 3000L) {
            financeRepository.addTransaction(match {
                it.title == "Almuerzo" &&
                    it.amount == 15.5 &&
                    it.type == "EXPENSE" &&
                    it.category == "Comida"
            })
        }
    }

    @Test
    fun `executeActions con tipo desconocido no invoca ningun repositorio`() = runTest {
        val payload = payloadJson("""{"type":"DANCE_BACHATA","title":"foo"}""")

        viewModel.executeActions("msg-7", payload)

        // Esperamos un instante para asegurar que el IO scope no invoque nada por casualidad.
        kotlinx.coroutines.delay(150)
        coVerify(exactly = 0) { taskRepository.addTask(any()) }
        coVerify(exactly = 0) { noteRepository.addNote(any()) }
        coVerify(exactly = 0) { financeRepository.addTransaction(any()) }
    }

    @Test
    fun `executeActions no hace nada cuando no hay usuario autenticado`() = runTest {
        every { firebaseAuth.currentUser } returns null

        val payload = payloadJson("""{"type":"CREATE_TASK","title":"x"}""")
        viewModel.executeActions("msg-8", payload)

        kotlinx.coroutines.delay(150)
        coVerify(exactly = 0) { taskRepository.addTask(any()) }
    }

    @Test
    fun `payload JSON invalido no revienta el ViewModel`() = runTest {
        val payload = "{esto no es json"
        // Debe atrapar la excepcion internamente (try/catch) y no propagarla.
        viewModel.executeActions("msg-9", payload)
        kotlinx.coroutines.delay(150)
        // Llega aqui sin lanzar.
    }

    @Test
    fun `exportToTasks parsea lineas con bullet y las convierte en tareas`() = runTest {
        val text = """
            Plan de mañana:
            - Tomar agua
            * Estudiar 30 min
            Día 1: Meditar
            1. Correr
            Esto no es una tarea
        """.trimIndent()

        viewModel.exportToTasks(text)

        coVerify(timeout = 3000L, atLeast = 1) {
            taskRepository.addTask(match { it.title == "Tomar agua" || it.title == "Estudiar 30 min" || it.title == "Meditar" || it.title == "Correr" })
        }
    }

    @Test
    fun `exportToNotes crea una nota con el contenido exportado`() = runTest {
        viewModel.exportToNotes("Plan de comidas balanceado para la semana")
        coVerify(timeout = 3000L) {
            noteRepository.addNote(match { it.content.contains("comidas", ignoreCase = true) })
        }
    }

    @Test
    fun `executeActions CREATE_TASK con campos avanzados llama a taskRepository`() = runTest {
        val payload = payloadJson(
            """{"type":"CREATE_TASK","title":"Rutina Gym","category":"Salud","dueDateOffsetDays":1,"time":"08:30","repeatMode":"WEEKLY","repeatDays":[2,4,6],"priority":"HIGH","description":"Hacer pierna"}"""
        )
        coEvery { taskRepository.addTask(any()) } returns Result.success(Unit)

        viewModel.executeActions("msg-task-adv", payload)

        coVerify(timeout = 3000L) {
            taskRepository.addTask(match {
                it.title == "Rutina Gym" &&
                    it.category == "Salud" &&
                    it.time == "08:30" &&
                    it.repeatMode == "WEEKLY" &&
                    it.repeatDays == listOf(2, 4, 6) &&
                    it.priority == "HIGH" &&
                    it.description == "Hacer pierna"
            })
        }
    }

    @Test
    fun `executeActions UPDATE_TASK modifica tarea existente`() = runTest {
        val existingTask = Task(id = "task-to-update", title = "Estudiar", category = "Estudio")
        every { taskRepository.getAllTasks() } returns flowOf(listOf(existingTask))
        coEvery { taskRepository.updateTask(any()) } returns Result.success(Unit)

        val payload = payloadJson(
            """{"type":"UPDATE_TASK","id":"task-to-update","title":"Estudiar Avanzado","priority":"HIGH","isCompleted":true}"""
        )

        viewModel.executeActions("msg-task-upd", payload)

        coVerify(timeout = 3000L) {
            taskRepository.updateTask(match {
                it.id == "task-to-update" &&
                    it.title == "Estudiar Avanzado" &&
                    it.priority == "HIGH" &&
                    it.isCompleted
            })
        }
    }

    @Test
    fun `executeActions DELETE_TASK elimina la tarea correspondiente`() = runTest {
        val payload = payloadJson("""{"type":"DELETE_TASK","id":"task-to-delete"}""")
        coEvery { taskRepository.deleteTask(any()) } returns Result.success(Unit)

        viewModel.executeActions("msg-task-del", payload)

        coVerify(timeout = 3000L) {
            taskRepository.deleteTask("task-to-delete")
        }
    }

    @Test
    fun `executeActions CREATE_HABIT llama a habitRepository para agregar el habito`() = runTest {
        val payload = payloadJson(
            """{"type":"CREATE_HABIT","title":"Meditar","icon":"🧘","reminderTime":"07:00","daysOfWeek":[1,3,5]}"""
        )
        coEvery { habitRepository.addHabit(any()) } returns Result.success(Unit)

        viewModel.executeActions("msg-habit-create", payload)

        coVerify(timeout = 3000L) {
            habitRepository.addHabit(match {
                it.title == "Meditar" &&
                    it.icon == "🧘" &&
                    it.reminderTime == "07:00" &&
                    it.daysOfWeek == listOf(1, 3, 5)
            })
        }
    }

    @Test
    fun `executeActions UPDATE_HABIT actualiza habito existente`() = runTest {
        val existingHabit = Habit(id = "habit-to-update", title = "Beber agua", icon = "💧")
        every { habitRepository.getHabits() } returns flowOf(listOf(existingHabit))
        coEvery { habitRepository.updateHabit(any()) } returns Result.success(Unit)

        val payload = payloadJson(
            """{"type":"UPDATE_HABIT","id":"habit-to-update","title":"Beber 2L de agua","reminderTime":"20:00"}"""
        )

        viewModel.executeActions("msg-habit-upd", payload)

        coVerify(timeout = 3000L) {
            habitRepository.updateHabit(match {
                it.id == "habit-to-update" &&
                    it.title == "Beber 2L de agua" &&
                    it.reminderTime == "20:00"
            })
        }
    }

    @Test
    fun `executeActions DELETE_HABIT llama a deleteHabit con id recibido`() = runTest {
        val payload = payloadJson("""{"type":"DELETE_HABIT","id":"habit-to-delete"}""")
        coEvery { habitRepository.deleteHabit(any()) } returns Result.success(Unit)

        viewModel.executeActions("msg-habit-del", payload)

        coVerify(timeout = 3000L) {
            habitRepository.deleteHabit("habit-to-delete")
        }
    }
}
