package com.katchy.focuslive.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.katchy.focuslive.data.model.MascotType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MascotRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userStatsRepository: UserStatsRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val prefs = context.getSharedPreferences("mascot_prefs", Context.MODE_PRIVATE)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val gson = com.google.gson.Gson()

    // Load saved mascot or default to POPPIN
    private val savedMascotId = prefs.getString(KEY_SELECTED_MASCOT, MascotType.POPPIN.id) ?: MascotType.POPPIN.id
    private val initialMascot = MascotType.entries.find { it.id == savedMascotId } ?: MascotType.POPPIN
    private val initialUnlocked: Set<String> =
        prefs.getStringSet(KEY_UNLOCKED_MASCOTS, null) ?: setOf(MascotType.POPPIN.id, MascotType.LLAMA.id)

    private val _selectedMascot = MutableStateFlow(initialMascot)
    val selectedMascot: StateFlow<MascotType> = _selectedMascot.asStateFlow()

    // Mascotas desbloqueadas con coins (LO QUE SE PERSISTE EN FIRESTORE).
    // Premium desbloquea todas temporalmente vía el flow derivado de abajo.
    private val _purchasedMascotIds = MutableStateFlow<Set<String>>(initialUnlocked)

    // Flow derivado: premium → todas las mascotas; free → solo las compradas.
    val unlockedMascotIds: StateFlow<Set<String>> =
        kotlinx.coroutines.flow.combine(
            userStatsRepository.isPremium,
            _purchasedMascotIds
        ) { isPremium, purchased ->
            if (isPremium) MascotType.entries.map { it.id }.toSet() else purchased
        }.stateIn(repositoryScope, SharingStarted.WhileSubscribed(5000), initialUnlocked)

    // Cargar mapa de XP guardado localmente o vacío
    private fun loadMascotXpFromPrefs(): Map<String, Int> {
        val json = prefs.getString(KEY_MASCOT_XP_MAP, null) ?: return emptyMap()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private val _mascotXpMap = MutableStateFlow<Map<String, Int>>(loadMascotXpFromPrefs())
    val mascotXpMap: StateFlow<Map<String, Int>> = _mascotXpMap.asStateFlow()

    // Flow derivado para obtener XP de la mascota seleccionada
    val selectedMascotXp: StateFlow<Int> = kotlinx.coroutines.flow.combine(
        selectedMascot,
        _mascotXpMap
    ) { selected, xpMap ->
        xpMap[selected.id] ?: 0
    }.stateIn(repositoryScope, SharingStarted.WhileSubscribed(5000), 0)

    fun getMascotLevel(xp: Int): Int {
        return 1 + (xp / 100) // Curva de 100 XP por nivel
    }

    fun addXpToSelectedMascot(amount: Int) {
        val currentMascotId = _selectedMascot.value.id
        synchronized(prefs) {
            val currentMap = loadMascotXpFromPrefs().toMutableMap()
            val currentXp = currentMap[currentMascotId] ?: 0
            val newXp = currentXp + amount
            currentMap[currentMascotId] = newXp
            
            prefs.edit().putString(KEY_MASCOT_XP_MAP, gson.toJson(currentMap)).commit()
            _mascotXpMap.value = currentMap
        }
        syncMascotToFirebase()
    }

    fun deductXpFromSelectedMascot(amount: Int) {
        val currentMascotId = _selectedMascot.value.id
        synchronized(prefs) {
            val currentMap = loadMascotXpFromPrefs().toMutableMap()
            val currentXp = currentMap[currentMascotId] ?: 0
            val newXp = maxOf(0, currentXp - amount)
            currentMap[currentMascotId] = newXp
            
            prefs.edit().putString(KEY_MASCOT_XP_MAP, gson.toJson(currentMap)).commit()
            _mascotXpMap.value = currentMap
        }
        syncMascotToFirebase()
    }

    fun clearUserData() {
        prefs.edit().clear().commit()
        _selectedMascot.value = MascotType.POPPIN
        _purchasedMascotIds.value = setOf(MascotType.POPPIN.id, MascotType.LLAMA.id)
        _mascotXpMap.value = emptyMap()
    }

    fun syncMascotToFirebase(explicitUserId: String? = null) {
        val userId = explicitUserId ?: auth.currentUser?.uid ?: return
        val selectedId = _selectedMascot.value.id
        val purchasedIds = _purchasedMascotIds.value.toList()
        val xpMap = _mascotXpMap.value
        repositoryScope.launch {
            syncMutex.withLock {
                try {
                    val mascotMap = hashMapOf(
                        "selectedMascotId" to selectedId,
                        "unlockedMascotIds" to purchasedIds,
                        "mascotXpMap" to xpMap
                    )
                    firestore.collection("users").document(userId)
                        .set(mascotMap, SetOptions.merge())
                        .await()
                    android.util.Log.d("MascotRepository", "Successfully synced mascot data to Firestore")
                } catch (e: Exception) {
                    android.util.Log.e("MascotRepository", "Failed to sync mascot data to Firestore", e)
                }
            }
        }
    }

    suspend fun syncMascotToFirebaseAwait(userId: String? = null) {
        val uid = userId ?: auth.currentUser?.uid ?: return
        val selectedId = _selectedMascot.value.id
        val purchasedIds = _purchasedMascotIds.value.toList()
        val xpMap = _mascotXpMap.value
        syncMutex.withLock {
            try {
                val mascotMap = hashMapOf(
                    "selectedMascotId" to selectedId,
                    "unlockedMascotIds" to purchasedIds,
                    "mascotXpMap" to xpMap
                )
                firestore.collection("users").document(uid)
                    .set(mascotMap, SetOptions.merge())
                    .await()
                android.util.Log.d("MascotRepository", "Successfully synced (await) mascot data to Firestore")
            } catch (e: Exception) {
                android.util.Log.e("MascotRepository", "Failed to sync (await) mascot data to Firestore", e)
            }
        }
    }

    suspend fun loadMascotFromFirebase(): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("users").document(userId).get().await()
                if (snapshot.exists()) {
                    val editor = prefs.edit()

                    val selectedMascotId = snapshot.getString("selectedMascotId")
                    val unlockedMascotIdsList = snapshot.get("unlockedMascotIds") as? List<*>
                    val unlockedMascotIdsSet = unlockedMascotIdsList?.mapNotNull { it?.toString() }?.toSet()
                    
                    val mascotXpMapData = snapshot.get("mascotXpMap") as? Map<*, *>
                    val mascotXpMapVal = mascotXpMapData?.map { it.key.toString() to (it.value as? Long ?: 0L).toInt() }?.toMap() ?: emptyMap()

                    val mascotToSelect = selectedMascotId?.let { id ->
                        MascotType.entries.find { it.id == id } ?: MascotType.POPPIN
                    }
                    if (mascotToSelect != null) {
                        _selectedMascot.value = mascotToSelect
                        editor.putString(KEY_SELECTED_MASCOT, mascotToSelect.id)
                    }
                    if (unlockedMascotIdsSet != null) {
                        _purchasedMascotIds.value = unlockedMascotIdsSet
                        editor.putStringSet(KEY_UNLOCKED_MASCOTS, unlockedMascotIdsSet)
                    }
                    
                    _mascotXpMap.value = mascotXpMapVal
                    editor.putString(KEY_MASCOT_XP_MAP, gson.toJson(mascotXpMapVal))

                    editor.commit()
                    android.util.Log.d("MascotRepository", "Successfully loaded mascot data from Firestore")
                } else {
                    _selectedMascot.value = MascotType.POPPIN
                    _purchasedMascotIds.value = setOf(MascotType.POPPIN.id, MascotType.LLAMA.id)
                    _mascotXpMap.value = emptyMap()
                    prefs.edit()
                        .putString(KEY_SELECTED_MASCOT, MascotType.POPPIN.id)
                        .putStringSet(KEY_UNLOCKED_MASCOTS, setOf(MascotType.POPPIN.id, MascotType.LLAMA.id))
                        .remove(KEY_MASCOT_XP_MAP)
                        .commit()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("MascotRepository", "Failed to load mascot data from Firestore", e)
                Result.failure(e)
            }
        }
    }

    private fun loadUnlockedMascots(): Set<String> {
        val saved = prefs.getStringSet(KEY_UNLOCKED_MASCOTS, null)
        return saved ?: setOf(MascotType.POPPIN.id, MascotType.LLAMA.id)
    }

    fun unlockMascot(mascotId: String) {
        val wasNew = synchronized(prefs) {
            val current = loadUnlockedMascots()
            if (mascotId !in current) {
                val updated = current + mascotId
                prefs.edit().putStringSet(KEY_UNLOCKED_MASCOTS, updated).commit()
                _purchasedMascotIds.value = updated
                true
            } else false
        }
        if (wasNew) syncMascotToFirebase()
    }

    fun setMascot(mascot: MascotType) {
        synchronized(prefs) {
            _selectedMascot.value = mascot
            prefs.edit().putString(KEY_SELECTED_MASCOT, mascot.id).commit()
        }
        syncMascotToFirebase()
    }

    companion object {
        private const val KEY_SELECTED_MASCOT = "selected_mascot_id"
        private const val KEY_UNLOCKED_MASCOTS = "unlocked_mascot_ids"
        private const val KEY_MASCOT_XP_MAP = "mascot_xp_map"
    }
}
