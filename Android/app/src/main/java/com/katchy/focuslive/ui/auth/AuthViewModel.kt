package com.katchy.focuslive.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.katchy.focuslive.data.repository.AuthRepository
import com.katchy.focuslive.data.repository.TaskRepository
import com.katchy.focuslive.data.repository.NoteRepository
import com.katchy.focuslive.data.repository.HabitRepository
import com.katchy.focuslive.data.repository.FinanceRepository
import com.katchy.focuslive.data.repository.UserStatsRepository
import com.katchy.focuslive.data.repository.MascotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val habitRepository: HabitRepository,
    private val financeRepository: FinanceRepository,
    private val userStatsRepository: UserStatsRepository,
    private val mascotRepository: MascotRepository
) : ViewModel() {

    private val _user = MutableStateFlow<FirebaseUser?>(authRepository.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Track previous user to detect account switches
    private var previousUserId: String? = authRepository.currentUser?.uid

    init {
        viewModelScope.launch {
            authRepository.authStateFlow.collect { firebaseUser ->
                val newUserId = firebaseUser?.uid

                // Detect account switch (different user logged in)
                if (previousUserId != null && newUserId != null && previousUserId != newUserId) {
                    android.util.Log.d("AuthViewModel", "Account switch detected: $previousUserId -> $newUserId")
                    handleAccountSwitch(previousUserId, newUserId)
                }

                previousUserId = newUserId
                _user.value = firebaseUser
            }
        }
    }

    fun signIn(credential: AuthCredential) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val result = authRepository.linkAnonymousWithCredential(credential)
            result.onSuccess { firebaseUser ->
                saveUserToFirestore(firebaseUser)
            }.onFailure { exception ->
                _loading.value = false
                _error.value = exception.message
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val result = authRepository.signInAnonymously()
            result.onSuccess { firebaseUser ->
                 saveUserToFirestore(firebaseUser)
            }.onFailure { exception ->
                _loading.value = false
                _error.value = "Guest login failed: ${exception.message}"
            }
        }
    }

    private suspend fun handleAccountSwitch(previousUserId: String?, newUserId: String) {
        try {
            android.util.Log.d("AuthViewModel", "Handling account switch to user: $newUserId")

            // Switch user data - this will sync old data, clear local DB, and download new data.
            // Pasamos previousUserId explícitamente porque FirebaseAuth ya cambió a newUserId
            // al dispararse este callback; si lo leyéramos de currentUser aquí devolvería newUserId.
            authRepository.switchUser(
                previousUserId,
                newUserId,
                taskRepository,
                noteRepository,
                habitRepository,
                financeRepository
            )

            android.util.Log.d("AuthViewModel", "Account switch completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Account switch failed", e)
            _error.value = "Error switching account: ${e.message}"
        }
    }

    private fun saveUserToFirestore(user: FirebaseUser) {
        viewModelScope.launch {
            val result = authRepository.saveUserToFirestore(user)
            result.onSuccess {
                // After saving user, download their data from cloud
                val newUserId = user.uid

                // If this is a new login (not just an account switch), download data
                if (previousUserId == null || previousUserId == newUserId) {
                    android.util.Log.d("AuthViewModel", "Downloading data for user: $newUserId")
                    taskRepository.refreshTasks()
                    noteRepository.refreshNotes()
                    habitRepository.refreshHabits()
                    financeRepository.refreshTransactions()
                    userStatsRepository.loadStatsFromFirebase()
                    mascotRepository.loadMascotFromFirebase()
                }

                _loading.value = false
            }.onFailure { exception ->
                _error.value = "Error saving user data: ${exception.message}"
                _loading.value = false
            }
        }
    }
}
