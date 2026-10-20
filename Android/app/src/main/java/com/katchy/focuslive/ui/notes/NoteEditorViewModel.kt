package com.katchy.focuslive.ui.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.repository.CourseRepository
import com.katchy.focuslive.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject

data class NoteEditorState(
    val note: Note = Note(), val loading: Boolean = true,
    val status: String = "", val error: String? = null, val closing: Boolean = false, val unavailable: Boolean = false
)

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: NoteRepository,
    courseRepository: CourseRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val owner = auth.currentUser?.uid.orEmpty()
    private val incomingId = savedState.get<String>("noteId").orEmpty()
    /** Identificador estable de la nota, también para la transición desde su tarjeta. */
    val id = incomingId.ifBlank {
        savedState.get<String>("newNoteId") ?: UUID.randomUUID().toString().also { savedState["newNoteId"] = it }
    }
    private val mutex = Mutex()
    private var debounce: Job? = null
    private var sequence = 0L
    private var savedSequence = -1L
    private var exists = incomingId.isNotBlank()
    private val _state = MutableStateFlow(NoteEditorState())
    val state = _state.asStateFlow()
    val categories = repository.getCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val courses = courseRepository.getCourses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            runCatching {
                check(owner.isNotBlank()) { "Inicia sesión para guardar tus apuntes" }
                val stored = repository.getNote(id)
                val draft = repository.getDraft(id)
                exists = stored != null
                check(incomingId.isBlank() || stored != null || draft != null) { "Este apunte ya no está disponible" }
                val note = draft?.let { recovered -> if (stored != null) recovered.copy(revision = stored.revision) else recovered } ?: stored ?: Note(id = id, userId = owner,
                    categoryId = savedState.get<String>("folderId").orEmpty().takeIf { it.isNotBlank() },
                    courseId = savedState.get<String>("courseId").orEmpty().takeIf { it.isNotBlank() })
                check(note.deletedAt == null) { "Restaura el apunte desde la papelera para editarlo" }
                _state.value = NoteEditorState(note = note, loading = false,
                    status = if (draft != null) "Borrador recuperado" else if (stored != null) "Guardado en este dispositivo" else "Autoguardado activado")
                if (draft != null) scheduleSave() else savedSequence = sequence
            }.onFailure { _state.value = NoteEditorState(loading = false, error = it.message, unavailable = true) }
        }
    }

    fun change(transform: (Note) -> Note) {
        if (_state.value.loading || _state.value.closing || _state.value.unavailable || owner != auth.currentUser?.uid) return
        val changed = transform(_state.value.note)
        if (changed == _state.value.note) return
        sequence++
        _state.value = _state.value.copy(note = changed, status = "Guardando", error = null)
        // Draft writes are never cancelled by typing or by the debounce timer.
        viewModelScope.launch {
            mutex.withLock {
                if (savedSequence < sequence) {
                    repository.saveDraft(_state.value.note).onFailure { fail(it) }
                }
            }
        }
        scheduleSave()
    }
    private fun scheduleSave() {
        debounce?.cancel()
        debounce = viewModelScope.launch {
            delay(500)
            // Separate job: cancelling the timer must not interrupt a database write.
            viewModelScope.launch { mutex.withLock { saveLatest() } }
        }
    }
    private fun fail(error: Throwable) {
        _state.value = _state.value.copy(status = "No se pudo guardar", error = error.message ?: "Inténtalo de nuevo")
    }
    private suspend fun saveLatest(): Boolean = try {
        performSave()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        fail(e)
        false
    }
    private suspend fun performSave(): Boolean {
        if (owner != auth.currentUser?.uid || owner.isBlank()) { fail(IllegalStateException("La cuenta cambió. Tu borrador permanece en su cuenta original.")); return false }
        val snapshot = _state.value.note
        val version = sequence
        if (!exists && snapshot.title.isBlank() && snapshot.content.isBlank()) {
            val cleared = repository.clearDraft(id)
            if (cleared.isFailure) { fail(cleared.exceptionOrNull()!!); return false }
            savedSequence = version
            return true
        }
        if (version == savedSequence) return true
        _state.value = _state.value.copy(status = "Guardando")
        val draftResult = repository.saveDraft(snapshot)
        if (draftResult.isFailure) { fail(draftResult.exceptionOrNull()!!); return false }
        val result = if (exists) repository.updateNote(snapshot) else repository.addNote(snapshot)
        if (result.isFailure) { fail(result.exceptionOrNull()!!); return false }
        exists = true
        savedSequence = version
        // A successful local write increments exactly this revision. Reading again is
        // unnecessary and could turn a committed write into a spurious read failure.
        val written = snapshot.copy(revision = snapshot.revision + 1, updatedAt = System.currentTimeMillis())
        _state.value = _state.value.copy(note = _state.value.note.copy(
            revision = written.revision, updatedAt = written.updatedAt, userId = written.userId))
        if (sequence == version) {
            repository.clearDraft(id)
            _state.value = _state.value.copy(status = "Guardado en este dispositivo", error = null)
        }
        return true
    }
    fun retry() = viewModelScope.launch { mutex.withLock { saveLatest() } }
    fun flush() = viewModelScope.launch { mutex.withLock { if (!_state.value.loading) saveLatest() } }
    fun close(onClosed: () -> Unit) {
        if (_state.value.unavailable) { onClosed(); return }
        if (_state.value.closing || _state.value.loading) return
        _state.value = _state.value.copy(closing = true)
        debounce?.cancel()
        viewModelScope.launch {
            val saved = mutex.withLock { saveLatest() }
            _state.value = _state.value.copy(closing = false)
            if (saved) onClosed()
        }
    }
    fun delete(onDeleted: () -> Unit) {
        if (_state.value.closing) return
        _state.value = _state.value.copy(closing = true)
        debounce?.cancel()
        viewModelScope.launch {
            mutex.withLock {
                if (saveLatest()) repository.deleteNote(id).onSuccess { onDeleted() }.onFailure { fail(it) }
            }
            _state.value = _state.value.copy(closing = false)
        }
    }
}
