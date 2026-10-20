package com.katchy.focuslive.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.model.NoteCategory
import com.katchy.focuslive.data.repository.CourseRepository
import com.katchy.focuslive.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository,
    courseRepository: CourseRepository
) : ViewModel() {
    val notes = combine(repository.getNotes(), repository.getDrafts()) { saved, drafts ->
        (saved + drafts.filter { it.deletedAt == null && (it.title.isNotBlank() || it.content.isNotBlank()) })
            .associateBy { it.id }.values.sortedByDescending { it.updatedAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = repository.getCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val courses = courseRepository.getCourses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val trashNotes = repository.getTrashNotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    fun dismissError() { _error.value = null }
    private fun write(action: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try { action() }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _error.value = e.message ?: "No se pudo guardar el cambio" }
            finally { _busy.value = false }
        }
    }
    fun addCategory(name: String, onCreated: (String) -> Unit = {}) {
        if (name.isBlank()) return
        write {
                val category = NoteCategory(name = name.trim())
                repository.addCategory(category)
                onCreated(category.id)
        }
    }
    fun updateCategory(category: NoteCategory, name: String, onSaved: () -> Unit = {}) {
        if (name.isBlank()) return
        write {
            repository.updateCategory(category.copy(name = name.trim()))
            onSaved()
        }
    }
    fun deleteCategory(category: NoteCategory, onDeleted: () -> Unit = {}) {
        write {
            repository.deleteCategory(category)
            onDeleted()
        }
    }
    fun restoreFromTrash(note: Note) = write {
        repository.restoreNote(note.id).getOrThrow()
    }
    fun deletePermanentlyFromTrash(noteId: String, onDeleted: () -> Unit = {}) = write {
        repository.deletePermanently(noteId).getOrThrow()
        onDeleted()
    }
    fun clearFlashcards(note: Note, onSaved: () -> Unit = {}) = write {
        val current = repository.getNote(note.id) ?: error("Este apunte ya no está disponible")
        repository.updateNote(current.copy(flashcards = emptyList())).getOrThrow()
        onSaved()
    }
    fun updateFlashcardLeitner(note: Note, cardId: String, wasCorrect: Boolean, onSaved: () -> Unit = {}) = write {
        val current = repository.getNote(note.id) ?: error("Este apunte ya no está disponible")
        val cards = current.flashcards.map { card ->
            if (card.id != cardId) card else {
                val box = if (wasCorrect) (card.box + 1).coerceAtMost(5) else 1
                val days = listOf(1, 3, 7, 14, 30)[box - 1]
                card.copy(box = box, lastReviewDate = System.currentTimeMillis(),
                    nextReviewDate = System.currentTimeMillis() + days * 86400000L)
            }
        }
        repository.updateNote(current.copy(flashcards = cards)).getOrThrow()
        onSaved()
    }
}
