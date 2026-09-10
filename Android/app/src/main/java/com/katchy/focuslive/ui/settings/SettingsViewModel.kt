package com.katchy.focuslive.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katchy.focuslive.data.model.AppTheme
import com.katchy.focuslive.data.repository.AuthRepository
import com.katchy.focuslive.data.repository.TaskRepository
import com.katchy.focuslive.data.repository.NoteRepository
import com.katchy.focuslive.data.repository.HabitRepository
import com.katchy.focuslive.data.repository.FinanceRepository
import com.katchy.focuslive.data.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
    private val authRepository: AuthRepository,
    private val appPreferencesRepository: com.katchy.focuslive.data.repository.AppPreferencesRepository,
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val habitRepository: HabitRepository,
    private val financeRepository: FinanceRepository,
    private val userStatsRepository: com.katchy.focuslive.data.repository.UserStatsRepository,
    private val openCodeCredentialsStore: com.katchy.focuslive.data.security.OpenCodeCredentialsStore
) : ViewModel() {

    private val _hasOpenCodeKey = MutableStateFlow(openCodeCredentialsStore.hasKey())
    val hasOpenCodeKey: StateFlow<Boolean> = _hasOpenCodeKey.asStateFlow()
    
    val isPremium: StateFlow<Boolean> = userStatsRepository.isPremium
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)

    val isPlannerEnabled = appPreferencesRepository.isPlannerEnabled
    val isNotesEnabled = appPreferencesRepository.isNotesEnabled
    val isFinanceEnabled = appPreferencesRepository.isFinanceEnabled
    val isHabitsEnabled = appPreferencesRepository.isHabitsEnabled
    
    val accentColor = appPreferencesRepository.accentColor
    val isPomodoroSoundEnabled = appPreferencesRepository.isPomodoroSoundEnabled
    
    val workDuration = appPreferencesRepository.workDuration
    val breakDuration = appPreferencesRepository.breakDuration


    fun setPlannerEnabled(enabled: Boolean) = appPreferencesRepository.setPlannerEnabled(enabled)
    fun setNotesEnabled(enabled: Boolean) = appPreferencesRepository.setNotesEnabled(enabled)
    fun setFinanceEnabled(enabled: Boolean) = appPreferencesRepository.setFinanceEnabled(enabled)
    fun setHabitsEnabled(enabled: Boolean) = appPreferencesRepository.setHabitsEnabled(enabled)
    
    fun setAccentColor(color: Int) = appPreferencesRepository.setAccentColor(color)
    fun setPomodoroSoundEnabled(enabled: Boolean) = appPreferencesRepository.setPomodoroSoundEnabled(enabled)
    
    fun setWorkDuration(minutes: Long) = appPreferencesRepository.setWorkDuration(minutes)
    fun setBreakDuration(minutes: Long) = appPreferencesRepository.setBreakDuration(minutes)

    fun saveOpenCodeKey(apiKey: String): Boolean = runCatching {
        openCodeCredentialsStore.save(apiKey)
        _hasOpenCodeKey.value = true
    }.isSuccess

    fun clearOpenCodeKey() {
        openCodeCredentialsStore.clear()
        _hasOpenCodeKey.value = false
    }
    
    fun updateDisplayName(newName: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.updateDisplayName(newName)
            onComplete(result.isSuccess)
        }
    }
    
    val currentLanguage = appPreferencesRepository.currentLanguage
    fun setLanguage(languageCode: String) = appPreferencesRepository.setLanguage(languageCode)


    val currentTheme: StateFlow<AppTheme> = themeRepository.currentTheme
    val currentUser = authRepository.authStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setTheme(theme: AppTheme) {
        themeRepository.setTheme(theme)
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.logout(
                taskRepository,
                noteRepository,
                habitRepository,
                financeRepository
            )
            onComplete()
        }
    }
}
