package com.katchy.focuslive.ui.chat

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.JsonParser
import com.katchy.focuslive.data.api.OpenCodeMessage
import com.katchy.focuslive.data.local.dao.ChatDao
import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.model.NoteCategory
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.data.repository.AppPreferencesRepository
import com.katchy.focuslive.data.repository.FinanceRepository
import com.katchy.focuslive.data.repository.CourseRepository
import com.katchy.focuslive.data.repository.HabitRepository
import com.katchy.focuslive.data.repository.MascotRepository
import com.katchy.focuslive.data.repository.NoteRepository
import com.katchy.focuslive.data.repository.OpenCodeRepository
import com.katchy.focuslive.data.repository.TaskRepository
import com.katchy.focuslive.data.repository.UserStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

@Keep
@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessage(
    @PrimaryKey val id: String = "",
    val sessionId: String = "",
    val sender: String = "", // "USER" or "BRISH"
    val text: String = "",
    val timestamp: Long = 0,
    val actionPayload: String? = null,
    val actionsExecuted: Boolean = false,
    val actionsDismissed: Boolean = false
)

@Keep
@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String = "",
    val title: String = "Nueva conversación",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val previewText: String = "",
    val messageCount: Int = 0
)

@Keep
@Entity(tableName = "user_ai_memory")
data class UserAiMemory(
    @PrimaryKey val id: String = "profile",
    val summaryBio: String = "",
    val keyFacts: List<String> = emptyList(),
    val preferences: Map<String, String> = emptyMap(),
    val lastUpdated: Long = System.currentTimeMillis()
)

data class ChatUiError(
    val message: String,
    val failedPrompt: String? = null
)

internal fun extractTaskTitlesFromAiText(text: String): List<String> {
    val bulletPattern = Regex("^\\s*(?:[-*•]|\\d+[.)]|\\[[ xX]?])\\s+(.+)$")
    return text.lineSequence()
        .mapNotNull { line -> bulletPattern.matchEntire(line)?.groupValues?.getOrNull(1)?.trim() }
        .map { it.replace(Regex("^[✅☐☑️\\s]+"), "").trim() }
        .filter { it.length in 3..100 }
        .distinctBy { it.lowercase(Locale.getDefault()) }
        .take(8)
        .toList()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val openCodeRepository: OpenCodeRepository,
    private val chatDao: ChatDao,
    private val mascotRepository: MascotRepository,
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository,
    private val courseRepository: CourseRepository,
    private val financeRepository: FinanceRepository,
    private val habitRepository: HabitRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val userStatsRepository: UserStatsRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val spaceRepository: com.katchy.focuslive.data.repository.LearningSpaceRepository,
    private val analyticsHelper: com.katchy.focuslive.util.AnalyticsHelper
) : ViewModel() {

    companion object {
        var initialPrompt: String? = null
        private val _isGeneratingGlobal = MutableStateFlow(false)
        val sessionStartTime = System.currentTimeMillis()
    }

    private var hasHandledInitialPrompt = false

    private val _currentSessionId = MutableStateFlow(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    // 1. Reactive Chat Sessions directly from Room Local Database (works 100% offline & instantly)
    val sessions: StateFlow<List<ChatSession>> = chatDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Reactive User AI Memory from Room Local Database
    val userAiMemory: StateFlow<UserAiMemory> = chatDao.getUserMemory()
        .map { it ?: UserAiMemory() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserAiMemory())

    // 3. Reactive Messages for Current Session from Room
    val messages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { sessionId ->
            chatDao.getMessagesForSession(sessionId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isGenerating: StateFlow<Boolean> = _isGeneratingGlobal.asStateFlow()

    private val _chatError = MutableStateFlow<ChatUiError?>(null)
    val chatError: StateFlow<ChatUiError?> = _chatError.asStateFlow()

    private val _isHistoryDrawerOpen = MutableStateFlow(false)
    val isHistoryDrawerOpen: StateFlow<Boolean> = _isHistoryDrawerOpen.asStateFlow()

    private val _isMemoryDialogOpen = MutableStateFlow(false)
    val isMemoryDialogOpen: StateFlow<Boolean> = _isMemoryDialogOpen.asStateFlow()

    val selectedMascot = mascotRepository.selectedMascot

    init {
        analyticsHelper.logScreenView("chat_screen")
        
        // Check if there are existing sessions, if so, load the latest one, otherwise start new
        viewModelScope.launch(Dispatchers.IO) {
            val existing = chatDao.getAllSessions().first()
            if (existing.isNotEmpty()) {
                _currentSessionId.value = existing.first().id
            }
            handlePendingInitialPrompt()
        }
    }

    fun openHistoryDrawer() {
        _isHistoryDrawerOpen.value = true
    }

    fun closeHistoryDrawer() {
        _isHistoryDrawerOpen.value = false
    }

    fun openMemoryDialog() {
        _isMemoryDialogOpen.value = true
    }

    fun closeMemoryDialog() {
        _isMemoryDialogOpen.value = false
    }

    fun startNewChat() {
        val newSessionId = UUID.randomUUID().toString()
        _currentSessionId.value = newSessionId
        _isHistoryDrawerOpen.value = false
        _chatError.value = null
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        _isHistoryDrawerOpen.value = false
        _chatError.value = null
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatDao.deleteSessionAndMessages(sessionId)
                
                // Also delete from Firestore if user is authenticated
                val uid = firebaseAuth.currentUser?.uid
                if (uid != null) {
                    try {
                        firestore.collection("users").document(uid)
                            .collection("chat_sessions").document(sessionId)
                            .delete()
                    } catch (e: Exception) {
                        // Safe ignore
                    }
                }

                if (_currentSessionId.value == sessionId) {
                    val remaining = chatDao.getAllSessions().first()
                    if (remaining.isNotEmpty()) {
                        _currentSessionId.value = remaining.first().id
                    } else {
                        startNewChat()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error deleting chat session", e)
            }
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = chatDao.getSessionById(sessionId)
                if (existing != null) {
                    chatDao.updateSession(existing.copy(title = newTitle.trim(), updatedAt = System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error renaming session", e)
            }
        }
    }

    fun addKeyFact(fact: String) {
        if (fact.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val currentFacts = userAiMemory.value.keyFacts.toMutableList()
            if (!currentFacts.contains(fact.trim())) {
                currentFacts.add(0, fact.trim())
                val updated = userAiMemory.value.copy(keyFacts = currentFacts, lastUpdated = System.currentTimeMillis())
                chatDao.saveUserMemory(updated)
            }
        }
    }

    fun deleteKeyFact(fact: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentFacts = userAiMemory.value.keyFacts.filter { it != fact }
            val updated = userAiMemory.value.copy(keyFacts = currentFacts, lastUpdated = System.currentTimeMillis())
            chatDao.saveUserMemory(updated)
        }
    }

    fun clearMemory() {
        viewModelScope.launch(Dispatchers.IO) {
            val emptyMemory = UserAiMemory(summaryBio = "", keyFacts = emptyList(), preferences = emptyMap())
            chatDao.saveUserMemory(emptyMemory)
        }
    }

    private fun handlePendingInitialPrompt() {
        if (hasHandledInitialPrompt) return
        val prompt = initialPrompt ?: return
        hasHandledInitialPrompt = true
        initialPrompt = null
        sendMessage(prompt)
    }

    fun clearChatHistory() {
        startNewChat()
    }

    fun sendMessage(text: String) = sendMessageInternal(text, persistUserMessage = true)

    fun retryLastMessage() {
        val failedPrompt = _chatError.value?.failedPrompt ?: return
        if (_isGeneratingGlobal.value) return
        sendMessageInternal(failedPrompt, persistUserMessage = false)
    }

    fun dismissError() {
        _chatError.value = null
    }

    private fun sendMessageInternal(text: String, persistUserMessage: Boolean) {
        if (text.isBlank() || _isGeneratingGlobal.value) return
        val sessionId = _currentSessionId.value
        _chatError.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val currentMsgs = messages.value

            // 1. Check for quick affirmative/rejection auto-actions on pending proposals
            val lastBrishMsg = currentMsgs.lastOrNull { it.sender == "BRISH" }
            if (persistUserMessage && lastBrishMsg != null && lastBrishMsg.actionPayload != null &&
                !lastBrishMsg.actionsExecuted && !lastBrishMsg.actionsDismissed
            ) {
                val cleanInput = text.trim().lowercase().replace(Regex("[¡!¿?.,]"), "")
                val affirmations = setOf("si", "sí", "ok", "okey", "dale", "aplica", "hazlo", "procede", "yes", "go", "claro", "bueno", "confirmar", "confirmo", "sii", "síí", "aceptar", "acepto", "perfecto", "listo")
                val rejections = setOf("no", "cancelar", "cancela", "tampoco", "no hacer nada", "rechazar", "rechazo")

                if (cleanInput in affirmations) {
                    val userMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        sender = "USER",
                        text = text,
                        timestamp = System.currentTimeMillis()
                    )
                    chatDao.insertMessage(userMsg)
                    executeActions(lastBrishMsg.id, lastBrishMsg.actionPayload)

                    val brishReply = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        sender = "BRISH",
                        text = "¡Excelente! He aplicado los cambios en tu app con éxito.",
                        timestamp = System.currentTimeMillis() + 100
                    )
                    chatDao.insertMessage(brishReply)
                    return@launch
                } else if (cleanInput in rejections) {
                    val userMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        sender = "USER",
                        text = text,
                        timestamp = System.currentTimeMillis()
                    )
                    chatDao.insertMessage(userMsg)
                    dismissActions(lastBrishMsg.id)

                    val brishReply = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        sender = "BRISH",
                        text = "Entendido, he cancelado las acciones propuestas.",
                        timestamp = System.currentTimeMillis() + 100
                    )
                    chatDao.insertMessage(brishReply)
                    return@launch
                }
            }

            // 2. Normal user message dispatch
            val userMsg = if (persistUserMessage) {
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    sender = "USER",
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                currentMsgs.lastOrNull { it.sender == "USER" && it.text == text } ?: ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    sender = "USER",
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
            }

            // Save user message to Room immediately (instant UI update)
            if (persistUserMessage) chatDao.insertMessage(userMsg)

            // Ensure session exists or create/update it in Room
            val isFirstMessage = currentMsgs.isEmpty()
            val existingSession = chatDao.getSessionById(sessionId)
            val sessionTitle = if (isFirstMessage || existingSession == null) generateSmartTitle(text) else existingSession.title

            val sessionDoc = ChatSession(
                id = sessionId,
                title = sessionTitle,
                createdAt = existingSession?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                previewText = text.take(60),
                messageCount = (existingSession?.messageCount ?: 0) + if (persistUserMessage) 1 else 0
            )
            chatDao.insertSession(sessionDoc)

            // 3. Query LLM asynchronously
            _isGeneratingGlobal.value = true
            val historyCopy = if (persistUserMessage) currentMsgs + userMsg else currentMsgs

            try {
                // Fetch real-time user context across the app from local DAOs
                val allTasksDeferred = async { try { taskRepository.getAllTasks().first() } catch (e: Exception) { emptyList() } }
                val habitsDeferred = async { try { habitRepository.getHabits().first() } catch (e: Exception) { emptyList() } }
                val notesDeferred = async { try { noteRepository.getNotes().first() } catch (e: Exception) { emptyList() } }
                val transactionsDeferred = async { try { financeRepository.getTransactions().first() } catch (e: Exception) { emptyList() } }
                val coursesDeferred = async { try { courseRepository.getCourses().first() } catch (e: Exception) { emptyList() } }
                val spacesDeferred = async { try { spaceRepository.getSpaces().first() } catch (e: Exception) { emptyList() } }

                val allTasks = allTasksDeferred.await()
                val habits = habitsDeferred.await()
                val notes = notesDeferred.await()
                val transactions = transactionsDeferred.await()
                val courses = coursesDeferred.await()
                val spaces = spacesDeferred.await()
                val courseNames = courses.associate { it.id to it.name }

                val nowCal = Calendar.getInstance()
                val fullDateFormat = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val currentFormattedDate = fullDateFormat.format(nowCal.time)
                val currentFormattedTime = timeFormat.format(nowCal.time)
                val currentTimeZone = TimeZone.getDefault().id
                val currentMinutesOfDay = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
                val todayDayOfWeek = nowCal.get(Calendar.DAY_OF_WEEK)

                val todayCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayStartOfDay = todayCal.timeInMillis

                val todayTasks = mutableListOf<Task>()
                val overdueTasks = mutableListOf<Task>()
                val undatedDeliveries = mutableListOf<Task>()

                allTasks.forEach { task ->
                    if (!task.hasDueDate) {
                        if (!task.isCompleted) undatedDeliveries.add(task)
                        return@forEach
                    }

                    val isScheduledForToday = when (task.repeatMode) {
                        "DAILY" -> task.timestamp <= nowCal.timeInMillis
                        "WEEKLY", "CUSTOM" -> task.timestamp <= nowCal.timeInMillis && task.repeatDays.contains(todayDayOfWeek)
                        else -> {
                            val taskCal = Calendar.getInstance().apply { timeInMillis = task.dueDate }
                            taskCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                            taskCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
                        }
                    }

                    if (isScheduledForToday) {
                        val completedToday = task.repeatMode != "NONE" && task.completedDates.any {
                            com.katchy.focuslive.util.HabitUtils.isSameDay(it, todayStartOfDay)
                        }
                        todayTasks.add(if (task.repeatMode == "NONE") task else task.copy(isCompleted = completedToday))
                    } else if (task.dueDate < todayStartOfDay && !task.isCompleted && task.repeatMode == "NONE") {
                        overdueTasks.add(task)
                    }
                }

                // Sort today's tasks chronologically
                todayTasks.sortBy { it.time ?: "23:59" }

                // Check for ongoing class or task right now
                val ongoingTask = todayTasks.filterNot { it.isCompleted }.find { task ->
                    if (!task.time.isNullOrBlank()) {
                        val startParts = task.time.split(":")
                        val startMin = (startParts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (startParts.getOrNull(1)?.toIntOrNull() ?: 0)
                        val endMin = if (!task.endTime.isNullOrBlank()) {
                            val endParts = task.endTime.split(":")
                            (endParts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (endParts.getOrNull(1)?.toIntOrNull() ?: 0)
                        } else {
                            startMin + 60
                        }
                        currentMinutesOfDay in startMin..endMin
                    } else false
                }

                val nextTask = todayTasks.filterNot { it.isCompleted }.find { task ->
                    if (!task.time.isNullOrBlank()) {
                        val startParts = task.time.split(":")
                        val startMin = (startParts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (startParts.getOrNull(1)?.toIntOrNull() ?: 0)
                        startMin > currentMinutesOfDay
                    } else false
                }

                val tasksContext = StringBuilder().apply {
                    if (ongoingTask != null) {
                        val range = if (!ongoingTask.endTime.isNullOrBlank()) "${ongoingTask.time} - ${ongoingTask.endTime}" else ongoingTask.time
                        append("[EN CURSO AHORA MISMO] (Hora actual: $currentFormattedTime):\n")
                        append("  -> ${ongoingTask.title} ($range) [Categoría: ${ongoingTask.category}]\n\n")
                    }
                    if (nextTask != null) {
                        val range = if (!nextTask.endTime.isNullOrBlank()) "${nextTask.time} - ${nextTask.endTime}" else nextTask.time
                        append("PRÓXIMA ACTIVIDAD / CLASE HOY:\n")
                        append("  -> ${nextTask.title} (Hora: $range) [Categoría: ${nextTask.category}]\n\n")
                    }

                    append("--- HORARIO Y TAREAS DE HOY ($currentFormattedDate) ---\n")
                    if (todayTasks.isEmpty()) {
                        append("No hay tareas ni clases programadas para hoy en la app.\n")
                    } else {
                        todayTasks.forEach { task ->
                            val status = if (task.isCompleted) "COMPLETADA" else "PENDIENTE"
                            val timeStr = when {
                                !task.time.isNullOrBlank() && !task.endTime.isNullOrBlank() -> " (${task.time} - ${task.endTime})"
                                !task.time.isNullOrBlank() -> " (${task.time})"
                                else -> " (Todo el día)"
                            }
                            val subtasksStr = if (task.subtasks.isNotEmpty()) {
                                " [Subtareas: " + task.subtasks.joinToString(", ") { "${it.title}${if (it.isCompleted) "✓" else ""}" } + "]"
                            } else ""
                            val course = task.courseId?.let(courseNames::get)
                            val contextLabel = course ?: task.category
                            append("- [ID: ${task.id}] ${task.title}$timeStr ($contextLabel) [$status]$subtasksStr\n")
                        }
                    }

                    // Weekly recurring / schedule
                    val weeklyTasks = allTasks.filter { it.repeatMode in setOf("WEEKLY", "CUSTOM", "DAILY") }
                    if (weeklyTasks.isNotEmpty()) {
                        append("\n--- HORARIO SEMANAL / CLASES RECURRENTES ---\n")
                        val dayNames = listOf("", "Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
                        weeklyTasks.forEach { task ->
                            val daysStr = if (task.repeatMode == "DAILY") "Todos los días" else task.repeatDays.mapNotNull { dayNames.getOrNull(it) }.joinToString(", ")
                            val timeStr = if (!task.time.isNullOrBlank()) " a las ${task.time}${if (!task.endTime.isNullOrBlank()) " - ${task.endTime}" else ""}" else ""
                            append("- [ID: ${task.id}] ${task.title}$timeStr ($daysStr) [${task.category}]\n")
                        }
                    }

                    if (undatedDeliveries.isNotEmpty()) {
                        append("\n--- ENTREGAS Y TAREAS PENDIENTES SIN FECHA LÍMITE ---\n")
                        undatedDeliveries.forEach { task ->
                            val course = task.courseId?.let(courseNames::get)
                            val contextLabel = course ?: task.category
                            append("- [ID: ${task.id}] ${task.title} ($contextLabel) [Sin fecha límite]\n")
                        }
                    }

                    if (overdueTasks.isNotEmpty()) {
                        append("\n--- TAREAS ATRASADAS PENDIENTES ---\n")
                        overdueTasks.take(5).forEach { task ->
                            append("- [ID: ${task.id}] ${task.title} (${task.category}) [VENCIDA PENDIENTE]\n")
                        }
                    }
                }.toString()

                val spacesMap = spaces.associate { it.id to it.name }
                val currentSpaceId = appPreferencesRepository.currentLearningSpaceId.value
                val activeSpace = spaces.find { it.id == currentSpaceId }

                val coursesContext = if (courses.isEmpty()) {
                    "Sin cursos registrados."
                } else {
                    val sb = StringBuilder()
                    if (activeSpace != null) {
                        sb.append("Espacio de aprendizaje activo: ${activeSpace.name}\n")
                    }
                    courses.forEach { course ->
                        val spaceName = spacesMap[course.spaceId] ?: course.semester.ifBlank { "General" }
                        val pendingWithDate = allTasks.count { it.courseId == course.id && !it.isCompleted && it.hasDueDate }
                        val pendingNoDate = allTasks.count { it.courseId == course.id && !it.isCompleted && !it.hasDueDate }
                        sb.append("- [ID: ${course.id}] ${course.name} [Espacio: $spaceName] · $pendingWithDate entregas con fecha, $pendingNoDate sin fecha límite\n")
                    }
                    sb.toString().trimEnd()
                }

                val habitsContext = StringBuilder().apply {
                    append("--- HÁBITOS DE HOY ---\n")
                    val todayHabits = habits.filter { it.daysOfWeek.isEmpty() || it.daysOfWeek.contains(todayDayOfWeek) }
                    if (todayHabits.isEmpty()) {
                        append("Sin hábitos para hoy.\n")
                    } else {
                        todayHabits.forEach { habit ->
                            val isCompletedToday = habit.completedDates.any {
                                val c = Calendar.getInstance().apply { timeInMillis = it; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                                c.timeInMillis == todayStartOfDay
                            }
                            val status = if (isCompletedToday) "COMPLETADO" else "PENDIENTE"
                            append("- [ID: ${habit.id}] ${habit.icon} ${habit.title} (Racha: ${habit.currentStreak}d) [$status]\n")
                        }
                    }
                }.toString()

                val notesContext = if (notes.isNotEmpty()) {
                    notes.take(10).joinToString("\n") { "- [ID: ${it.id}] ${it.title}: ${it.content.take(120)}" }
                } else "Sin notas registradas."

                val financeContext = if (transactions.isNotEmpty()) {
                    val inc = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                    val exp = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    val recent = transactions.take(4).joinToString(", ") { "${it.title} (${if (it.type == "INCOME") "+" else "-"}S/ ${"%.2f".format(Locale.US, it.amount)})" }
                    "Balance: +S/ ${"%.2f".format(Locale.US, inc)} | -S/ ${"%.2f".format(Locale.US, exp)}. Recientes: $recent"
                } else "Sin transacciones."

                // Long-term memory context
                val memory = userAiMemory.value
                val memoryContext = StringBuilder().apply {
                    if (memory.summaryBio.isNotBlank()) {
                        append("Resumen del usuario: ${memory.summaryBio}\n")
                    }
                    if (memory.keyFacts.isNotEmpty()) {
                        append("Hechos que recuerdas del usuario de conversaciones previas:\n")
                        memory.keyFacts.forEach { append("  * $it\n") }
                    }
                    if (memory.summaryBio.isBlank() && memory.keyFacts.isEmpty()) {
                        append("Aún no tienes recuerdos previos. Aprende proactivamente de lo que el usuario comparta en esta sesión.\n")
                    }
                }.toString()

                val stats = userStatsRepository.userStats.value
                val userName = firebaseAuth.currentUser?.displayName ?: "Invitado"
                val firstName = userName.split(" ").firstOrNull()?.ifBlank { "Invitado" } ?: "Invitado"

                val systemInstruction = """
                    Eres Brish AI, el copiloto inteligente de productividad, enfoque y organización personal de la aplicación Brish.
                    
                    IDENTIDAD Y CREADOR:
                    - Tu nombre es Brish AI (o Brish).
                    - Fuiste creado por el Ingeniero Joseph. Si te preguntan quién te creó, responde con respeto que fuiste desarrollado por el Ingeniero Joseph.
                    - Te diriges al usuario por su primer nombre: $firstName.
                    
                    CONTEXTO TEMPORAL ACTUAL:
                    - Hoy es: $currentFormattedDate.
                    - Hora exacta actual: $currentFormattedTime.
                    - Zona horaria del dispositivo: $currentTimeZone.
                    - "mañana" = dueDateOffsetDays: 1. "pasado mañana" = dueDateOffsetDays: 2. "hoy" = dueDateOffsetDays: 0.
                    
                    [MEMORIA A LARGO PLAZO DE CONVERSACIONES ANTERIORES]
                    $memoryContext
                    
                    [ESTADO EN TIEMPO REAL EN LA APP]
                    - Nivel: ${stats.currentLevel} | Racha: ${stats.streak} días | Minutos de enfoque: ${stats.totalFocusMinutes}
                    
                    [AGENDA / HORARIO / TAREAS EN VIVO]
                    $tasksContext

                    [CURSOS DEL CICLO]
                    $coursesContext
                    
                    [HÁBITOS]
                    $habitsContext
                    
                    [NOTAS]
                    $notesContext
                    
                    [FINANZAS]
                    $financeContext
                    
                    REGLAS CRÍTICAS DE IDIOMA Y ESTILO:
                    1. IDIOMA ESTRICTO: Responde SIEMPRE 100% en ESPAÑOL natural, fluido y amigable. NUNCA uses caracteres chinos ni mezcles idiomas bajo ninguna circunstancia.
                    2. SIN FORMATO EXTRAÑO: NO uses asteriscos (**) ni texto en negrita de Markdown. Escribe de forma limpia, moderna y fluida.
                    3. HORARIOS Y CLASES: Conoces el horario real del usuario mostrado arriba en [AGENDA / HORARIO / TAREAS EN VIVO]. Si te pregunta qué clase o tarea tiene en este momento, responde directamente indicando su actividad según la hora actual ($currentFormattedTime).
                    4. CONTINUIDAD: No saludes de forma robótica ni repitas "Hola" constantemente. Sé conciso, cálido y agradable.
                    - Usa los datos actuales de Brish como fuente principal. No inventes cursos, entregas, fechas ni resultados de acciones.
                    - Distingue propuestas de cambios guardados. El bloque de acciones es una propuesta que el usuario debe revisar; no digas que ya guardaste o eliminaste datos.
                    - Si falta un dato esencial para una acción, pregunta solo por ese dato. Para estudiar, sugiere un siguiente paso breve y concreto.
                    
                    5. ACCIONES AGÉNTICAS AUTOMÁTICAS (AGENT_ACTION):
                       - Cuando el usuario solicite agendar, crear, modificar, borrar o registrar tareas, hábitos, notas, finanzas o pomodoro, responde amablemente y agrega al final el bloque JSON exacto:
                       
                       ```json
                       {
                         "intent": "AGENT_ACTION",
                         "actions": [
                           {
                             "type": "CREATE_TASK",
                             "title": "Título de la tarea",
                             "category": "General/Estudio/Trabajo/Personal/Salud",
                             "dueDateOffsetDays": 0,
                             "time": "HH:mm (Opcional)",
                             "repeatMode": "NONE/DAILY/WEEKLY (Opcional)",
                             "priority": "HIGH/MEDIUM/LOW (Opcional)",
                             "description": "Detalle breve (Opcional)"
                           },
                           {
                             "type": "UPDATE_TASK",
                             "id": "ID_EXACTO_DE_LA_TAREA_DEL_CONTEXTO",
                             "title": "Nuevo título (Opcional)",
                             "time": "HH:mm (Opcional)",
                             "isCompleted": false
                           },
                           {
                             "type": "DELETE_TASK",
                             "id": "ID_EXACTO_DE_LA_TAREA_DEL_CONTEXTO"
                           },
                           {
                             "type": "CREATE_HABIT",
                             "title": "Nombre del hábito",
                             "icon": "spa",
                             "reminderTime": "HH:mm (Opcional)",
                             "daysOfWeek": [1, 2, 3, 4, 5, 6, 7]
                           },
                           {
                             "type": "UPDATE_HABIT",
                             "id": "ID_EXACTO_DEL_HABITO",
                             "title": "Nuevo título"
                           },
                           {
                             "type": "DELETE_HABIT",
                             "id": "ID_EXACTO_DEL_HABITO"
                           },
                           {
                             "type": "CREATE_NOTE",
                             "title": "Título de la nota",
                             "content": "Contenido en Markdown"
                           },
                           {
                             "type": "SET_POMODORO",
                             "focusMinutes": 25,
                             "breakMinutes": 5
                           },
                           {
                             "type": "CREATE_TRANSACTION",
                             "title": "Descripción",
                             "amount": 15.0,
                             "transactionType": "EXPENSE",
                             "category": "Comida"
                           }
                         ]
                       }
                       ```
                    
                    REGLAS ANTI-BUGS:
                    - Nunca inventes IDs ficticios.
                    - Solo menciona confirmación con botones cuando incluyas el bloque JSON `AGENT_ACTION`.
                """.trimIndent()

                val openCodeMessages = mutableListOf<OpenCodeMessage>()
                openCodeMessages.add(OpenCodeMessage(role = "system", content = systemInstruction))

                historyCopy.takeLast(12).forEach { msg ->
                    val role = if (msg.sender == "USER") "user" else "assistant"
                    openCodeMessages.add(OpenCodeMessage(role = role, content = msg.text))
                }

                val apiResult = openCodeRepository.getChatCompletion(openCodeMessages)

                apiResult.fold(
                    onSuccess = { reply ->
                        val (cleanText, actionPayload) = extractActionPayload(reply)

                        val brishMsg = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            sender = "BRISH",
                            text = cleanText,
                            actionPayload = actionPayload,
                            actionsExecuted = false,
                            timestamp = System.currentTimeMillis()
                        )
                        chatDao.insertMessage(brishMsg)

                        // Update session preview & timestamp
                        chatDao.insertSession(
                            sessionDoc.copy(
                                updatedAt = System.currentTimeMillis(),
                                previewText = cleanText.take(60),
                                messageCount = sessionDoc.messageCount + 1
                            )
                        )

                        // Background Long-Term Memory Extraction
                        if (shouldExtractMemory(text)) triggerMemoryExtraction(text, cleanText)
                    },
                    onFailure = { error ->
                        _chatError.value = ChatUiError(friendlyChatError(error), text)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Exception in background generation", e)
                _chatError.value = ChatUiError(friendlyChatError(e), text)
            } finally {
                _isGeneratingGlobal.value = false
            }
        }
    }

    private fun friendlyChatError(error: Throwable): String {
        val msg = error.message ?: ""
        return when {
            msg.contains("Inicia sesión", ignoreCase = true) -> "Inicia sesión para conversar con Brish AI."
            msg.contains("429") -> "Brish AI recibió muchas solicitudes. Espera un momento y vuelve a intentarlo."
            msg.contains("401") || msg.contains("403") -> "No pudimos validar tu sesión. Vuelve a iniciar sesión e inténtalo otra vez."
            msg.contains("502") -> "Problema temporal de conexión con el servidor de IA. Inténtalo de nuevo."
            msg.contains("503") -> "El servicio de IA está ocupado en este momento. Inténtalo en unos segundos."
            error is java.net.UnknownHostException || error is java.net.SocketTimeoutException || error is java.io.IOException -> "No pudimos conectar con Brish AI. Revisa tu conexión a internet e inténtalo nuevamente."
            else -> "Brish AI no pudo responder esta vez. Puedes intentarlo nuevamente."
        }
    }

    private fun shouldExtractMemory(text: String): Boolean {
        if (text.length < 18) return false
        val normalized = text.lowercase(Locale.getDefault())
        val personalSignals = listOf("prefiero", "me gusta", "estudio", "mi curso", "mi meta", "quiero lograr", "trabajo en", "recuérdame que")
        return personalSignals.any(normalized::contains)
    }

    private fun triggerMemoryExtraction(userText: String, aiReply: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val extractionPrompt = """
                    Analiza este intercambio entre el usuario y Brish AI.
                    Extrae HECHOS NUEVOS O PREFERENCIAS sobre la vida, trabajo, estudios, personas o metas del usuario (máximo 1 o 2 oraciones concisas).
                    Si el mensaje no contiene información personal útil (ej. solo saludo o agradecimiento), responde estrictamente "NINGUNO".
                    
                    Usuario: "$userText"
                    IA: "$aiReply"
                    
                    Responde solo con el hecho o "NINGUNO". Ejemplo de hecho: "Estudia ingeniería y tiene parcial de cálculo el viernes."
                """.trimIndent()

                val messages = listOf(OpenCodeMessage("user", extractionPrompt))
                val result = openCodeRepository.getChatCompletion(messages)
                val fact = result.getOrNull()?.trim()

                if (!fact.isNullOrBlank() && !fact.contains("NINGUNO", ignoreCase = true) && fact.length in 10..150) {
                    addKeyFact(fact)
                }
            } catch (e: Exception) {
                android.util.Log.w("ChatViewModel", "Memory extraction skipped", e)
            }
        }
    }

    private fun generateSmartTitle(text: String): String {
        val clean = text.trim().replace(Regex("[¡!¿?.,\n]"), " ").trim()
        val words = clean.split(" ").filter { it.isNotBlank() }
        return if (words.size <= 4) {
            clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        } else {
            words.take(4).joinToString(" ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } + "..."
        }
    }

    private fun extractActionPayload(reply: String): Pair<String, String?> {
        val actionIndex = reply.indexOf("AGENT_ACTION")
        if (actionIndex == -1) return Pair(reply, null)

        var startIndex = -1
        var braceCount = 0
        for (i in actionIndex downTo 0) {
            val c = reply[i]
            if (c == '}') braceCount++
            else if (c == '{') {
                if (braceCount == 0) {
                    startIndex = i
                    break
                } else {
                    braceCount--
                }
            }
        }

        if (startIndex == -1) return Pair(reply, null)

        braceCount = 0
        var inString = false
        var isEscaped = false
        var endIndex = -1

        for (i in startIndex until reply.length) {
            val c = reply[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (c == '\\') {
                isEscaped = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == '{') {
                    braceCount++
                } else if (c == '}') {
                    braceCount--
                    if (braceCount == 0) {
                        endIndex = i
                        break
                    }
                }
            }
        }

        if (endIndex != -1) {
            val jsonPayload = reply.substring(startIndex, endIndex + 1)
            var cleanReply = reply

            var blockStart = reply.lastIndexOf("```json", startIndex)
            if (blockStart == -1) {
                blockStart = reply.lastIndexOf("```", startIndex)
            }
            val blockEnd = reply.indexOf("```", endIndex + 1)

            if (blockStart != -1 && blockEnd != -1 && (startIndex - blockStart) < 20 && (blockEnd - endIndex) < 20) {
                cleanReply = reply.removeRange(blockStart, blockEnd + 3).trim()
            } else {
                cleanReply = reply.replace(jsonPayload, "").trim()
            }
            return Pair(cleanReply, jsonPayload)
        }

        return Pair(reply, null)
    }

    fun executeActions(messageId: String, payload: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (firebaseAuth.currentUser == null) return@launch

                val jsonObject = JsonParser.parseString(payload).asJsonObject
                val actions = jsonObject.getAsJsonArray("actions") ?: return@launch

                val allTasks = try { taskRepository.getAllTasks().first() } catch (e: Exception) { emptyList() }
                val allHabits = try { habitRepository.getHabits().first() } catch (e: Exception) { emptyList() }

                actions.take(10).forEach { actionElement ->
                    val action = actionElement.asJsonObject
                    val type = action.get("type")?.asString ?: ""

                    when (type) {
                        "CREATE_TASK" -> {
                            val title = action.get("title")?.asString?.trim()?.takeIf { it.isNotBlank() } ?: "Nueva tarea"
                            val category = action.get("category")?.asString ?: "General"
                            val offsetDays = (action.get("dueDateOffsetDays")?.asInt ?: 0).coerceIn(-365, 365)
                            val time = if (action.has("time") && !action.get("time").isJsonNull) action.get("time").asString else null
                            val repeatMode = if (action.has("repeatMode") && !action.get("repeatMode").isJsonNull) action.get("repeatMode").asString else "NONE"
                            val repeatDays = try {
                                if (action.has("repeatDays") && action.get("repeatDays").isJsonArray) {
                                    action.getAsJsonArray("repeatDays").map { it.asInt }
                                } else emptyList()
                            } catch (e: Exception) { emptyList<Int>() }

                            val priority = if (action.has("priority") && !action.get("priority").isJsonNull) action.get("priority").asString else "MEDIUM"
                            val description = if (action.has("description") && !action.get("description").isJsonNull) action.get("description").asString else ""

                            val cal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, offsetDays)
                                if (!time.isNullOrBlank() && time.contains(":")) {
                                    val parts = time.split(":")
                                    if (parts.size == 2) {
                                        set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 9)
                                        set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                }
                            }

                            val task = Task(
                                title = title,
                                category = category,
                                isCompleted = false,
                                timestamp = cal.timeInMillis,
                                dueDate = cal.timeInMillis,
                                time = time,
                                repeatMode = repeatMode,
                                repeatDays = repeatDays,
                                priority = priority,
                                description = description
                            )
                            taskRepository.addTask(task)
                        }
                        "UPDATE_TASK" -> {
                            val taskId = action.get("id")?.asString ?: ""
                            val existingTask = allTasks.find { it.id == taskId }

                            if (existingTask != null) {
                                val title = if (action.has("title") && !action.get("title").isJsonNull) action.get("title").asString else existingTask.title
                                val time = if (action.has("time")) {
                                    val t = action.get("time")
                                    if (t.isJsonNull) null else t.asString
                                } else existingTask.time
                                val isCompleted = if (action.has("isCompleted")) action.get("isCompleted").asBoolean else existingTask.isCompleted
                                val priority = if (action.has("priority") && !action.get("priority").isJsonNull) {
                                    action.get("priority").asString
                                } else existingTask.priority

                                taskRepository.updateTask(
                                    existingTask.copy(
                                        title = title,
                                        time = time,
                                        isCompleted = isCompleted,
                                        priority = priority
                                    )
                                )
                            }
                        }
                        "DELETE_TASK" -> {
                            val taskId = action.get("id")?.asString ?: ""
                            if (taskId.isNotEmpty()) {
                                taskRepository.deleteTask(taskId)
                            }
                        }
                        "CREATE_HABIT" -> {
                            val title = action.get("title")?.asString ?: "Nuevo Hábito"
                            val icon = action.get("icon")?.asString ?: "spa"
                            val reminderTime = if (action.has("reminderTime") && !action.get("reminderTime").isJsonNull) action.get("reminderTime").asString else null
                            val daysOfWeek = try {
                                if (action.has("daysOfWeek") && action.get("daysOfWeek").isJsonArray) {
                                    action.getAsJsonArray("daysOfWeek").map { it.asInt }
                                } else listOf(1, 2, 3, 4, 5, 6, 7)
                            } catch (e: Exception) { listOf(1, 2, 3, 4, 5, 6, 7) }

                            val habit = Habit(
                                title = title,
                                icon = icon,
                                reminderTime = reminderTime,
                                daysOfWeek = daysOfWeek,
                                currentStreak = 0,
                                completedDates = emptyList()
                            )
                            habitRepository.addHabit(habit)
                        }
                        "UPDATE_HABIT" -> {
                            val habitId = action.get("id")?.asString ?: ""
                            val existingHabit = allHabits.find { it.id == habitId }
                            if (existingHabit != null) {
                                val title = if (action.has("title") && !action.get("title").isJsonNull) {
                                    action.get("title").asString
                                } else existingHabit.title
                                val reminderTime = if (action.has("reminderTime")) {
                                    val t = action.get("reminderTime")
                                    if (t.isJsonNull) null else t.asString
                                } else existingHabit.reminderTime
                                habitRepository.updateHabit(
                                    existingHabit.copy(title = title, reminderTime = reminderTime)
                                )
                            }
                        }
                        "DELETE_HABIT" -> {
                            val habitId = action.get("id")?.asString ?: ""
                            if (habitId.isNotEmpty()) {
                                habitRepository.deleteHabit(habitId)
                            }
                        }
                        "CREATE_NOTE" -> {
                            val title = action.get("title")?.asString ?: "Nota de Brish AI"
                            val content = action.get("content")?.asString ?: ""
                            val colorHex = action.get("colorHex")?.asString ?: "#FFF9C4"
                            val categoryName = action.get("categoryName")?.asString
                            var categoryId: String? = null
                            if (!categoryName.isNullOrBlank()) {
                                val categories = try { noteRepository.getCategories().first() } catch (e: Exception) { emptyList() }
                                val existing = categories.find { it.name.equals(categoryName, ignoreCase = true) }
                                if (existing != null) {
                                    categoryId = existing.id
                                } else {
                                    val created = NoteCategory(name = categoryName)
                                    noteRepository.addCategory(created)
                                    categoryId = created.id
                                }
                            }
                            noteRepository.addNote(
                                Note(
                                    title = title,
                                    content = content,
                                    colorHex = colorHex,
                                    priority = "MEDIUM",
                                    timestamp = System.currentTimeMillis(),
                                    categoryId = categoryId
                                )
                            )
                        }
                        "DELETE_NOTE" -> {
                            val noteId = action.get("id")?.asString ?: ""
                            if (noteId.isNotEmpty()) {
                                noteRepository.deleteNote(noteId)
                            }
                        }
                        "SET_POMODORO" -> {
                            val focusMinutes = action.get("focusMinutes")?.asInt ?: 25
                            val breakMinutes = action.get("breakMinutes")?.asInt ?: 5
                            appPreferencesRepository.setWorkDuration(focusMinutes.toLong())
                            appPreferencesRepository.setBreakDuration(breakMinutes.toLong())
                        }
                        "CREATE_TRANSACTION" -> {
                            val title = action.get("title")?.asString ?: "Gasto"
                            val amount = kotlin.math.abs(action.get("amount")?.asDouble ?: 0.0)
                            val transactionType = action.get("transactionType")?.asString ?: "EXPENSE"
                            val category = action.get("category")?.asString ?: "General"

                            val transaction = com.katchy.focuslive.data.model.Transaction(
                                title = title,
                                amount = amount,
                                type = transactionType,
                                category = category,
                                timestamp = System.currentTimeMillis()
                            )
                            if (amount > 0.0) financeRepository.addTransaction(transaction)
                        }
                    }
                }

                chatDao.updateActionsExecuted(messageId, true)

            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error executing agent actions", e)
            }
        }
    }

    fun dismissActions(messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updateActionsDismissed(messageId, true)
        }
    }

    fun exportToNotes(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val title = "Apunte de Brish AI (${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(System.currentTimeMillis())})"
            val note = Note(
                title = title,
                content = text,
                timestamp = System.currentTimeMillis()
            )
            noteRepository.addNote(note)
        }
    }

    fun exportToTasks(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            extractTaskTitlesFromAiText(text).forEach { title ->
                val now = System.currentTimeMillis()
                val task = Task(
                    title = title,
                    category = "Brish AI",
                    isCompleted = false,
                    timestamp = now,
                    dueDate = now
                )
                taskRepository.addTask(task)
            }
        }
    }
}
