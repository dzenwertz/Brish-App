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
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: HomeViewModel
    
    // Mocks
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val courseRepository: CourseRepository = mockk(relaxed = true)
    private val habitRepository: HabitRepository = mockk(relaxed = true)
    private val financeRepository: FinanceRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val noteRepository: NoteRepository = mockk(relaxed = true)
    private val mascotRepository: MascotRepository = mockk(relaxed = true)
    private val networkMonitor: NetworkMonitor = mockk(relaxed = true)
    private val timerManager: TimerManager = mockk(relaxed = true)
    private val appPreferencesRepository: AppPreferencesRepository = mockk(relaxed = true)
    private val userStatsRepository: UserStatsRepository = mockk(relaxed = true)
    private val widgetManager: com.katchy.focuslive.data.manager.WidgetManager = mockk(relaxed = true)
    private val notificationSchedulerProvider: javax.inject.Provider<com.katchy.focuslive.scheduler.NotificationScheduler> = mockk(relaxed = true)
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
        every { habitRepository.getHabits() } returns flowOf(emptyList())
        every { financeRepository.getTransactions() } returns flowOf(emptyList())
        every { noteRepository.getNotes() } returns flowOf(emptyList())
        every { authRepository.authStateFlow } returns flowOf(null)
        every { mascotRepository.selectedMascot } returns MutableStateFlow(com.katchy.focuslive.data.model.MascotType.POPPIN)
        
        // TimerManager Setup
        every { timerManager.timeLeft } returns MutableStateFlow(1500L)
        every { timerManager.isTimerRunning } returns MutableStateFlow(false)
        every { timerManager.timerState } returns MutableStateFlow(TimerManager.TimerState.WORK)
        every { timerManager.activeTaskId } returns MutableStateFlow(null)
        every { timerManager.ambientSound } returns MutableStateFlow(TimerManager.AmbientSound.NONE)
        
        // Prefs
        every { appPreferencesRepository.workDuration } returns MutableStateFlow(25L)
        every { appPreferencesRepository.breakDuration } returns MutableStateFlow(5L)
        every { appPreferencesRepository.isPlannerEnabled } returns MutableStateFlow(true)
        every { appPreferencesRepository.isNotesEnabled } returns MutableStateFlow(true)
        every { appPreferencesRepository.isFinanceEnabled } returns MutableStateFlow(true)
        every { appPreferencesRepository.isHabitsEnabled } returns MutableStateFlow(true)
        every { appPreferencesRepository.isPomodoroSoundEnabled } returns MutableStateFlow(true)
        every { appPreferencesRepository.customSoundUri } returns MutableStateFlow(null)
        
        every { userStatsRepository.userStats } returns MutableStateFlow(com.katchy.focuslive.data.model.UserStats())
        every { userStatsRepository.isPremium } returns MutableStateFlow(false)
        every { userStatsRepository.pendingDailyReward } returns MutableStateFlow(null)
        every { userStatsRepository.getStreakMultiplier() } returns 1.0f

        viewModel = HomeViewModel(
            taskRepository,
            courseRepository,
            habitRepository,
            financeRepository,
            authRepository,
            noteRepository,
            mascotRepository,
            networkMonitor,
            timerManager,
            appPreferencesRepository,
            userStatsRepository,
            widgetManager,
            notificationSchedulerProvider,
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

        // Assert: only 1 repository call allowed by debounce
        coVerify(exactly = 1) { taskRepository.toggleTaskCompletion("2", any()) }
    }

    @Test
    fun toggleHabitToday_calledRapidly_debouncesSubsequentCalls() = runTest {
        // Arrange
        val habit = com.katchy.focuslive.data.model.Habit(id = "h1", title = "Debounce Habit")
        coEvery { habitRepository.updateHabit(any()) } returns Result.success(Unit)

        // Act: two clicks with 0ms delay
        viewModel.toggleHabitToday(habit)
        viewModel.toggleHabitToday(habit)

        // Assert: only 1 repository call allowed by debounce
        coVerify(exactly = 1) { habitRepository.updateHabit(any()) }
    }
}
