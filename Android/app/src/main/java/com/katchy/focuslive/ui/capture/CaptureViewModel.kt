package com.katchy.focuslive.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.draft.DraftStore
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
    val error: String? = null,
    val recovered: Boolean = false,
    val savedIndicator: Boolean = false
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val habitRepository: HabitRepository,
    private val scheduler: com.katchy.focuslive.scheduler.NotificationScheduler,
    private val draftStore: DraftStore
) : ViewModel() {
    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state.asStateFlow()
    private var indicatorJob: kotlinx.coroutines.Job? = null
    private var dirty = false

    init {
        viewModelScope.launch {
            val savedText = draftStore.read(DRAFT_KEY)
            val savedType = draftStore.read(DRAFT_TYPE_KEY)?.let { name -> CaptureType.entries.find { it.name == name } }
            if (savedText != null || savedType != null) {
                val text = savedText.orEmpty()
                _state.update {
                    it.copy(
                        text = text,
                        forcedType = savedType,
                        draft = reparse(text, savedType),
                        error = null,
                        recovered = !savedText.isNullOrBlank()
                    )
                }
            }
        }
    }

    fun onTextChange(text: String) {
        dirty = true
        _state.update { it.copy(text = text, draft = reparse(text, it.forcedType), error = null, recovered = false, savedIndicator = false) }
        val toStore = text.takeIf(String::isNotBlank)
        draftStore.scheduleWrite(DRAFT_KEY, toStore)
        indicatorJob?.cancel()
        if (toStore != null) {
            indicatorJob = viewModelScope.launch {
                delay(DraftStore.WRITE_DEBOUNCE_MS + 80L)
                _state.update { it.copy(savedIndicator = true) }
                delay(1200L)
                _state.update { it.copy(savedIndicator = false) }
            }
        }
    }

    fun onTypeChange(type: CaptureType?) {
        dirty = true
        _state.update { it.copy(forcedType = type, draft = reparse(it.text, type)) }
        draftStore.scheduleWrite(DRAFT_TYPE_KEY, type?.name)
    }

    fun flushDraft() {
        if (!dirty) return
        val current = _state.value
        draftStore.writeBlocking(DRAFT_KEY, current.text.takeIf(String::isNotBlank))
        draftStore.writeBlocking(DRAFT_TYPE_KEY, current.forcedType?.name)
    }

    fun reset(prefill: String = "") {
        dirty = prefill.isNotBlank()
        indicatorJob?.cancel()
        _state.update { CaptureUiState(text = prefill, draft = reparse(prefill, null)) }
        draftStore.scheduleWrite(DRAFT_KEY, prefill.takeIf(String::isNotBlank))
        draftStore.scheduleWrite(DRAFT_TYPE_KEY, null)
    }

    fun discard() {
        dirty = false
        indicatorJob?.cancel()
        _state.update { CaptureUiState() }
        draftStore.writeBlocking(DRAFT_KEY, null)
        draftStore.writeBlocking(DRAFT_TYPE_KEY, null)
    }

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
                dirty = false
                indicatorJob?.cancel()
                _state.update { it.copy(save = SaveState.Saved, savedIndicator = false, recovered = false) }
                draftStore.writeNow(DRAFT_KEY, null)
                draftStore.writeNow(DRAFT_TYPE_KEY, null)
                delay(350) // deja ver el ✓ un instante antes de cerrar
                reset()
                onDone()
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
        CaptureType.HABIT -> {
            val habit = Habit(
                title = draft.timesPerDay?.let { "${draft.title} · $it al día" } ?: draft.title,
                reminderTime = draft.time?.format(TimeFormat),
                daysOfWeek = draft.repeatDays.ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) }
            )
            habitRepository.addHabit(habit).onSuccess { scheduler.scheduleHabitReminder(habit) }
        }
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
        const val DRAFT_KEY = "capture:input"
        const val DRAFT_TYPE_KEY = "capture:type"
        val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
