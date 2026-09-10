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
        every { taskRepository.getAllTasks() } returns tasksFlow

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
        val result = viewModel.createSpace("Ciberseguridad")
        assertTrue(result)

        advanceUntilIdle()

        coVerify {
            spaceRepository.addSpace(match { it.name == "Ciberseguridad" })
            preferences.setCurrentLearningSpaceId(any())
            preferences.setCurrentAcademicCycle("Ciberseguridad")
        }
    }

    @Test
    fun `agregar entrega sin fecha limite establece hasDueDate en false`() = runTest {
        val course = Course(id = "c1", name = "Linux", spaceId = "space-uni")

        val result = viewModel.addDeliverable(
            course = course,
            title = "Configurar firewall",
            dueDate = 0L,
            hasDueDate = false
        )
        assertTrue(result)

        advanceUntilIdle()

        coVerify {
            taskRepository.addTask(match {
                it.title == "Configurar firewall" &&
                    !it.hasDueDate &&
                    it.courseId == "c1"
            })
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
}
