package com.katchy.focuslive.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.katchy.focuslive.data.repository.AuthRepository
import com.katchy.focuslive.data.repository.TaskRepository
import com.katchy.focuslive.data.repository.NoteRepository
import com.katchy.focuslive.data.repository.HabitRepository
import com.katchy.focuslive.data.repository.FinanceRepository
import com.katchy.focuslive.data.repository.SubscriptionRepository
import com.katchy.focuslive.data.worker.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private val subscriptionRepository: SubscriptionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _user = MutableStateFlow<FirebaseUser?>(authRepository.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Track previous user to detect account switches
    private var previousUserId: String? = authRepository.currentUser?.uid
    private var accountSwitchJob: Job? = null
    private var lastQueuedUserId: String? = null

    init {
        viewModelScope.launch {
            authRepository.authStateFlow.collect { firebaseUser ->
                val newUserId = firebaseUser?.uid
                val oldUserId = previousUserId
                // Publish identity before any account cleanup or network work can suspend.
                previousUserId = newUserId
                _user.value = firebaseUser
                if (oldUserId != newUserId) {
                    accountSwitchJob?.cancel()
                    lastQueuedUserId = null
                }
                if (newUserId != null) {
                    enqueueRefresh(newUserId)
                    if (oldUserId != null && oldUserId != newUserId) {
                        accountSwitchJob = viewModelScope.launch {
                            handleAccountSwitch(oldUserId, newUserId)
                        }
                    }
                }
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
        if (authRepository.currentUser?.uid != newUserId) return
        try {
            android.util.Log.d("AuthViewModel", "Handling account switch to user: $newUserId")

            // Keep previous-owner records and pending writes; reset account presentation state.
            // Pass the captured old owner because FirebaseAuth already exposes the new account.
            authRepository.switchUser(
                previousUserId,
                newUserId,
                taskRepository,
                noteRepository,
                habitRepository,
                financeRepository
            ).getOrThrow()

            android.util.Log.d("AuthViewModel", "Account switch completed successfully")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Account switch failed", e)
            if (authRepository.currentUser?.uid == newUserId) {
                _error.value = "Error switching account: ${e.message}"
            }
        }
    }

    private fun enqueueRefresh(owner: String) {
        if (authRepository.currentUser?.uid != owner || lastQueuedUserId == owner) return
        SyncScheduler.enqueue(context, owner, forceRefresh = true)
        lastQueuedUserId = owner
    }

    private fun saveUserToFirestore(user: FirebaseUser) {
        val owner = user.uid
        if (authRepository.currentUser?.uid != owner) return
        // Authentication succeeded; offline profile/sync work does not keep the login UI busy.
        _loading.value = false
        enqueueRefresh(owner)
        viewModelScope.launch {
            val result = authRepository.saveUserToFirestore(user)
            if (authRepository.currentUser?.uid != owner) return@launch
            result.onFailure { exception ->
                _error.value = "Error saving user data: ${exception.message}"
            }
        }
        viewModelScope.launch {
            if (authRepository.currentUser?.uid == owner) subscriptionRepository.loadFromFirebase()
        }
    }
}
