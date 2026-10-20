package com.katchy.focuslive.ui.courses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.manager.TimerManager
import com.katchy.focuslive.data.model.Course
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.data.repository.CourseRepository
import com.katchy.focuslive.data.repository.NoteRepository
import com.katchy.focuslive.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CourseDetailState(
    val course: Course? = null,
    val deliveries: List<Task> = emptyList(),
    val notes: List<Note> = emptyList()
)

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    courseRepository: CourseRepository,
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val timerManager: TimerManager
) : ViewModel() {
    val courseId = checkNotNull(savedStateHandle.get<String>("courseId"))
    val state: StateFlow<CourseDetailState> = combine(
        courseRepository.getCourses(), taskRepository.getDeliveries(), noteRepository.getNotes()
    ) { courses, tasks, notes ->
        CourseDetailState(
            course = courses.find { it.id == courseId },
            deliveries = tasks.filter { it.courseId == courseId && (it.kind == "DELIVERY" || it.kind == "AGENDA" || it.deliverableType == "DELIVERY") }
                .sortedWith(compareBy<Task> { !it.hasDueDate }.thenBy { it.dueDate }),
            notes = notes.filter { it.courseId == courseId }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CourseDetailState())

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()
    fun clearError() { _error.value = null }

    private fun write(onSuccess: () -> Unit = {}, action: suspend () -> Unit) {
        if (_isSaving.value) return
        _isSaving.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                action()
                onSuccess()
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.localizedMessage ?: "No se pudo guardar el cambio. Intenta nuevamente." }
            finally { _isSaving.value = false }
        }
    }

    fun toggleDelivery(task: Task) = write { taskRepository.toggleTaskCompletion(task.id).getOrThrow() }

    fun beginFocus(task: Task) {
        timerManager.setActiveTask(task.id)
        timerManager.startTimer()
    }

    fun saveDeliverable(draft: DeliverableDraft, existing: Task? = null, onSuccess: () -> Unit = {}) {
        draft.validationError()?.let { _error.value = it; return }
        val course = state.value.course ?: run { _error.value = "No se encontró el curso."; return }
        if (existing != null && existing.courseId != courseId) { _error.value = "La entrega pertenece a otro curso."; return }
        write(onSuccess) {
            val task = (existing ?: Task()).copy(
                kind = "DELIVERY", title = draft.title.trim(), description = draft.description.trim(),
                category = course.name, courseId = courseId, deliverableType = "DELIVERY",
                timestamp = if (existing != null) existing.timestamp else draft.date ?: System.currentTimeMillis(),
                dueDate = draft.date?.let { deliveryDueMillis(it, draft.time) } ?: 0L,
                hasDueDate = draft.date != null, time = draft.time?.takeIf { draft.date != null },
                endTime = null, repeatMode = "NONE", repeatDays = emptyList()
            )
            if (existing == null) taskRepository.addTask(task).getOrThrow()
            else taskRepository.updateTask(task).getOrThrow()
        }
    }

    fun deleteDelivery(taskId: String, onSuccess: () -> Unit = {}) =
        write(onSuccess) { taskRepository.deleteTask(taskId).getOrThrow() }
}
