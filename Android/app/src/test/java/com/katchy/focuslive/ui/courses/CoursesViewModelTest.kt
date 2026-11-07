package com.katchy.focuslive.ui.courses

import com.katchy.focuslive.data.model.Course
import com.katchy.focuslive.data.model.LearningSpace
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.data.repository.AppPreferencesRepository
import com.katchy.focuslive.data.repository.CourseRepository
import com.katchy.focuslive.data.repository.LearningSpaceRepository
import com.katchy.focuslive.data.repository.TaskRepository
import com.katchy.focuslive.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoursesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: CoursesViewModel

    private val preferences: AppPreferencesRepository = mockk(relaxed = true)
    private val courseRepository: CourseRepository = mockk(relaxed = true)
    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val spaceRepository: LearningSpaceRepository = mockk(relaxed = true)

    private val activeSpacesFlow = MutableStateFlow<List<LearningSpace>>(emptyList())
    private val archivedSpacesFlow = MutableStateFlow<List<LearningSpace>>(emptyList())
    private val currentSpaceIdFlow = MutableStateFlow("")
    private val currentCycleFlow = MutableStateFlow("2026-2")
    private val coursesFlow = MutableStateFlow<List<Course>>(emptyList())
    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())

    @Before
    fun setup() {
        val defaultSpace = LearningSpace(id = "space-uni", name = "Ciclo 2026-2", sortOrder = 0)
        activeSpacesFlow.value = listOf(defaultSpace)
        currentSpaceIdFlow.value = "space-uni"

        every { spaceRepository.getActiveSpaces() } returns activeSpacesFlow
        every { spaceRepository.getArchivedSpaces() } returns archivedSpacesFlow
        every { preferences.currentLearningSpaceId } returns currentSpaceIdFlow
        every { preferences.currentAcademicCycle } returns currentCycleFlow
        every { preferences.courses } returns MutableStateFlow(emptyList())
        every { courseRepository.getCourses() } returns coursesFlow
        every { taskRepository.getDeliveries() } returns tasksFlow

        coEvery { spaceRepository.addSpace(any()) } returns Result.success(Unit)
        coEvery { spaceRepository.updateSpace(any()) } returns Result.success(Unit)
        coEvery { spaceRepository.archiveSpace(any(), any()) } returns Result.success(Unit)
        coEvery { spaceRepository.deleteSpace(any()) } returns Result.success(Unit)
        coEvery { courseRepository.addCourse(any()) } returns Result.success(Unit)
        coEvery { courseRepository.deleteCourse(any()) } returns Result.success(Unit)
        coEvery { taskRepository.addTask(any()) } returns Result.success(Unit)

        viewModel = CoursesViewModel(preferences, courseRepository, taskRepository, spaceRepository)
    }

    @Test
    fun `cursos se filtran por el espacio activo actual`() = runTest {
        val courseUni = Course(id = "c1", name = "Calculo", spaceId = "space-uni")
        val courseCyber = Course(id = "c2", name = "Redes", spaceId = "space-cyber")
        coursesFlow.value = listOf(courseUni, courseCyber)

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.courses.collect {}
        }

        val filtered = viewModel.courses.value
        assertEquals(1, filtered.size)
        assertEquals("Calculo", filtered.first().course.name)

        collectJob.cancel()
    }

    @Test
    fun `crear nuevo espacio registra y selecciona el espacio`() = runTest {
        coEvery { spaceRepository.addSpace(any()) } coAnswers { kotlinx.coroutines.delay(1); Result.success(Unit) }
        var success = false
        viewModel.createSpace("Ciberseguridad") { success = true }
        assertFalse(success)

        advanceUntilIdle()

        assertTrue(success)
        coVerify {
            spaceRepository.addSpace(match { it.name == "Ciberseguridad" })
            preferences.setCurrentLearningSpaceId(any())
            preferences.setCurrentAcademicCycle("Ciberseguridad")
        }
    }

    @Test
    fun `archivar espacio llama a spaceRepository con isArchived true`() = runTest {
        val space1 = LearningSpace(id = "s1", name = "Ciclo 2026-1")
        val space2 = LearningSpace(id = "s2", name = "Ciclo 2026-2")
        activeSpacesFlow.value = listOf(space1, space2)
        currentSpaceIdFlow.value = "s1"

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.selectedSpace.collect {}
        }

        viewModel.archiveCurrentSpace()
        advanceUntilIdle()

        coVerify {
            spaceRepository.archiveSpace("s1", true)
        }

        collectJob.cancel()
    }
    @Test
    fun `failed space create does not select it or report success`() = runTest {
        advanceUntilIdle()
        io.mockk.clearMocks(preferences, answers = false)
        coEvery { spaceRepository.addSpace(any()) } returns Result.failure(IllegalStateException("Sin guardar"))
        var success = false
        viewModel.createSpace("Nuevo") { success = true }
        advanceUntilIdle()
        assertFalse(success)
        assertEquals("Sin guardar", viewModel.error.value)
        io.mockk.verify(exactly = 0) { preferences.setCurrentLearningSpaceId(any()) }
    }

    @Test
    fun `nonempty course deletion rejection keeps dialog callback pending`() = runTest {
        coEvery { courseRepository.deleteCourse("c1") } returns Result.failure(IllegalStateException("El curso contiene apuntes"))
        var success = false
        viewModel.removeCourse("c1") { success = true }
        advanceUntilIdle()
        assertFalse(success)
        assertEquals("El curso contiene apuntes", viewModel.error.value)
    }

    @Test
    fun `failed archive never changes selected space`() = runTest {
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.selectedSpace.collect {} }
        advanceUntilIdle()
        io.mockk.clearMocks(preferences, answers = false)
        coEvery { spaceRepository.archiveSpace(any(), true) } returns Result.failure(IllegalStateException("Error"))
        var success = false
        viewModel.archiveCurrentSpace { success = true }
        advanceUntilIdle()
        assertFalse(success)
        io.mockk.verify(exactly = 0) { preferences.setCurrentLearningSpaceId(any()) }
        collector.cancel()
    }

    @Test
    fun `empty course deletion confirms only after repository success`() = runTest {
        coEvery { courseRepository.deleteCourse("empty") } coAnswers { kotlinx.coroutines.delay(1); Result.success(Unit) }
        var success = false
        viewModel.removeCourse("empty") { success = true }
        assertFalse(success)
        advanceUntilIdle()
        assertTrue(success)
        coVerify { courseRepository.deleteCourse("empty") }
    }

    @Test
    fun `failed space delete does not change selection`() = runTest {
        advanceUntilIdle()
        io.mockk.clearMocks(preferences, answers = false)
        coEvery { spaceRepository.deleteSpace(any()) } returns Result.failure(IllegalStateException("Contiene cursos"))
        var success = false
        viewModel.deleteSpace("space-uni") { success = true }
        advanceUntilIdle()
        assertFalse(success)
        assertEquals("Contiene cursos", viewModel.error.value)
        io.mockk.verify(exactly = 0) { preferences.setCurrentLearningSpaceId(any()) }
    }

    @Test
    fun `partial legacy import retries missing ids without duplicates and clears only after success`() = runTest {
        val legacy = MutableStateFlow(listOf(
            AppPreferencesRepository.AcademicCourse(id = "present", name = "Existente"),
            AppPreferencesRepository.AcademicCourse(id = "a", name = "Redes"),
            AppPreferencesRepository.AcademicCourse(id = "b", name = "Calculo"),
            AppPreferencesRepository.AcademicCourse(id = "c", name = "Linux")
        ))
        coursesFlow.value = listOf(Course(id = "present", name = "Existente", spaceId = "space-uni"))
        every { preferences.courses } returns legacy
        every { preferences.clearLegacyCourses() } answers { legacy.value = emptyList() }
        var failSecond = true
        coEvery { courseRepository.addCourse(any()) } coAnswers {
            val course = firstArg<Course>()
            if (course.id == "b" && failSecond) Result.failure(IllegalStateException("Importacion parcial"))
            else {
                coursesFlow.value = coursesFlow.value + course
                Result.success(Unit)
            }
        }

        val firstOpen = CoursesViewModel(preferences, courseRepository, taskRepository, spaceRepository)
        advanceUntilIdle()
        assertEquals("Importacion parcial", firstOpen.error.value)
        assertEquals(setOf("present", "a", "c"), coursesFlow.value.map { it.id }.toSet())
        assertEquals(4, legacy.value.size)
        io.mockk.verify(exactly = 0) { preferences.clearLegacyCourses() }
        coVerify(exactly = 0) { courseRepository.addCourse(match { it.id == "present" }) }
        coVerify(exactly = 1) { courseRepository.addCourse(match { it.id == "c" }) }

        failSecond = false
        CoursesViewModel(preferences, courseRepository, taskRepository, spaceRepository)
        advanceUntilIdle()
        assertEquals(setOf("present", "a", "b", "c"), coursesFlow.value.map { it.id }.toSet())
        assertEquals(4, coursesFlow.value.size)
        assertTrue(legacy.value.isEmpty())
        coVerify(exactly = 1) { courseRepository.addCourse(match { it.id == "a" }) }
        coVerify(exactly = 2) { courseRepository.addCourse(match { it.id == "b" }) }
        coVerify(exactly = 1) { courseRepository.addCourse(match { it.id == "c" }) }
        io.mockk.verify(exactly = 1) { preferences.clearLegacyCourses() }
    }

}
