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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.tasks.await
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

    private val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()

    private val _user = MutableStateFlow<FirebaseUser?>(authRepository.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Track previous user to detect account switches
    private var previousUserId: String? = authRepository.currentUser?.uid
    private var previousUserWasGuest: Boolean = authRepository.currentUser?.isAnonymous == true
    private var accountSwitchJob: Job? = null
    private var lastQueuedUserId: String? = null

    init {
        viewModelScope.launch {
            authRepository.authStateFlow.collect { firebaseUser ->
                val newUserId = firebaseUser?.uid
                val oldUserId = previousUserId
                val oldUserWasGuest = previousUserWasGuest
                // Publish identity before any account cleanup or network work can suspend.
                previousUserId = newUserId
                previousUserWasGuest = firebaseUser?.isAnonymous == true
                _user.value = firebaseUser
                if (oldUserId != newUserId) {
                    accountSwitchJob?.cancel()
                    lastQueuedUserId = null
                }
                if (newUserId != null) {
                    enqueueRefresh(newUserId)
                    if (oldUserId != null && oldUserId != newUserId) {
                        accountSwitchJob = viewModelScope.launch {
                            handleAccountSwitch(oldUserId, newUserId, adoptGuestData = oldUserWasGuest)
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
            // Sin red Firebase puede tardar mucho: no dejamos la pantalla cargando para siempre.
            val result = withTimeoutOrNull(30_000L) { authRepository.linkAnonymousWithCredential(credential) }
                ?: Result.failure(Exception("Google tardó demasiado en responder. Revisa tu conexión e inténtalo otra vez."))
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

    fun signInWithEmail(email: String, password: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.isBlank()) {
            _error.value = "Escribe tu correo y contraseña."
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val result = withTimeoutOrNull(30_000L) {
                runCatching { firebaseAuth.signInWithEmailAndPassword(cleanEmail, password).await().user ?: error("No se recibió la cuenta.") }
            } ?: Result.failure(Exception("El inicio de sesión tardó demasiado. Revisa tu conexión."))
            result.onSuccess(::saveUserToFirestore).onFailure {
                _loading.value = false
                _error.value = emailAuthMessage(it)
            }
        }
    }

    fun registerWithEmail(email: String, password: String) {
        val cleanEmail = email.trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _error.value = "Escribe un correo válido."
            return
        }
        if (password.length < 6) {
            _error.value = "La contraseña debe tener al menos 6 caracteres."
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val result = withTimeoutOrNull(30_000L) {
                if (firebaseAuth.currentUser?.isAnonymous == true) {
                    // Registrarse desde el modo invitado vincula la cuenta y conserva el mismo id y todos los datos.
                    authRepository.linkAnonymousWithCredential(
                        com.google.firebase.auth.EmailAuthProvider.getCredential(cleanEmail, password)
                    )
                } else {
                    runCatching { firebaseAuth.createUserWithEmailAndPassword(cleanEmail, password).await().user ?: error("No se recibió la cuenta.") }
                }
            } ?: Result.failure(Exception("El registro tardó demasiado. Revisa tu conexión."))
            result.onSuccess(::saveUserToFirestore).onFailure {
                _loading.value = false
                _error.value = emailAuthMessage(it)
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val cleanEmail = email.trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _error.value = "Escribe tu correo para enviarte el enlace."
            return
        }
        viewModelScope.launch {
            runCatching { firebaseAuth.sendPasswordResetEmail(cleanEmail).await() }
                .onSuccess { _error.value = "Si existe una cuenta con ese correo, te llegará un enlace para cambiar la contraseña." }
                .onFailure { _error.value = emailAuthMessage(it) }
        }
    }

    private fun emailAuthMessage(error: Throwable): String = when ((error as? com.google.firebase.auth.FirebaseAuthException)?.errorCode) {
        "ERROR_INVALID_EMAIL" -> "El correo no tiene un formato válido."
        "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "El correo o la contraseña no coinciden."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Ya existe una cuenta con ese correo. Inicia sesión."
        "ERROR_WEAK_PASSWORD" -> "La contraseña debe tener al menos 6 caracteres."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Sin conexión. Revisa tu internet e inténtalo otra vez."
        else -> "No se pudo continuar. Revisa tus datos e inténtalo otra vez."
    }

    private suspend fun handleAccountSwitch(previousUserId: String?, newUserId: String, adoptGuestData: Boolean = false) {
        if (authRepository.currentUser?.uid != newUserId) return
        try {
            // Del modo invitado a una cuenta: lo creado como invitado pasa a la cuenta nueva antes de descargarla.
            if (adoptGuestData && previousUserId != null) {
                authRepository.adoptGuestRecords(previousUserId, newUserId).getOrThrow()
            }
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
        // Al vincular un invitado el id no cambia y Firebase no avisa: se publica el usuario ya no anónimo.
        _user.value = authRepository.currentUser
        previousUserWasGuest = authRepository.currentUser?.isAnonymous == true
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
