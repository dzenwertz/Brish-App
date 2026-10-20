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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()
    fun clearError() { _error.value = null }


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
        taskRepository.getDeliveries(),
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
                    .filter { it.courseId == course.id && (it.kind == "DELIVERY" || it.deliverableType == "DELIVERY" || it.kind == "AGENDA") }
                    .sortedWith(compareBy<Task> { !it.hasDueDate }.thenBy { it.dueDate })
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Initialize default space and migrate legacy courses if needed
        viewModelScope.launch {
            try {
                val existing = spaceRepository.getActiveSpaces().firstOrNull().orEmpty()
                if (existing.isEmpty()) {
                    val initialCycle = preferences.currentAcademicCycle.value.ifBlank { "Ciclo 2026-2" }
                    val defaultSpace = LearningSpace(name = initialCycle, sortOrder = 0)
                    spaceRepository.addSpace(defaultSpace).getOrThrow()
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
                    val importedIds = courseRepository.getCourses().first().mapTo(mutableSetOf()) { it.id }
                    var importFailure: Throwable? = null
                    legacyCourses.forEach { legacy ->
                        if (legacy.id !in importedIds) {
                            courseRepository.addCourse(
                                Course(
                                    id = legacy.id,
                                    name = legacy.name,
                                    color = legacy.color,
                                    spaceId = currentId,
                                    semester = preferences.currentAcademicCycle.value
                                )
                            ).onSuccess { importedIds.add(legacy.id) }
                                .onFailure { if (importFailure == null) importFailure = it }
                        }
                    }
                    importFailure?.let { throw it }
                    preferences.clearLegacyCourses()
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.localizedMessage ?: "No se pudo cargar el espacio." }
        }
    }

    fun selectSpace(spaceId: String) {
        preferences.setCurrentLearningSpaceId(spaceId)
        val space = activeSpaces.value.find { it.id == spaceId }
        if (space != null) {
            preferences.setCurrentAcademicCycle(space.name)
        }
    }

    private fun write(onSuccess: () -> Unit, action: suspend () -> Unit) {
        if (_isSaving.value) return
        _isSaving.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                action()
                onSuccess()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "No se pudo guardar el cambio. Intenta nuevamente."
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun createSpace(name: String, onSuccess: () -> Unit = {}) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) { _error.value = "Escribe un nombre."; return }
        write(onSuccess) {
            val newSpace = LearningSpace(name = cleanName, sortOrder = activeSpaces.value.size)
            spaceRepository.addSpace(newSpace).getOrThrow()
            preferences.setCurrentLearningSpaceId(newSpace.id)
            preferences.setCurrentAcademicCycle(cleanName)
        }
    }

    fun renameCurrentSpace(newName: String, onSuccess: () -> Unit = {}) {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) { _error.value = "Escribe un nombre."; return }
        val current = selectedSpace.value ?: return
        write(onSuccess) {
            spaceRepository.updateSpace(current.copy(name = cleanName)).getOrThrow()
            preferences.setCurrentAcademicCycle(cleanName)
        }
    }

    fun archiveCurrentSpace(onSuccess: () -> Unit = {}) {
        val current = selectedSpace.value ?: return
        write(onSuccess) {
            spaceRepository.archiveSpace(current.id, isArchived = true).getOrThrow()
            selectRemainingSpace(current.id)
        }
    }

    fun restoreSpace(spaceId: String, onSuccess: () -> Unit = {}) {
        val restored = archivedSpaces.value.find { it.id == spaceId }
            ?: activeSpaces.value.find { it.id == spaceId }
        write(onSuccess) {
            spaceRepository.archiveSpace(spaceId, isArchived = false).getOrThrow()
            preferences.setCurrentLearningSpaceId(spaceId)
            restored?.let { preferences.setCurrentAcademicCycle(it.name) }
        }
    }

    private fun selectRemainingSpace(excludedId: String) {
        val remaining = activeSpaces.value.filter { it.id != excludedId }
        if (remaining.isNotEmpty()) selectSpace(remaining.first().id)
        else preferences.setCurrentLearningSpaceId("")
    }

    fun deleteSpace(spaceId: String, onSuccess: () -> Unit = {}) {
        write(onSuccess) {
            spaceRepository.deleteSpace(spaceId).getOrThrow()
            if (currentSpaceId.value == spaceId) selectRemainingSpace(spaceId)
        }
    }

    fun addCourse(name: String, onSuccess: () -> Unit = {}) {
        if (name.isBlank()) { _error.value = "Escribe un nombre."; return }
        val currentSpace = selectedSpace.value
        if (currentSpace == null) { _error.value = "Crea o selecciona un espacio primero."; return }
        val palette = listOf(0xFF0EA5E9, 0xFF8B5CF6, 0xFF10B981, 0xFFF59E0B, 0xFFEC4899, 0xFF14B8A6)
        val color = palette[courses.value.size % palette.size]
        write(onSuccess) {
            courseRepository.addCourse(Course(name = name.trim(), color = color,
                spaceId = currentSpace.id, semester = currentSpace.name)).getOrThrow()
        }
    }

    fun removeCourse(courseId: String, onSuccess: () -> Unit = {}) {
        write(onSuccess) { courseRepository.deleteCourse(courseId).getOrThrow() }
    }
}
