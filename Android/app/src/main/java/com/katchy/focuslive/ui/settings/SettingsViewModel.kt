package com.katchy.focuslive.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.GsonBuilder
import com.katchy.focuslive.data.local.BrishDatabase
import com.katchy.focuslive.data.model.AppTheme
import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.data.model.Transaction
import com.katchy.focuslive.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
    private val authRepository: AuthRepository,
    private val preferences: AppPreferencesRepository,
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val habitRepository: HabitRepository,
    private val financeRepository: FinanceRepository,
    subscriptionRepository: SubscriptionRepository,
    private val database: BrishDatabase,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val isPremium = subscriptionRepository.isPremium
    val currentTheme = themeRepository.currentTheme
    val currentUser = authRepository.authStateFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val isPomodoroSoundEnabled = preferences.isPomodoroSoundEnabled
    val workDuration = preferences.workDuration
    val breakDuration = preferences.breakDuration
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    fun dismissMessage() { _message.value = null }
    fun setTheme(theme: AppTheme) = themeRepository.setTheme(theme)
    /** Vuelve a mostrar la guía de primeros pasos la próxima vez que se abra Hoy. */
    fun showGuideAgain() { preferences.setGuideShown(false); _message.value = "La guía aparecerá al volver a Hoy." }
    fun setPomodoroSoundEnabled(enabled: Boolean) = preferences.setPomodoroSoundEnabled(enabled)
    fun setWorkDuration(minutes: Long) = preferences.setWorkDuration(minutes.coerceIn(5, 120))
    fun setBreakDuration(minutes: Long) = preferences.setBreakDuration(minutes.coerceIn(1, 30))
    fun updateDisplayName(name: String, onComplete: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val result = authRepository.updateDisplayName(name.trim())
        result.onFailure { _message.value = "No se pudo actualizar el nombre" }
        onComplete(result.isSuccess)
    }
    fun logout(onComplete: () -> Unit = {}) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            authRepository.logout(taskRepository, noteRepository, habitRepository, financeRepository)
            _busy.value = false
            onComplete()
        }
    }

    /** Export is read-only for legacy data, and never deletes records from Firebase. */
    fun exportLegacy(uri: Uri) {
        if (_busy.value) return
        val owner = auth.currentUser?.uid ?: return
        _busy.value = true
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val localHabits = database.habitDao().getAllHabits(owner).first()
                    val localFinance = database.financeDao().getAllTransactions(owner).first()
                    val remote = withTimeoutOrNull(5000) {
                        runCatching {
                            val user = firestore.collection("users").document(owner)
                            val habits = user.collection("habits").get().await().toObjects(Habit::class.java)
                            val finance = user.collection("finance").get().await().toObjects(Transaction::class.java)
                            habits to finance
                        }.getOrNull()
                    }
                    check(auth.currentUser?.uid == owner) { "La cuenta cambió; exportación cancelada" }
                    val deletedHabits = database.pendingDeleteDao().getPendingDeletes(owner, "habit").map { it.itemId }.toSet()
                    val deletedFinance = database.pendingDeleteDao().getPendingDeletes(owner, "transaction").map { it.itemId }.toSet()
                    val habits = (remote?.first.orEmpty() + localHabits).associateBy { it.id }.values.filter { it.id !in deletedHabits }
                    val finance = (remote?.second.orEmpty() + localFinance).associateBy { it.id }.values.filter { it.id !in deletedFinance }
                    val payload = mapOf("formatVersion" to 1, "exportedAt" to System.currentTimeMillis(),
                        "userId" to owner, "includesCloud" to (remote != null), "habits" to habits, "finance" to finance)
                    context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                        it.write(GsonBuilder().setPrettyPrinting().create().toJson(payload))
                    } ?: error("No se pudo abrir el archivo")
                    if (remote == null) "Exportación guardada con datos locales. La nube no estuvo disponible." else "Exportación guardada"
                }
            }.onSuccess { _message.value = it }.onFailure { _message.value = it.message ?: "No se pudo exportar" }
            _busy.value = false
        }
    }
}
