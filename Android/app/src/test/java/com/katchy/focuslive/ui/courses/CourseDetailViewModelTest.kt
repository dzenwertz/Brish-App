package com.katchy.focuslive.ui.courses

import androidx.lifecycle.SavedStateHandle
import com.katchy.focuslive.data.manager.TimerManager
import com.katchy.focuslive.data.model.Course
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.data.repository.CourseRepository
import com.katchy.focuslive.data.repository.NoteRepository
import com.katchy.focuslive.data.repository.TaskRepository
import com.katchy.focuslive.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CourseDetailViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val courses = mockk<CourseRepository>(relaxed = true)
    private val tasks = mockk<TaskRepository>(relaxed = true)
    private val notes = mockk<NoteRepository>(relaxed = true)
    private val timer = mockk<TimerManager>(relaxed = true)
    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
    private lateinit var vm: CourseDetailViewModel

    @Before fun setup() {
        every { courses.getCourses() } returns flowOf(listOf(Course(id = "c1", name = "Redes")))
        every { tasks.getDeliveries() } returns tasksFlow
        every { notes.getNotes() } returns flowOf(emptyList())
        coEvery { tasks.addTask(any()) } returns Result.success(Unit)
        coEvery { tasks.updateTask(any()) } returns Result.success(Unit)
        vm = CourseDetailViewModel(SavedStateHandle(mapOf("courseId" to "c1")), courses, tasks, notes, timer)
    }

    @Test fun `create undated delivery confirms write and retains course link`() = runTest {
        coEvery { tasks.addTask(any()) } coAnswers { kotlinx.coroutines.delay(1); Result.success(Unit) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        var success = false
        vm.saveDeliverable(DeliverableDraft("  Practica  ", "  detalles  ")) { success = true }
        assertFalse(success)
        advanceUntilIdle()
        assertTrue(success)
        coVerify { tasks.addTask(match {
            it.kind == "DELIVERY" && it.courseId == "c1" && !it.hasDueDate && it.dueDate == 0L &&
                it.time == null && it.title == "Practica" && it.description == "detalles"
        }) }
    }

    @Test fun `edit preserves id course and completion without creating another task`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        val existing = Task(id = "d1", courseId = "c1", kind = "DELIVERY", isCompleted = true)
        vm.saveDeliverable(DeliverableDraft("Nuevo", "Descripcion", 1_780_000_000_000L, "14:30"), existing)
        advanceUntilIdle()
        coVerify { tasks.updateTask(match { it.id == "d1" && it.courseId == "c1" && it.isCompleted && it.time == "14:30" }) }
        coVerify(exactly = 0) { tasks.addTask(any()) }
    }

    @Test fun `failed create update delete never confirms success`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        coEvery { tasks.addTask(any()) } returns Result.failure(IllegalStateException("Guardar fallo"))
        coEvery { tasks.updateTask(any()) } returns Result.failure(IllegalStateException("Guardar fallo"))
        coEvery { tasks.deleteTask(any()) } returns Result.failure(IllegalStateException("Guardar fallo"))
        var successes = 0
        vm.saveDeliverable(DeliverableDraft("Entrega")) { successes++ }
        advanceUntilIdle()
        vm.saveDeliverable(DeliverableDraft("Entrega"), Task(courseId = "c1")) { successes++ }
        advanceUntilIdle()
        vm.deleteDelivery("d1") { successes++ }
        advanceUntilIdle()
        assertEquals(0, successes)
        assertEquals("Guardar fallo", vm.error.value)
        assertFalse(vm.isSaving.value)
    }

    @Test fun `invalid time and course reassignment never write`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.saveDeliverable(DeliverableDraft("Entrega", date = 1L, time = "9:00"))
        vm.saveDeliverable(DeliverableDraft("Entrega", time = "09:00"))
        vm.saveDeliverable(DeliverableDraft("Entrega"), Task(courseId = "other"))
        advanceUntilIdle()
        coVerify(exactly = 0) { tasks.addTask(any()) }
        coVerify(exactly = 0) { tasks.updateTask(any()) }
    }

    @Test fun `detail includes own legacy deliveries only`() = runTest {
        tasksFlow.value = listOf(Task(id = "new", courseId = "c1", kind = "DELIVERY"),
            Task(id = "legacy", courseId = "c1"), Task(id = "other", courseId = "c2", kind = "DELIVERY"), Task(id = "agenda"))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        assertEquals(setOf("new", "legacy"), vm.state.value.deliveries.map { it.id }.toSet())
    }
}
