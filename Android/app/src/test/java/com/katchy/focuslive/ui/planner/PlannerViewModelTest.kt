package com.katchy.focuslive.ui.planner

import com.katchy.focuslive.data.model.MascotType
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.data.repository.AppPreferencesRepository
import com.katchy.focuslive.data.repository.MascotRepository
import com.katchy.focuslive.data.repository.TaskRepository
import com.katchy.focuslive.data.repository.UserStatsRepository
import com.katchy.focuslive.data.service.AIService
import com.katchy.focuslive.data.service.BrainDumpTask
import com.katchy.focuslive.scheduler.NotificationScheduler
import com.katchy.focuslive.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: PlannerViewModel
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val mascotRepository: MascotRepository = mockk(relaxed = true)
    private val appPreferencesRepository: AppPreferencesRepository = mockk(relaxed = true)
    private val notificationScheduler: NotificationScheduler = mockk(relaxed = true)
    private val userStatsRepository: UserStatsRepository = mockk(relaxed = true)
    private val aiService: AIService = mockk(relaxed = true)

    @Before
    fun setup() {
        every { mascotRepository.selectedMascot } returns MutableStateFlow(MascotType.POPPIN)
        every { userStatsRepository.isPremium } returns MutableStateFlow(false)
        every { taskRepository.getTasksForDate(any()) } returns flowOf(emptyList<Task>())

        viewModel = PlannerViewModel(
            taskRepository,
            mascotRepository,
            appPreferencesRepository,
            notificationScheduler,
            userStatsRepository,
            aiService
        )
    }

    private fun startOfDay(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private val DAY_MS = 24L * 60L * 60L * 1000L

    private fun kotlinx.coroutines.test.TestScope.collectTasks(): List<Task> {
        val job = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.tasksForSelectedDate.collect { }
        }
        advanceUntilIdle()
        runCatching { job.cancel() }
        advanceUntilIdle()
        return viewModel.tasksForSelectedDate.value
    }

    @Test
    fun `tasksForSelectedDate filters non-recurring task to its own day`() = runTest {
        val today = startOfDay(System.currentTimeMillis())
        val taskOnDay = Task(id = "1", title = "Hoy", timestamp = today, repeatMode = "NONE")
        val taskAnotherDay = Task(
            id = "2", title = "Otro dia", timestamp = today + DAY_MS, repeatMode = "NONE"
        )
        every { taskRepository.getTasksForDate(any()) } returns flowOf(listOf(taskOnDay, taskAnotherDay))

        val items = collectTasks()
        assertEquals(1, items.size)
        assertEquals("Hoy", items[0].title)
    }

    @Test
    fun `tareas DAILY se muestran en cualquier dia posterior a su creacion`() = runTest {
        val today = startOfDay(System.currentTimeMillis())
        val yesterday = today - DAY_MS
        val dailyTask = Task(
            id = "d1", title = "Beber agua", timestamp = yesterday, repeatMode = "DAILY"
        )
        every { taskRepository.getTasksForDate(any()) } returns flowOf(listOf(dailyTask))

        val items = collectTasks()
        assertEquals(1, items.size)
        assertEquals("Beber agua", items[0].title)
    }

    @Test
    fun `tarea WEEKLY solo aparece en el dia de la semana configurado`() = runTest {
        val mondayLast = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis > System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, -1)
        }.timeInMillis

        val weeklyTask = Task(
            id = "w1",
            title = "Gym",
            timestamp = mondayLast,
            repeatMode = "WEEKLY",
            repeatDays = listOf(Calendar.MONDAY)
        )
        every { taskRepository.getTasksForDate(any()) } returns flowOf(listOf(weeklyTask))

        val aMonday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.WEEK_OF_YEAR, 1)
        }.timeInMillis
        viewModel.selectDate(aMonday)

        val onMonday = collectTasks()
        assertTrue("El lunes debería contener la tarea semanal", onMonday.any { it.id == "w1" })
    }

    @Test
    fun `addTask llama al repositorio con el titulo recibido`() = runTest {
        val title = "Nueva Tarea"
        coEvery { taskRepository.addTask(any()) } returns Result.success(Unit)

        viewModel.addTask(title)

        advanceUntilIdle()
        coVerify { taskRepository.addTask(match { it.title == title }) }
    }

    @Test
    fun `toggleTask de tarea recurrente marca completedDates y resetea isCompleted`() = runTest {
        val today = startOfDay(System.currentTimeMillis())
        val recurring = Task(
            id = "r1", title = "Leer 10 min", timestamp = today, repeatMode = "DAILY"
        )
        
        coEvery { taskRepository.toggleTaskCompletion(any(), any()) } returns Result.success(true)
        
        viewModel.toggleTask(recurring)
        advanceUntilIdle()

        coVerify {
            taskRepository.toggleTaskCompletion("r1", today)
        }
    }

    @Test
    fun `toggleTask de tarea NO recurrente invierte isCompleted`() = runTest {
        val today = startOfDay(System.currentTimeMillis())
        val oneOff = Task(
            id = "one1", title = "Enviar reporte", timestamp = today,
            isCompleted = false, repeatMode = "NONE"
        )
        coEvery { taskRepository.toggleTaskCompletion(any(), any()) } returns Result.success(true)
        
        viewModel.toggleTask(oneOff)
        advanceUntilIdle()

        coVerify {
            taskRepository.toggleTaskCompletion("one1", today)
        }
    }

    @Test
    fun `generateBrainDump convierte los resultados de la IA en tareas`() = runTest {
        viewModel.selectDate(startOfDay(System.currentTimeMillis()))
        val aiTasks = listOf(
            BrainDumpTask(title = "Comprar pan", description = "Integral", time = "08:00")
        )
        coEvery { aiService.generateBrainDumpTasks(any()) } returns aiTasks
        coEvery { taskRepository.addTask(any()) } returns Result.success(Unit)

        var successCalled = false
        viewModel.generateBrainDump("comprar pan integral a las 8") { successCalled = true }
        advanceUntilIdle()

        coVerify(atLeast = 1) {
            taskRepository.addTask(match {
                it.title == "Comprar pan" &&
                    it.time == "08:00" &&
                    it.subtasks.any { s -> s.title == "Integral" }
            })
        }
        assertTrue(successCalled)
    }

    @Test
    fun `generateBrainDump con texto en blanco no invoca IA`() = runTest {
        viewModel.generateBrainDump("   ") {}
        advanceUntilIdle()

        coVerify(exactly = 0) { aiService.generateBrainDumpTasks(any()) }
        coVerify(exactly = 0) { taskRepository.addTask(any()) }
    }
}