package com.katchy.focuslive.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.katchy.focuslive.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserStatsRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: UserStatsRepository
    private lateinit var prefs: SharedPreferences
    private lateinit var storage: MutableMap<String, Any?>
    private val auth: FirebaseAuth = mockk(relaxed = true)
    private val firestore: FirebaseFirestore = mockk(relaxed = true)
    private val documentRef: DocumentReference = mockk(relaxed = true)
    private val user: FirebaseUser = mockk(relaxed = true)

    @Before
    fun setup() {
        every { auth.currentUser } returns user
        every { user.uid } returns "user1"

        val collectionRef = mockk<CollectionReference>(relaxed = true)
        every { firestore.collection("users") } returns collectionRef
        every { collectionRef.document(any()) } returns documentRef
        every { documentRef.set(any(), any()) } returns completedVoidTask()

        prefs = fakeSharedPreferences()
        val context = mockk<Context>(relaxed = true)
        every { context.getSharedPreferences("user_stats_prefs", Context.MODE_PRIVATE) } returns prefs

        repository = UserStatsRepository(context, auth, firestore)
    }

    private fun fakeSharedPreferences(): SharedPreferences {
        storage = mutableMapOf()
        val prefs = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)

        every { prefs.getInt(any(), any()) } answers {
            storage[firstArg()] as? Int ?: secondArg()
        }
        every { prefs.getLong(any(), any()) } answers {
            storage[firstArg()] as? Long ?: secondArg()
        }
        every { prefs.getBoolean(any(), any()) } answers {
            storage[firstArg()] as? Boolean ?: secondArg()
        }
        every { prefs.getStringSet(any(), any()) } answers {
            @Suppress("UNCHECKED_CAST")
            (storage[firstArg()] as? Set<String>)?.toMutableSet() ?: mutableSetOf()
        }
        every { prefs.getString(any(), any()) } answers {
            storage[firstArg()] as? String ?: secondArg()
        }
        every { prefs.edit() } returns editor

        every { editor.putInt(any(), any()) } answers {
            storage[firstArg()] = secondArg()
            editor
        }
        every { editor.putLong(any(), any()) } answers {
            storage[firstArg()] = secondArg()
            editor
        }
        every { editor.putBoolean(any(), any()) } answers {
            storage[firstArg()] = secondArg()
            editor
        }
        every { editor.putStringSet(any(), any()) } answers {
            storage[firstArg()] = secondArg()
            editor
        }
        every { editor.putString(any(), any()) } answers {
            storage[firstArg()] = secondArg()
            editor
        }
        every { editor.remove(any()) } answers {
            storage.remove(firstArg())
            editor
        }
        every { editor.clear() } answers {
            storage.clear()
            editor
        }
        every { editor.apply() } returns Unit
        every { editor.commit() } returns true

        return prefs
    }

    private fun completedVoidTask(): Task<Void> {
        val task = mockk<Task<Void>>(relaxed = true)
        every { task.isComplete } returns true
        every { task.isSuccessful } returns true
        every { task.result } returns null
        every { task.exception } returns null
        return task
    }

    private fun documentSnapshot(data: Map<String, Any?>): DocumentSnapshot {
        val snap = mockk<DocumentSnapshot>(relaxed = true)
        every { snap.exists() } returns true
        data.forEach { (k, v) ->
            when (v) {
                is Long -> every { snap.getLong(k) } returns v
                is Int -> every { snap.getLong(k) } returns v.toLong()
                is Boolean -> every { snap.getBoolean(k) } returns v
                is List<*> -> every { snap.get(k) } returns v
                null -> Unit
                else -> every { snap.get(k) } returns v
            }
        }
        return snap
    }

    private fun completedTaskOf(snap: DocumentSnapshot): Task<DocumentSnapshot> {
        val task = mockk<Task<DocumentSnapshot>>(relaxed = true)
        every { task.isComplete } returns true
        every { task.isSuccessful } returns true
        every { task.result } returns snap
        every { task.exception } returns null
        return task
    }

    @Test
    fun `addXp increments xp and emits updated stats`() = runTest {
        repository.addXp(50)
        advanceUntilIdle()

        assertEquals(50, repository.userStats.value.currentXp)
        assertEquals(50, storage["current_xp"])
    }

    @Test
    fun `addCoins increments coins and emits updated stats`() = runTest {
        repository.addCoins(20)
        advanceUntilIdle()

        assertEquals(20, repository.userStats.value.coins)
        assertEquals(20, storage["coins"])
    }

    @Test
    fun `spendCoins returns false when balance is insufficient`() = runTest {
        repository.addCoins(10)
        advanceUntilIdle()

        val result = repository.spendCoins(20)

        assertFalse(result)
        assertEquals(10, repository.userStats.value.coins)
    }

    @Test
    fun `spendCoins returns true and deducts when balance is sufficient`() = runTest {
        repository.addCoins(100)
        advanceUntilIdle()

        val result = repository.spendCoins(30)

        assertTrue(result)
        assertEquals(70, repository.userStats.value.coins)
    }

    @Test
    fun `unlockBadge adds badge rewards and emits badge event`() = runTest {
        repository.unlockBadge("FIRST_TASK")
        advanceUntilIdle()

        val stats = repository.userStats.value
        assertTrue(stats.badges.contains("FIRST_TASK"))
        assertEquals(100, stats.currentXp)
        assertEquals(50, stats.coins)
    }

    @Test
    fun `unlockBadge does not duplicate existing badge`() = runTest {
        repository.unlockBadge("FIRST_TASK")
        repository.unlockBadge("FIRST_TASK")
        advanceUntilIdle()

        assertEquals(100, repository.userStats.value.currentXp)
        assertEquals(50, repository.userStats.value.coins)
    }

    @Test
    fun `checkDailyLogin on first run sets streak to 1 and rewards`() = runTest {
        repository.checkDailyLogin()
        advanceUntilIdle()

        assertEquals(1, repository.userStats.value.streak)
        assertEquals(10, repository.userStats.value.currentXp)
        assertEquals(5, repository.userStats.value.coins)
        assertTrue(repository.pendingDailyReward.value != null)
    }

    @Test
    fun `checkDailyLogin same day does nothing`() = runTest {
        repository.checkDailyLogin()
        advanceUntilIdle()
        val firstCoins = repository.userStats.value.coins

        repository.checkDailyLogin()
        advanceUntilIdle()

        assertEquals(firstCoins, repository.userStats.value.coins)
    }

    @Test
    fun `clearUserData resets stats to defaults`() = runTest {
        repository.addXp(100)
        repository.addCoins(50)
        advanceUntilIdle()

        repository.clearUserData()

        assertEquals(0, repository.userStats.value.currentXp)
        assertEquals(0, repository.userStats.value.coins)
        assertFalse(repository.isPremium.value)
    }

    @Test
    fun `loadStatsFromFirebase with existing doc updates local stats`() = runTest {
        val snap = documentSnapshot(
            mapOf(
                "totalTasksCompleted" to 5L,
                "totalFocusMinutes" to 60L,
                "currentXp" to 200L,
                "streak" to 3L,
                "coins" to 42L,
                "lastLoginDate" to 1_700_000_000_000L,
                "streakFreezes" to 1L,
                "isPremium" to true,
                "badges" to listOf("BADGE_1")
            )
        )
        every { documentRef.get() } returns completedTaskOf(snap)

        repository.loadStatsFromFirebase()
        advanceUntilIdle()

        assertEquals(200, repository.userStats.value.currentXp)
        assertEquals(42, repository.userStats.value.coins)
        assertEquals(3, repository.userStats.value.streak)
        assertTrue(repository.isPremium.value)
        assertTrue(repository.userStats.value.badges.contains("BADGE_1"))
    }

    @Test
    fun `loadStatsFromFirebase with missing doc resets to defaults`() = runTest {
        val snap = mockk<DocumentSnapshot>(relaxed = true)
        every { snap.exists() } returns false
        every { documentRef.get() } returns completedTaskOf(snap)

        repository.addXp(999)
        repository.addCoins(999)
        advanceUntilIdle()

        repository.loadStatsFromFirebase()
        advanceUntilIdle()

        assertEquals(0, repository.userStats.value.currentXp)
        assertEquals(0, repository.userStats.value.coins)
        assertEquals(0, repository.userStats.value.streak)
        assertFalse(repository.isPremium.value)
    }
}
