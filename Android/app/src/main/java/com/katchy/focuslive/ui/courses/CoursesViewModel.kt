package com.katchy.focuslive.ui.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.model.Course
import com.katchy.focuslive.data.model.LearningSpace
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.data.repository.AppPreferencesRepository
import com.katchy.focuslive.data.repository.CourseRepository
import com.katchy.focuslive.data.repository.LearningSpaceRepository
import com.katchy.focuslive.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CourseSummary(
    val course: Course,
    val deliveries: List<Task> = emptyList()
) {
    val pendingDeliveries: List<Task> get() = deliveries.filterNot { it.isCompleted }
    val completedCount: Int get() = deliveries.count { it.isCompleted }
    val progress: Float get() = if (deliveries.isEmpty()) 0f else completedCount.toFloat() / deliveries.size
}

@HiltViewModel
class CoursesViewModel @Inject constructor(
    private val preferences: AppPreferencesRepository,
    private val courseRepository: CourseRepository,
    private val taskRepository: TaskRepository,
    private val spaceRepository: LearningSpaceRepository
) : ViewModel() {

    val activeSpaces: StateFlow<List<LearningSpace>> = spaceRepository.getActiveSpaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archivedSpaces: StateFlow<List<LearningSpace>> = spaceRepository.getArchivedSpaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentSpaceId: StateFlow<String> = preferences.currentLearningSpaceId

    val selectedSpace: StateFlow<LearningSpace?> = combine(
        activeSpaces,
        currentSpaceId
    ) { spaces, id ->
        spaces.firstOrNull { it.id == id } ?: spaces.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Legacy cycle name support for components that display it
    val currentCycle: StateFlow<String> = preferences.currentAcademicCycle

    val courses: StateFlow<List<CourseSummary>> = combine(
        courseRepository.getCourses(),
        taskRepository.getAllTasks(),
        selectedSpace,
        activeSpaces
    ) { allCourses, allTasks, currentSpace, spaces ->
        if (currentSpace == null) return@combine emptyList()

        val spaceCourses = allCourses.filter { course ->
            if (course.spaceId.isNotBlank()) {
                course.spaceId == currentSpace.id
            } else {
                // Backward compatibility: match by semester name or assign to first/default space
                course.semester.equals(currentSpace.name, ignoreCase = true) ||
                    (spaces.firstOrNull()?.id == currentSpace.id)
            }
        }

        spaceCourses.map { course ->
            CourseSummary(
                course = course,
                deliveries = allTasks
                    .filter { it.courseId == course.id }
                    .sortedWith(compareBy<Task> { !it.hasDueDate }.thenBy { it.dueDate })
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Initialize default space and migrate legacy courses if needed
        viewModelScope.launch {
            val existing = spaceRepository.getActiveSpaces().firstOrNull().orEmpty()
            if (existing.isEmpty()) {
                val initialCycle = preferences.currentAcademicCycle.value.ifBlank { "Ciclo 2026-2" }
                val defaultSpace = LearningSpace(name = initialCycle, sortOrder = 0)
                spaceRepository.addSpace(defaultSpace)
                preferences.setCurrentLearningSpaceId(defaultSpace.id)
            } else {
                if (preferences.currentLearningSpaceId.value.isBlank() ||
                    existing.none { it.id == preferences.currentLearningSpaceId.value }
                ) {
                    preferences.setCurrentLearningSpaceId(existing.first().id)
                }
            }

            val legacyCourses = preferences.courses.value
            if (legacyCourses.isNotEmpty()) {
                val currentId = preferences.currentLearningSpaceId.value
                val imported = legacyCourses.all { legacy ->
                    courseRepository.addCourse(
                        Course(
                            name = legacy.name,
                            color = legacy.color,
                            spaceId = currentId,
                            semester = preferences.currentAcademicCycle.value
                        )
                    ).isSuccess
                }
                if (imported) preferences.clearLegacyCourses()
            }
        }
    }

    fun selectSpace(spaceId: String) {
        preferences.setCurrentLearningSpaceId(spaceId)
        val space = activeSpaces.value.find { it.id == spaceId }
        if (space != null) {
            preferences.setCurrentAcademicCycle(space.name)
        }
    }

    fun createSpace(name: String): Boolean {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return false
        viewModelScope.launch {
            val newSpace = LearningSpace(
                name = cleanName,
                sortOrder = activeSpaces.value.size
            )
            spaceRepository.addSpace(newSpace)
            preferences.setCurrentLearningSpaceId(newSpace.id)
            preferences.setCurrentAcademicCycle(cleanName)
        }
        return true
    }

    fun renameCurrentSpace(newName: String): Boolean {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) return false
        val current = selectedSpace.value ?: return false
        viewModelScope.launch {
            spaceRepository.updateSpace(current.copy(name = cleanName))
            preferences.setCurrentAcademicCycle(cleanName)
        }
        return true
    }

    fun archiveCurrentSpace(): Boolean {
        val current = selectedSpace.value ?: return false
        viewModelScope.launch {
            spaceRepository.archiveSpace(current.id, isArchived = true)
            val remaining = activeSpaces.value.filter { it.id != current.id }
            if (remaining.isNotEmpty()) {
                selectSpace(remaining.first().id)
            } else {
                preferences.setCurrentLearningSpaceId("")
            }
        }
        return true
    }

    fun restoreSpace(spaceId: String) {
        viewModelScope.launch {
            spaceRepository.archiveSpace(spaceId, isArchived = false)
            selectSpace(spaceId)
        }
    }

    fun deleteSpace(spaceId: String): Boolean {
        viewModelScope.launch {
            spaceRepository.deleteSpace(spaceId)
            val remaining = activeSpaces.value.filter { it.id != spaceId }
            if (remaining.isNotEmpty()) {
                selectSpace(remaining.first().id)
            } else {
                preferences.setCurrentLearningSpaceId("")
            }
        }
        return true
    }

    fun addCourse(name: String): Boolean {
        if (name.isBlank()) return false
        val palette = listOf(0xFF0EA5E9, 0xFF8B5CF6, 0xFF10B981, 0xFFF59E0B, 0xFFEC4899, 0xFF14B8A6)
        val color = palette[courses.value.size % palette.size]
        val currentSpace = selectedSpace.value
        val spaceId = currentSpace?.id.orEmpty()
        val spaceName = currentSpace?.name.orEmpty().ifBlank { currentCycle.value }

        viewModelScope.launch {
            courseRepository.addCourse(
                Course(
                    name = name.trim(),
                    color = color,
                    spaceId = spaceId,
                    semester = spaceName
                )
            )
        }
        return true
    }

    fun removeCourse(courseId: String) {
        viewModelScope.launch { courseRepository.deleteCourse(courseId) }
    }

    fun addDeliverable(course: Course, title: String, dueDate: Long, hasDueDate: Boolean = true): Boolean {
        if (title.isBlank()) return false
        val finalTimestamp = if (hasDueDate) dueDate else System.currentTimeMillis()
        viewModelScope.launch {
            taskRepository.addTask(
                Task(
                    title = title.trim(),
                    category = course.name,
                    courseId = course.id,
                    deliverableType = "DELIVERY",
                    timestamp = finalTimestamp,
                    dueDate = if (hasDueDate) dueDate else 0L,
                    hasDueDate = hasDueDate,
                    priority = "HIGH"
                )
            )
        }
        return true
    }
}
