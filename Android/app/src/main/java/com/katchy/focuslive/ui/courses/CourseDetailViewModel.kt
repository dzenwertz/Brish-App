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
    private val courseId = checkNotNull(savedStateHandle.get<String>("courseId"))
    val state: StateFlow<CourseDetailState> = combine(
        courseRepository.getCourses(), taskRepository.getAllTasks(), noteRepository.getNotes()
    ) { courses, tasks, notes ->
        CourseDetailState(
            course = courses.find { it.id == courseId },
            deliveries = tasks.filter { it.courseId == courseId }
                .sortedWith(compareBy<Task> { !it.hasDueDate }.thenBy { it.dueDate }),
            notes = notes.filter { it.courseId == courseId }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CourseDetailState())

    fun toggleDelivery(task: Task) = viewModelScope.launch { taskRepository.toggleTaskCompletion(task.id) }

    fun beginFocus(task: Task) {
        timerManager.setActiveTask(task.id)
        timerManager.startTimer()
    }

    fun addNote(title: String, content: String) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            noteRepository.addNote(Note(title = title.ifBlank { "Apunte" }, content = content, courseId = courseId))
        }
    }

    fun addDeliverable(title: String, dueDate: Long, hasDueDate: Boolean = true) {
        val course = state.value.course ?: return
        if (title.isBlank()) return
        val finalTimestamp = if (hasDueDate) dueDate else System.currentTimeMillis()
        viewModelScope.launch {
            taskRepository.addTask(
                Task(
                    title = title.trim(),
                    category = course.name,
                    courseId = courseId,
                    deliverableType = "DELIVERY",
                    timestamp = finalTimestamp,
                    dueDate = if (hasDueDate) dueDate else 0L,
                    hasDueDate = hasDueDate,
                    priority = "HIGH"
                )
            )
        }
    }

    fun deleteDelivery(taskId: String) = viewModelScope.launch { taskRepository.deleteTask(taskId) }

    fun deleteNote(noteId: String) = viewModelScope.launch { noteRepository.deleteNote(noteId) }

    fun updateNote(note: Note) = viewModelScope.launch { noteRepository.updateNote(note) }
}
