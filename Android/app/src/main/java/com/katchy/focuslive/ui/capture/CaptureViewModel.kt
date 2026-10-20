package com.katchy.focuslive.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.data.repository.HabitRepository
import com.katchy.focuslive.data.repository.NoteRepository
import com.katchy.focuslive.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SaveState { Idle, Saving, Saved }

data class CaptureUiState(
    val text: String = "",
    val forcedType: CaptureType? = null,
    val draft: CaptureDraft? = null,
    val save: SaveState = SaveState.Idle,
    val error: String? = null
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val habitRepository: HabitRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state.asStateFlow()

    fun onTextChange(text: String) = _state.update { it.copy(text = text, draft = reparse(text, it.forcedType), error = null) }

    fun onTypeChange(type: CaptureType?) = _state.update { it.copy(forcedType = type, draft = reparse(it.text, type)) }

    fun reset(prefill: String = "") = _state.update { CaptureUiState(text = prefill, draft = reparse(prefill, null)) }

    private fun reparse(text: String, forced: CaptureType?): CaptureDraft? =
        text.takeIf { it.isNotBlank() }?.let { parseCapture(it, LocalDate.now(), forced) }

    fun save(onDone: () -> Unit) {
        val current = _state.value
        val draft = current.draft
        if (draft == null || draft.title.isBlank()) {
            _state.update { it.copy(error = "Escribe qué quieres guardar.") }
            return
        }
        if (current.save != SaveState.Idle) return
        _state.update { it.copy(save = SaveState.Saving, error = null) }
        viewModelScope.launch {
            val result = runCatching { persist(draft).getOrThrow() }
            if (result.isSuccess) {
                _state.update { it.copy(save = SaveState.Saved) }
                delay(650) // deja ver el ✓ antes de cerrar
                onDone()
                reset()
            } else {
                val message = result.exceptionOrNull()?.message.orEmpty()
                _state.update {
                    it.copy(
                        save = SaveState.Idle,
                        error = if ("logged in" in message) "Inicia sesión para guardar." else "No se pudo guardar. Inténtalo de nuevo."
                    )
                }
            }
        }
    }

    private suspend fun persist(draft: CaptureDraft): Result<Unit> = when (draft.type) {
        CaptureType.NOTE -> noteRepository.addNote(Note(title = draft.title, content = ""))
        CaptureType.HABIT -> habitRepository.addHabit(
            Habit(
                title = draft.timesPerDay?.let { "${draft.title} · $it al día" } ?: draft.title,
                reminderTime = draft.time?.format(TimeFormat),
                daysOfWeek = draft.repeatDays.ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) }
            )
        )
        CaptureType.TASK, CaptureType.EVENT -> {
            val day = (draft.date ?: LocalDate.now()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val time = draft.time
            taskRepository.addTask(
                Task(
                    kind = "AGENDA",
                    title = draft.title,
                    timestamp = day,
                    dueDate = day,
                    time = time?.format(TimeFormat),
                    endTime = if (draft.type == CaptureType.EVENT) time?.let { t ->
                        if (t.hour >= 23) null else t.plusHours(1).format(TimeFormat)
                    } else null,
                    repeatMode = when {
                        draft.daily -> "DAILY"
                        draft.repeatDays.isNotEmpty() -> "CUSTOM"
                        else -> "NONE"
                    },
                    repeatDays = draft.repeatDays,
                    category = draft.area ?: "Personal"
                )
            )
        }
    }

    private companion object {
        val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
