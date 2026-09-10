package com.katchy.focuslive.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.tasks.await
import com.katchy.focuslive.data.local.BrishDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.work.WorkManager
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    val context: android.content.Context
    val currentUser: FirebaseUser?
    val authStateFlow: Flow<FirebaseUser?>
    suspend fun signInWithCredential(credential: AuthCredential): Result<FirebaseUser>
    suspend fun linkAnonymousWithCredential(credential: AuthCredential): Result<FirebaseUser>
    suspend fun signInAnonymously(): Result<FirebaseUser>
    suspend fun saveUserToFirestore(user: FirebaseUser): Result<Unit>
    suspend fun updateDisplayName(newName: String): Result<Unit>
    suspend fun logout(
        taskRepository: TaskRepository,
        noteRepository: NoteRepository,
        habitRepository: HabitRepository,
        financeRepository: FinanceRepository
    ): Result<Unit>
suspend fun switchUser(
        previousUserId: String?,
        newUserId: String,
        taskRepository: TaskRepository,
        noteRepository: NoteRepository,
        habitRepository: HabitRepository,
        financeRepository: FinanceRepository
    ): Result<Unit>
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val database: BrishDatabase,
    private val userStatsRepository: UserStatsRepository,
    private val mascotRepository: MascotRepository,
    override val context: android.content.Context
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    override val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithCredential(credential: AuthCredential): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User is null"))
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("auth_provider", credential.provider)
                setCustomKey("auth_stage", "firebase_auth")
                setCustomKey("package_name", context.packageName)
                log("FirebaseAuth signInWithCredential failed")
                recordException(e)
            }
            Result.failure(e)
        }
    }

    override suspend fun linkAnonymousWithCredential(credential: AuthCredential): Result<FirebaseUser> {
        val current = firebaseAuth.currentUser
        if (current == null || !current.isAnonymous) {
            return signInWithCredential(credential)
        }
        return try {
            val linked = current.linkWithCredential(credential).await().user
            if (linked != null) Result.success(linked)
            else Result.failure(Exception("User is null"))
        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            signInWithCredential(credential)
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Result.failure(e)
        }
    }

    override suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInAnonymously().await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUserToFirestore(user: FirebaseUser): Result<Unit> {
        return try {
            val userMap = hashMapOf(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to user.displayName,
                "photoUrl" to user.photoUrl?.toString(),
                "lastLogin" to System.currentTimeMillis()
            )
            firestore.collection("users").document(user.uid)
                .set(userMap, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDisplayName(newName: String): Result<Unit> {
        val user = firebaseAuth.currentUser ?: return Result.failure(Exception("No user logged in"))
        return try {
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()
            user.updateProfile(profileUpdates).await()
            saveUserToFirestore(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(
        taskRepository: TaskRepository,
        noteRepository: NoteRepository,
        habitRepository: HabitRepository,
        financeRepository: FinanceRepository
    ): Result<Unit> {
        val userId = currentUser?.uid
        val errors = mutableListOf<Exception>()

        return try {
            // 0. Cancel any pending sync work FIRST so it doesn't run with stale auth
            //    or mix data between users.
            withContext(Dispatchers.IO) {
                WorkManager.getInstance(context).cancelUniqueWork("SyncWork")
            }

            // 1. Sync pending changes before logout (CRITICAL). Stats y mascotas también
            //    deben sincronizarse *antes* de clearUserData, que es destructivo.
            //    Usamos supervisorScope para que el fallo de un repositorio no cancele los demás.
            if (userId != null) {
                android.util.Log.d("AuthRepository", "Syncing pending changes before logout for user: $userId")
                supervisorScope {
                    val syncs = listOf(
                        launch { taskRepository.syncPendingChanges(userId).onFailure { errors += it as Exception } },
                        launch { noteRepository.syncPendingChanges(userId).onFailure { errors += it as Exception } },
                        launch { habitRepository.syncPendingChanges(userId).onFailure { errors += it as Exception } },
                        launch { financeRepository.syncPendingChanges(userId).onFailure { errors += it as Exception } },
                        launch { userStatsRepository.syncStatsToFirebaseAwait(userId).let { } },
                        launch { mascotRepository.syncMascotToFirebaseAwait(userId).let { } }
                    )
                    syncs.forEach { it.join() }
                }
            }

            // 2. Sign out from Firebase
            firebaseAuth.signOut()

            // 3. Clear local data for the user (now safe since we synced)
            withContext(Dispatchers.IO) {
                if (userId != null) {
                    taskRepository.clearUserData(userId)
                    noteRepository.clearUserData(userId)
                    habitRepository.clearUserData(userId)
                    financeRepository.clearUserData(userId)
                }
                userStatsRepository.clearUserData()
                mascotRepository.clearUserData()
                database.chatDao().clearAll()
            }

            android.util.Log.d("AuthRepository", "Logout completed successfully")
            if (errors.isNotEmpty()) {
                Result.failure(Exception("Some syncs failed during logout: ${errors.map { it.message }}"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Logout failed", e)
            // Even if sync fails, we still sign out to prevent user being stuck
            firebaseAuth.signOut()
            Result.failure(e)
        }
    }

    override suspend fun switchUser(
        previousUserId: String?,
        newUserId: String,
        taskRepository: TaskRepository,
        noteRepository: NoteRepository,
        habitRepository: HabitRepository,
        financeRepository: FinanceRepository
    ): Result<Unit> {
        val errors = mutableListOf<Exception>()
        return try {
            android.util.Log.d("AuthRepository", "Switching user from $previousUserId to $newUserId")

            // 0. Cancel any pending sync work from the previous user to avoid it running
            //    with the new auth state or mixing data between accounts.
            withContext(Dispatchers.IO) {
                WorkManager.getInstance(context).cancelUniqueWork("SyncWork")
            }

            // 1. Sync pending changes from previous user (usando previousUserId explícito;
            //    FirebaseAuth YA cambió a newUserId al dispararse el callback, así que
            //    currentUser?.uid aquí devolvería newUserId y perderíamos el sync del anterior).
            if (previousUserId != null && previousUserId != newUserId) {
                android.util.Log.d("AuthRepository", "Syncing data from previous user: $previousUserId")
                supervisorScope {
                    val syncs = listOf(
                        launch { taskRepository.syncPendingChanges(previousUserId).onFailure { errors += it as Exception } },
                        launch { noteRepository.syncPendingChanges(previousUserId).onFailure { errors += it as Exception } },
                        launch { habitRepository.syncPendingChanges(previousUserId).onFailure { errors += it as Exception } },
                        launch { financeRepository.syncPendingChanges(previousUserId).onFailure { errors += it as Exception } },
                        launch { userStatsRepository.syncStatsToFirebaseAwait(previousUserId).let { } },
                        launch { mascotRepository.syncMascotToFirebaseAwait(previousUserId).let { } }
                    )
                    syncs.forEach { it.join() }
                }

                // 2. Clear previous user's local data
                withContext(Dispatchers.IO) {
                    taskRepository.clearUserData(previousUserId)
                    noteRepository.clearUserData(previousUserId)
                    habitRepository.clearUserData(previousUserId)
                    financeRepository.clearUserData(previousUserId)
                    userStatsRepository.clearUserData()
                    mascotRepository.clearUserData()
                    database.chatDao().clearAll()
                }
            }

            // 3. Download new user's data from cloud.
            //    Se pasa newUserId explícito para evitar leer auth.currentUser en los repos.
            android.util.Log.d("AuthRepository", "Downloading data for new user: $newUserId")
            supervisorScope {
                val downloads = listOf(
                    launch { taskRepository.refreshTasks(newUserId).onFailure { errors += it as Exception } },
                    launch { noteRepository.refreshNotes(newUserId).onFailure { errors += it as Exception } },
                    launch { habitRepository.refreshHabits(newUserId).onFailure { errors += it as Exception } },
                    launch { financeRepository.refreshTransactions(newUserId).onFailure { errors += it as Exception } },
                    launch { userStatsRepository.loadStatsFromFirebase().onFailure { errors += it as Exception } },
                    launch { mascotRepository.loadMascotFromFirebase().onFailure { errors += it as Exception } }
                )
                downloads.forEach { it.join() }
            }

            android.util.Log.d("AuthRepository", "User switch completed successfully")
            if (errors.isNotEmpty()) {
                Result.failure(Exception("Some sync/download steps failed during switch: ${errors.map { it.message }}"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "User switch failed", e)
            Result.failure(e)
        }
    }
}
