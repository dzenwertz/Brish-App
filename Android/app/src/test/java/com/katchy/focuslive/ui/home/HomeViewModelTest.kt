package com.katchy.focuslive.ui.home

import android.app.NotificationManager
import android.content.Context
import com.katchy.focuslive.data.manager.TimerManager
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.data.repository.*
import com.katchy.focuslive.util.MainDispatcherRule
import com.katchy.focuslive.ui.util.NetworkMonitor
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: HomeViewModel

    // Mocks
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val courseRepository: CourseRepository = mockk(relaxed = true)
    private val spaceRepository: LearningSpaceRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val networkMonitor: NetworkMonitor = mockk(relaxed = true)
    private val timerManager: TimerManager = mockk(relaxed = true)
    private val appPreferencesRepository: AppPreferencesRepository = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val notificationManager: NotificationManager = mockk(relaxed = true)
    private val workManager: androidx.work.WorkManager = mockk(relaxed = true)

    @Before
    fun setup() {
        // Mock Services
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager

        io.mockk.mockkStatic(androidx.work.WorkManager::class)
        every { androidx.work.WorkManager.getInstance(any()) } returns workManager

        // Setup default flows
        every { taskRepository.getTasksForDate(any()) } returns flowOf(emptyList())
        every { taskRepository.getAllTasks() } returns flowOf(emptyList())
        every { courseRepository.getCourses() } returns flowOf(emptyList())
        every { spaceRepository.getSpaces() } returns flowOf(emptyList())
        every { networkMonitor.connectionState } returns MutableStateFlow(com.katchy.focuslive.ui.util.ConnectionState.Unavailable)
        every { authRepository.authStateFlow } returns flowOf(null)
        every { authRepository.currentUser } returns null

        // TimerManager Setup
        every { timerManager.timeLeft } returns MutableStateFlow(1500L)
        every { timerManager.isTimerRunning } returns MutableStateFlow(false)
        every { timerManager.timerState } returns MutableStateFlow(TimerManager.TimerState.WORK)
        every { timerManager.activeTaskId } returns MutableStateFlow(null)
        every { timerManager.ambientSound } returns MutableStateFlow(TimerManager.AmbientSound.NONE)

        // Prefs
        every { appPreferencesRepository.workDuration } returns MutableStateFlow(25L)
        every { appPreferencesRepository.breakDuration } returns MutableStateFlow(5L)
        every { appPreferencesRepository.isPomodoroSoundEnabled } returns MutableStateFlow(true)
        every { appPreferencesRepository.customSoundUri } returns MutableStateFlow(null)


        viewModel = HomeViewModel(
            taskRepository,
            courseRepository,
            spaceRepository,
            authRepository,
            networkMonitor,
            timerManager,
            appPreferencesRepository,
            context
        )
    }

    @Test
    fun toggleTask_completingTask_callsRepository() = runTest {
        // Arrange
        val task = Task(id = "1", title = "Test Task", isCompleted = false)
        coEvery { taskRepository.toggleTaskCompletion(any(), any()) } returns Result.success(true)

        // Act
        viewModel.toggleTask(task)

        runCurrent()
        // Assert
        coVerify(exactly = 1) { taskRepository.toggleTaskCompletion("1", any()) }
    }

    @Test
    fun toggleTask_calledRapidly_debouncesSubsequentCalls() = runTest {
        // Arrange
        val task = Task(id = "2", title = "Debounce Task", isCompleted = false)
        coEvery { taskRepository.toggleTaskCompletion(any(), any()) } returns Result.success(true)

        // Act: two clicks with 0ms delay
        viewModel.toggleTask(task)
        viewModel.toggleTask(task)

        runCurrent()
        // Assert: only 1 repository call allowed by debounce
        coVerify(exactly = 1) { taskRepository.toggleTaskCompletion("2", any()) }
    }

    @Test fun toggleTask_failurePublishesSnackbarError() = runTest {
        coEvery { taskRepository.toggleTaskCompletion(any(), any()) } returns Result.failure(IllegalStateException("Write rejected"))
        viewModel.toggleTask(Task(id = "failed"))
        runCurrent()
        assertNotNull(viewModel.taskCreationError.value)
    }

    @Test fun addTask_successAcknowledgesRepositoryPersistence() = runTest {
        coEvery { taskRepository.addTask(any()) } returns Result.success(Unit)
        var saved = false
        viewModel.addTask("  Leer  ", time = "10:00", onSaved = { saved = true })
        runCurrent()
        coVerify(exactly = 1) { taskRepository.addTask(match { it.title == "Leer" && it.kind == "AGENDA" && it.time == "10:00" }) }
        assertTrue(saved)
        assertNull(viewModel.taskCreationError.value)
        assertFalse(viewModel.isCreatingTask.value)
    }

    @Test fun addTask_failedPersistenceKeepsFormOpen_andRetryCanSucceed() = runTest {
        coEvery { taskRepository.addTask(any()) } returns Result.failure(IllegalStateException("Save failed"))
        var saved = false
        viewModel.addTask("Leer", onSaved = { saved = true })
        runCurrent()
        assertFalse(saved)
        assertNotNull(viewModel.taskCreationError.value)
        assertFalse(viewModel.isCreatingTask.value)
        coEvery { taskRepository.addTask(any()) } returns Result.success(Unit)
        viewModel.addTask("Leer", onSaved = { saved = true })
        runCurrent()
        assertTrue(saved)
        assertNull(viewModel.taskCreationError.value)
    }

    @Test fun addTask_waitsForPersistence_andRejectsDuplicateSubmission() = runTest {
        val result = kotlinx.coroutines.CompletableDeferred<Result<Unit>>()
        coEvery { taskRepository.addTask(any()) } coAnswers { result.await() }
        var saved = false
        viewModel.addTask("Leer", onSaved = { saved = true })
        runCurrent()
        assertTrue(viewModel.isCreatingTask.value)
        assertFalse(saved)
        viewModel.addTask("Duplicado")
        runCurrent()
        coVerify(exactly = 1) { taskRepository.addTask(any()) }
        result.complete(Result.success(Unit))
        runCurrent()
        assertTrue(saved)
        assertFalse(viewModel.isCreatingTask.value)
    }

    @Test fun postponeTask_andUndoPostponeTask_updatesAndRestoresOriginalTask() = runTest {
        val original = Task(id = "postpone-1", title = "Llamar banco", timestamp = 1_700_000_000_000L, dueDate = 1_700_000_000_000L, isMyDay = true)
        coEvery { taskRepository.updateTask(any()) } returns Result.success(Unit)

        var postponedDone = false
        viewModel.postponeTask(original) { postponedDone = true }
        runCurrent()

        assertTrue(postponedDone)
        coVerify(exactly = 1) { taskRepository.updateTask(match { it.id == "postpone-1" && !it.isMyDay && it.timestamp > original.timestamp }) }

        viewModel.undoPostponeTask()
        runCurrent()

        coVerify(exactly = 1) { taskRepository.updateTask(match { it.id == "postpone-1" && it.isMyDay && it.timestamp == original.timestamp }) }
    }

}
