package com.katchy.focuslive.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.repository.NoteRepository
import com.katchy.focuslive.data.service.AIService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val mascotRepository: com.katchy.focuslive.data.repository.MascotRepository,
    private val appPreferencesRepository: com.katchy.focuslive.data.repository.AppPreferencesRepository,
    private val taskRepository: com.katchy.focuslive.data.repository.TaskRepository,
    private val userStatsRepository: com.katchy.focuslive.data.repository.UserStatsRepository,
    private val aiService: AIService,
    private val ocrManager: com.katchy.focuslive.util.OcrManager
) : ViewModel() {

    val selectedMascot = mascotRepository.selectedMascot
    val isPremium = userStatsRepository.isPremium

    init {
        viewModelScope.launch {
            try {
                noteRepository.refreshNotes()
            } catch (e: Exception) {
                // Ignore network failure, local Room data is used
            }
            val existing = noteRepository.getCategories().first()
            if (existing.isEmpty()) {
                listOf("Ideas", "Proyectos", "Finanzas", "Comida").forEachIndexed { index, name ->
                    noteRepository.addCategory(
                        com.katchy.focuslive.data.model.NoteCategory(
                            name = name,
                            sortOrder = index,
                            isDefault = true
                        )
                    )
                }
            }
        }
    }

    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing = _isAiProcessing.asStateFlow()

    private val _isOcrProcessing = MutableStateFlow(false)
    val isOcrProcessing = _isOcrProcessing.asStateFlow()

    private val _isGeneratingFlashcards = MutableStateFlow(false)
    val isGeneratingFlashcards = _isGeneratingFlashcards.asStateFlow()

    suspend fun summarizeNoteText(content: String): String {
        if (content.isBlank()) return ""
        _isAiProcessing.value = true
        return try {
            aiService.summarizeText(content)
        } catch (e: Exception) {
            content
        } finally {
            _isAiProcessing.value = false
        }
    }

    suspend fun polishNoteText(content: String): String {
        if (content.isBlank()) return ""
        _isAiProcessing.value = true
        return try {
            aiService.polishText(content)
        } catch (e: Exception) {
            content
        } finally {
            _isAiProcessing.value = false
        }
    }

    fun enhanceNoteWithAI(note: Note, onSuccess: (Note) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (note.content.isBlank()) {
                onError("La nota está vacía.")
                return@launch
            }
            _isAiProcessing.value = true
            try {
                val enhanced = aiService.enhanceNoteContent(note.content)
                val updatedNote = note.copy(content = enhanced)
                noteRepository.updateNote(updatedNote)
                onSuccess(updatedNote)
            } catch (e: Exception) {
                onError(e.message ?: "Error al procesar con IA.")
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _colorFilter = MutableStateFlow<String?>(null)
    val colorFilter = _colorFilter.asStateFlow()

    // Categories Flow
    val categories = noteRepository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combine flows
    val notes: StateFlow<List<Note>> = kotlinx.coroutines.flow.combine(
        noteRepository.getNotes(),
        _searchQuery,
        _colorFilter
    ) { list, query, color ->
        list.filter { note ->
            val matchesQuery = note.title.contains(query, ignoreCase = true) || note.content.contains(query, ignoreCase = true)
            // Color filter might be deprecated by sections, but let's keep it for now as a secondary filter
            val matchesColor = color == null || note.colorHex == color
            matchesQuery && matchesColor
        }.sortedWith(
            compareByDescending<Note> { it.isPinned }
                .thenByDescending { 
                    when(it.priority) {
                        "HIGH" -> 3
                        "MEDIUM" -> 2
                        else -> 1
                    }
                }
                .thenByDescending { it.timestamp }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setColorFilter(color: String?) {
        _colorFilter.value = if(_colorFilter.value == color) null else color
    }
    
    // Trash / Papelera
    private val _trashNotes = MutableStateFlow<List<Note>>(emptyList())
    val trashNotes: StateFlow<List<Note>> = _trashNotes.asStateFlow()

    // --- Category Management ---
    fun addCategory(name: String, onCreated: (String) -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newCategory = com.katchy.focuslive.data.model.NoteCategory(name = name.trim())
            noteRepository.addCategory(newCategory)
            onCreated(newCategory.id)
        }
    }

    fun updateCategory(category: com.katchy.focuslive.data.model.NoteCategory, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            noteRepository.updateCategory(category.copy(name = newName.trim()))
        }
    }
    
    fun deleteCategory(category: com.katchy.focuslive.data.model.NoteCategory) {
        viewModelScope.launch {
            noteRepository.deleteCategory(category)
        }
    }

    fun addNote(title: String, content: String, colorHex: String, isPinned: Boolean, priority: String, categoryId: String?) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            val newNote = Note(
                title = title,
                content = content,
                colorHex = colorHex,
                isPinned = isPinned,
                priority = priority,
                categoryId = categoryId
            )
            noteRepository.addNote(newNote)
            userStatsRepository.incrementNotes()
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteRepository.updateNote(note)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            val existing = notes.value.find { it.id == noteId }
            if (existing != null) {
                _trashNotes.value = listOf(existing) + _trashNotes.value.filter { it.id != noteId }
            }
            noteRepository.deleteNote(noteId)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            _trashNotes.value = listOf(note) + _trashNotes.value.filter { it.id != note.id }
            noteRepository.deleteNote(note.id)
        }
    }

    fun restoreFromTrash(note: Note) {
        viewModelScope.launch {
            _trashNotes.value = _trashNotes.value.filter { it.id != note.id }
            noteRepository.addNote(note)
        }
    }

    fun deletePermanentlyFromTrash(noteId: String) {
        _trashNotes.value = _trashNotes.value.filter { it.id != noteId }
    }

    fun emptyTrash() {
        _trashNotes.value = emptyList()
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            _trashNotes.value = _trashNotes.value.filter { it.id != note.id }
            noteRepository.addNote(note)
        }
    }

    fun convertToTask(note: Note) {
        viewModelScope.launch {
            val taskTitle = if (note.title.isNotBlank()) note.title else note.content.take(50)
            val newTask = com.katchy.focuslive.data.model.Task(
                title = taskTitle,
                timestamp = System.currentTimeMillis(),
                isCompleted = false
            )
            taskRepository.addTask(newTask)
            noteRepository.deleteNote(note.id)
        }
    }

    fun organizeNotesWithAI(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (!isPremium.value) {
                onError("Premium required")
                return@launch
            }
            _isAiProcessing.value = true
            try {
                // 1. Get current notes and categories
                val currentNotes = notes.value
                if (currentNotes.isEmpty()) {
                    onError("No tienes notas para organizar.")
                    return@launch
                }
                val currentCategories = categories.value
                
                // 2. Call AI service
                val organizedList = aiService.organizeNotes(currentNotes, currentCategories)
                if (organizedList.isEmpty()) {
                    onError("No se pudo organizar las notas. Inténtalo de nuevo.")
                    return@launch
                }
                
                // 3. Resolve categories and update notes in DB
                val resolvedCategoryIds = mutableMapOf<String, String>()
                currentCategories.forEach { cat ->
                    resolvedCategoryIds[cat.name.lowercase().trim()] = cat.id
                }
                
                // Read from DB once again to ensure we have latest notes inside Dispatchers.IO context
                organizedList.forEach { org ->
                    val note = currentNotes.find { it.id == org.noteId }
                    if (note != null) {
                        val trimmedCatName = org.categoryName.trim()
                        val categoryId = if (trimmedCatName.isEmpty()) {
                            null
                        } else {
                            val key = trimmedCatName.lowercase()
                            if (resolvedCategoryIds.containsKey(key)) {
                                resolvedCategoryIds[key]
                            } else {
                                // Create new category
                                val newCat = com.katchy.focuslive.data.model.NoteCategory(name = trimmedCatName)
                                noteRepository.addCategory(newCat)
                                resolvedCategoryIds[key] = newCat.id
                                newCat.id
                            }
                        }
                        
                        // Update note with new categoryId (keeping title and content unchanged!)
                        noteRepository.updateNote(note.copy(categoryId = categoryId))
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("NotesViewModel", "Exception in organizeNotesWithAI", e)
                onError(e.message ?: "Ocurrió un error inesperado.")
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    fun processOcrImage(uri: android.net.Uri, onResult: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isOcrProcessing.value = true
            val result = ocrManager.extractTextFromUri(uri)
            _isOcrProcessing.value = false
            result.fold(
                onSuccess = { text ->
                    if (text.isNotBlank()) {
                        onResult(text)
                    } else {
                        onError("No se pudo detectar texto en la imagen.")
                    }
                },
                onFailure = { error ->
                    onError(error.message ?: "Error al procesar la imagen.")
                }
            )
        }
    }

    fun generateFlashcardsForNote(note: Note, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val remaining = userStatsRepository.getRemainingFlashcardGenerations()
            if (remaining == 0) {
                onError("Has alcanzado tu límite de 2 generaciones gratuitas de Flashcards. ¡Conviértete en Premium para crear mazos ilimitados!")
                return@launch
            }
            if (note.content.isBlank()) {
                onError("La nota está vacía. Agrega contenido para generar tarjetas.")
                return@launch
            }
            _isGeneratingFlashcards.value = true
            try {
                val cards = aiService.generateFlashcards(note.content)
                if (cards.isNotEmpty()) {
                    userStatsRepository.incrementFlashcardGenerationCount()
                    val updatedNote = note.copy(flashcards = cards)
                    noteRepository.updateNote(updatedNote)
                    onSuccess()
                } else {
                    onError("No se pudieron generar tarjetas. Inténtalo con otro texto.")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error al generar tarjetas.")
            } finally {
                _isGeneratingFlashcards.value = false
            }
        }
    }

    fun updateFlashcardLeitner(note: Note, cardId: String, wasCorrect: Boolean) {
        viewModelScope.launch {
            val updatedCards = note.flashcards.map { card ->
                if (card.id == cardId) {
                    val newBox = if (wasCorrect) {
                        (card.box + 1).coerceAtMost(5)
                    } else {
                        1
                    }
                    val days = when (newBox) {
                        1 -> 1
                        2 -> 3
                        3 -> 7
                        4 -> 14
                        5 -> 30
                        else -> 1
                    }
                    val nextReview = System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L
                    card.copy(
                        box = newBox,
                        lastReviewDate = System.currentTimeMillis(),
                        nextReviewDate = nextReview
                    )
                } else {
                    card
                }
            }
            noteRepository.updateNote(note.copy(flashcards = updatedCards))
        }
    }

    fun clearFlashcards(note: Note) {
        viewModelScope.launch {
            noteRepository.updateNote(note.copy(flashcards = emptyList()))
        }
    }
}
