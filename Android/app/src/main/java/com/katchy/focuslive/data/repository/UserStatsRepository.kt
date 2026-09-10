package com.katchy.focuslive.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.katchy.focuslive.data.model.UserStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserStatsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val prefs = context.getSharedPreferences("user_stats_prefs", Context.MODE_PRIVATE)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun clearUserData() {
        prefs.edit().clear().commit()
        _isPremium.value = checkCreatorDirectly()
        _userStats.value = loadStats()
        _pendingDailyReward.value = null
    }

    fun syncStatsToFirebase(explicitUserId: String? = null) {
        val userId = explicitUserId ?: auth.currentUser?.uid ?: return
        repositoryScope.launch {
            performStatsSync(userId)
        }
    }

    suspend fun syncStatsToFirebaseAwait(userId: String? = null) {
        val uid = userId ?: auth.currentUser?.uid ?: return
        performStatsSync(uid)
    }

    private suspend fun performStatsSync(userId: String) {
        val stats = loadStats()
        val premiumNow = isPremium.value
        try {
            val statsMap = hashMapOf(
                "totalTasksCompleted" to stats.totalTasksCompleted,
                "totalFocusMinutes" to stats.totalFocusMinutes,
                "currentXp" to stats.currentXp,
                "streak" to stats.streak,
                "coins" to stats.coins,
                "lastLoginDate" to stats.lastLoginDate,
                "badges" to stats.badges,
                "streakFreezes" to stats.streakFreezes,
                "isPremium" to premiumNow
            )
            firestore.collection("users").document(userId)
                .set(statsMap, SetOptions.merge())
                .await()
            Log.d("UserStatsRepository", "Successfully synced user stats to Firestore")
        } catch (e: Exception) {
            Log.e("UserStatsRepository", "Failed to sync user stats to Firestore", e)
        }
    }

    suspend fun loadStatsFromFirebase(): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("No user logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("users").document(userId).get().await()
                if (snapshot.exists()) {
                    val editor = prefs.edit()

                    val totalTasksCompleted = snapshot.getLong("totalTasksCompleted")?.toInt()
                    val totalFocusMinutes = snapshot.getLong("totalFocusMinutes")?.toInt()
                    val currentXp = snapshot.getLong("currentXp")?.toInt()
                    val streak = snapshot.getLong("streak")?.toInt()
                    val coins = snapshot.getLong("coins")?.toInt()
                    val lastLoginDate = snapshot.getLong("lastLoginDate")
                    val streakFreezes = snapshot.getLong("streakFreezes")?.toInt()
                    val isPremiumVal = snapshot.getBoolean("isPremium")

                    val badgesList = snapshot.get("badges") as? List<*>
                    val badgesSet = badgesList?.mapNotNull { it?.toString() }?.toSet()

                    totalTasksCompleted?.let { editor.putInt(KEY_TASKS, it) }
                    totalFocusMinutes?.let { editor.putInt(KEY_FOCUS_MINS, it) }
                    currentXp?.let { editor.putInt(KEY_XP, it) }
                    streak?.let { editor.putInt(KEY_STREAK, it) }
                    coins?.let { editor.putInt(KEY_COINS, it) }
                    lastLoginDate?.let { editor.putLong(KEY_LAST_LOGIN, it) }
                    streakFreezes?.let { editor.putInt(KEY_STREAK_FREEZES, it) }
                    isPremiumVal?.let {
                        val finalPremium = it || checkCreatorDirectly()
                        editor.putBoolean(KEY_IS_PREMIUM, finalPremium)
                        _isPremium.value = finalPremium
                    }
                    badgesSet?.let { editor.putStringSet(KEY_BADGES, it) }

                    editor.commit()
                    _userStats.value = loadStats()
                    Log.d("UserStatsRepository", "Successfully loaded stats from Firebase Firestore")
                } else {
                    prefs.edit().clear().commit()
                    _isPremium.value = checkCreatorDirectly()
                    _userStats.value = loadStats()
                    dismissPendingDailyReward()
                    Log.d("UserStatsRepository", "No stats doc found; reset to defaults")
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("UserStatsRepository", "Failed to load stats from Firebase Firestore", e)
                Result.failure(e)
            }
        }
    }

    private fun notifyAndSync() {
        _userStats.value = loadStats()
        syncStatsToFirebase()
    }

    private val _userStats = MutableStateFlow(loadStats())
    val userStats: StateFlow<UserStats> = _userStats.asStateFlow()

    private fun checkCreatorDirectly(): Boolean {
        val user = auth.currentUser ?: return false
        val email = user.email?.lowercase()?.trim() ?: ""
        val allowedEmails = setOf(
            "josephivan050505@gmail.com",
            "cachayvargasivan@gmail.com",
            "dzenwertz05@gmail.com"
        )
        val match = allowedEmails.contains(email)
        if (match) {
            prefs.edit().putBoolean(KEY_IS_PREMIUM, true).apply()
        }
        return match
    }

    private val _isPremium = MutableStateFlow(prefs.getBoolean(KEY_IS_PREMIUM, false) || checkCreatorDirectly())
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _newlyUnlockedBadge = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val newlyUnlockedBadge = _newlyUnlockedBadge.asSharedFlow()

    fun setPremium(enabled: Boolean) {
        val finalEnabled = enabled || checkCreatorDirectly()
        prefs.edit().putBoolean(KEY_IS_PREMIUM, finalEnabled).apply()
        _isPremium.value = finalEnabled
        notifyAndSync()
        if (finalEnabled) {
            unlockBadge(com.katchy.focuslive.util.GamificationLogic.BADGE_PREMIUM_MEMBER)
        }
    }

    private val _pendingDailyReward = MutableStateFlow<DailyReward?>(loadPendingReward())
    val pendingDailyReward: StateFlow<DailyReward?> = _pendingDailyReward.asStateFlow()

    private fun calculateLevelStats(xp: Int): LevelStats {
        var level = 1
        var currentLevelMinXp = 0
        var nextLevelMinXp = 100
        
        while (true) {
            val limit = level * (100 + 25 * (level - 1))
            if (xp < limit) {
                nextLevelMinXp = limit
                break
            }
            currentLevelMinXp = limit
            level++
        }
        
        val progress = if (nextLevelXpRange(currentLevelMinXp, nextLevelMinXp)) {
            (xp - currentLevelMinXp).toFloat() / (nextLevelMinXp - currentLevelMinXp).toFloat()
        } else 0f
        
        return LevelStats(level, currentLevelMinXp, nextLevelMinXp, progress)
    }

    private fun nextLevelXpRange(min: Int, max: Int): Boolean = max > min

    private data class LevelStats(
        val level: Int,
        val currentLevelMinXp: Int,
        val nextLevelXp: Int,
        val progress: Float
    )

    private fun loadPendingReward(): DailyReward? {
        val hasPending = prefs.getBoolean(KEY_HAS_PENDING_REWARD, false)
        if (!hasPending) return null
        return DailyReward(
            streak = prefs.getInt(KEY_PENDING_STREAK, 0),
            coinsAwarded = prefs.getInt(KEY_PENDING_COINS, 0),
            xpAwarded = prefs.getInt(KEY_PENDING_XP, 0),
            streakFreezesConsumed = prefs.getInt(KEY_PENDING_FREEZES, 0),
            streakFrozen = prefs.getBoolean(KEY_PENDING_FROZEN, false),
            streakReset = prefs.getBoolean(KEY_PENDING_RESET, false)
        )
    }

    private fun savePendingReward(reward: DailyReward) {
        prefs.edit()
            .putBoolean(KEY_HAS_PENDING_REWARD, true)
            .putInt(KEY_PENDING_STREAK, reward.streak)
            .putInt(KEY_PENDING_COINS, reward.coinsAwarded)
            .putInt(KEY_PENDING_XP, reward.xpAwarded)
            .putInt(KEY_PENDING_FREEZES, reward.streakFreezesConsumed)
            .putBoolean(KEY_PENDING_FROZEN, reward.streakFrozen)
            .putBoolean(KEY_PENDING_RESET, reward.streakReset)
            .apply()
        _pendingDailyReward.value = reward
    }

    fun dismissPendingDailyReward() {
        prefs.edit()
            .putBoolean(KEY_HAS_PENDING_REWARD, false)
            .remove(KEY_PENDING_STREAK)
            .remove(KEY_PENDING_COINS)
            .remove(KEY_PENDING_XP)
            .remove(KEY_PENDING_FREEZES)
            .remove(KEY_PENDING_FROZEN)
            .remove(KEY_PENDING_RESET)
            .apply()
        _pendingDailyReward.value = null
    }

    private fun loadStats(): UserStats {
        val xp = prefs.getInt(KEY_XP, 0)
        val levelStats = calculateLevelStats(xp)

        return UserStats(
            totalTasksCompleted = prefs.getInt(KEY_TASKS, 0),
            totalFocusMinutes = prefs.getInt(KEY_FOCUS_MINS, 0),
            currentXp = xp,
            currentLevel = levelStats.level,
            nextLevelXp = levelStats.nextLevelXp,
            progressToNextLevel = levelStats.progress,
            streak = prefs.getInt(KEY_STREAK, 0),
            coins = prefs.getInt(KEY_COINS, 0),
            lastLoginDate = prefs.getLong(KEY_LAST_LOGIN, 0),
            badges = (prefs.getStringSet(KEY_BADGES, emptySet()) ?: emptySet()).toList(),
            streakFreezes = prefs.getInt(KEY_STREAK_FREEZES, 0)
        )
    }

    fun addXp(amount: Int, multiplyIfPremium: Boolean = true) {
        synchronized(prefs) {
            val multiplier = if (multiplyIfPremium && isPremium.value) 2 else 1
            val actualAmount = amount * multiplier
            val currentXp = prefs.getInt(KEY_XP, 0)
            prefs.edit().putInt(KEY_XP, currentXp + actualAmount).commit()
        }
        notifyAndSync()
    }

    fun deductXp(amount: Int, multiplyIfPremium: Boolean = true) {
        synchronized(prefs) {
            val multiplier = if (multiplyIfPremium && isPremium.value) 2 else 1
            val actualAmount = amount * multiplier
            val currentXp = prefs.getInt(KEY_XP, 0)
            prefs.edit().putInt(KEY_XP, maxOf(0, currentXp - actualAmount)).commit()
        }
        notifyAndSync()
    }

    fun addFocusMinutes(minutes: Int) {
        val currentMins = synchronized(prefs) {
            val v = prefs.getInt(KEY_FOCUS_MINS, 0) + minutes
            prefs.edit().putInt(KEY_FOCUS_MINS, v).commit()
            v
        }
        notifyAndSync()
        if (currentMins >= 25) unlockBadge(com.katchy.focuslive.util.GamificationLogic.BADGE_POMODORO_1)
        if (currentMins >= 125) unlockBadge(com.katchy.focuslive.util.GamificationLogic.BADGE_POMODORO_5)
    }

    fun incrementTasks() {
        val currentTasks = synchronized(prefs) {
            val v = prefs.getInt(KEY_TASKS, 0) + 1
            prefs.edit().putInt(KEY_TASKS, v).commit()
            v
        }
        notifyAndSync()
        if (currentTasks >= 10) unlockBadge(com.katchy.focuslive.util.GamificationLogic.BADGE_PLANNER_PRO)
    }

    fun decrementTasks() {
        synchronized(prefs) {
            val currentTasks = prefs.getInt(KEY_TASKS, 0)
            if (currentTasks > 0) prefs.edit().putInt(KEY_TASKS, currentTasks - 1).commit()
        }
        notifyAndSync()
    }

    fun incrementHabits() {
        val currentHabits = synchronized(prefs) {
            val v = prefs.getInt(KEY_HABITS_COMPLETED, 0) + 1
            prefs.edit().putInt(KEY_HABITS_COMPLETED, v).commit()
            v
        }
        notifyAndSync()
        if (currentHabits >= 10) unlockBadge(com.katchy.focuslive.util.GamificationLogic.BADGE_HABIT_MASTER)
    }

    fun decrementHabits() {
        synchronized(prefs) {
            val currentHabits = prefs.getInt(KEY_HABITS_COMPLETED, 0)
            if (currentHabits > 0) prefs.edit().putInt(KEY_HABITS_COMPLETED, currentHabits - 1).commit()
        }
        notifyAndSync()
    }

    fun incrementNotes() {
        val currentNotes = synchronized(prefs) {
            val v = prefs.getInt(KEY_NOTES_CREATED, 0) + 1
            prefs.edit().putInt(KEY_NOTES_CREATED, v).commit()
            v
        }
        notifyAndSync()
        if (currentNotes >= 5) unlockBadge(com.katchy.focuslive.util.GamificationLogic.BADGE_NOTE_TAKER)
    }

    fun checkDailyLogin() {
        val today = System.currentTimeMillis()

        val snapshot: DailyLoginSnapshot = synchronized(prefs) {
            val lastLogin = prefs.getLong(KEY_LAST_LOGIN, 0L)
            val currentStreak = prefs.getInt(KEY_STREAK, 0)
            val currentFreezes = prefs.getInt(KEY_STREAK_FREEZES, 0)
            DailyLoginSnapshot(lastLogin, currentStreak, currentFreezes)
        }

        if (snapshot.lastLogin == 0L) {
            val newStreak = 1
            val baseCoins = 5
            val baseXp = 10
            val dailyCoins = if (isPremium.value) baseCoins * 2 else baseCoins
            val dailyXp = if (isPremium.value) baseXp * 2 else baseXp

            synchronized(prefs) {
                prefs.edit()
                    .putLong(KEY_LAST_LOGIN, today)
                    .putInt(KEY_STREAK, newStreak)
                    .commit()
            }
            addCoins(dailyCoins, multiplyIfPremium = false)
            addXp(dailyXp, multiplyIfPremium = false)
            savePendingReward(
                DailyReward(
                    streak = newStreak,
                    coinsAwarded = dailyCoins,
                    xpAwarded = dailyXp,
                    streakFreezesConsumed = 0,
                    streakFrozen = false,
                    streakReset = false
                )
            )
            return
        }

        val calLastMidnight = java.util.Calendar.getInstance().apply {
            timeInMillis = snapshot.lastLogin
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val calTodayMidnight = java.util.Calendar.getInstance().apply {
            timeInMillis = today
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val diffMillis = calTodayMidnight.timeInMillis - calLastMidnight.timeInMillis
        val diffDays = Math.round(diffMillis.toDouble() / (1000 * 60 * 60 * 24)).toInt()

        if (diffDays <= 0) return

        var newStreak = snapshot.currentStreak
        var freezesConsumed = 0
        var streakFrozen = false
        var streakReset = false
        var dailyCoins = 0
        var dailyXp = 0

        if (diffDays == 1) {
            newStreak = snapshot.currentStreak + 1
            if (newStreak == 3) unlockBadge("STREAK_3")
            if (newStreak == 7) unlockBadge("STREAK_7")
            if (newStreak == 30) unlockBadge("STREAK_30")
            val baseCoins = minOf(newStreak * 2, 20)
            val baseXp = minOf(newStreak * 5, 50)
            dailyCoins = if (isPremium.value) baseCoins * 2 else baseCoins
            dailyXp = if (isPremium.value) baseXp * 2 else baseXp
        } else {
            val daysMissed = diffDays - 1
            if (snapshot.currentFreezes >= daysMissed) {
                freezesConsumed = daysMissed
                streakFrozen = true
                newStreak = snapshot.currentStreak + 1
                if (newStreak == 3) unlockBadge("STREAK_3")
                if (newStreak == 7) unlockBadge("STREAK_7")
                if (newStreak == 30) unlockBadge("STREAK_30")
                val baseCoins = minOf(newStreak * 2, 20)
                val baseXp = minOf(newStreak * 5, 50)
                dailyCoins = if (isPremium.value) baseCoins * 2 else baseCoins
                dailyXp = if (isPremium.value) baseXp * 2 else baseXp
            } else {
                streakReset = true
                newStreak = 1
                val baseCoins = 5
                val baseXp = 10
                dailyCoins = if (isPremium.value) baseCoins * 2 else baseCoins
                dailyXp = if (isPremium.value) baseXp * 2 else baseXp
            }
        }

        synchronized(prefs) {
            prefs.edit()
                .putLong(KEY_LAST_LOGIN, today)
                .putInt(KEY_STREAK, newStreak)
                .putInt(KEY_STREAK_FREEZES, snapshot.currentFreezes - freezesConsumed)
                .commit()
        }

        addCoins(dailyCoins, multiplyIfPremium = false)
        addXp(dailyXp, multiplyIfPremium = false)
        savePendingReward(
            DailyReward(
                streak = newStreak,
                coinsAwarded = dailyCoins,
                xpAwarded = dailyXp,
                streakFreezesConsumed = freezesConsumed,
                streakFrozen = streakFrozen,
                streakReset = streakReset
            )
        )
    }

    private data class DailyLoginSnapshot(
        val lastLogin: Long,
        val currentStreak: Int,
        val currentFreezes: Int
    )

    fun unlockBadge(badgeId: String) {
        val wasNew = synchronized(prefs) {
            val currentBadges = prefs.getStringSet(KEY_BADGES, emptySet()) ?: emptySet()
            if (!currentBadges.contains(badgeId)) {
                prefs.edit()
                    .putStringSet(KEY_BADGES, currentBadges + badgeId)
                    .commit()
                true
            } else false
        }
        if (!wasNew) return
        addXp(100, multiplyIfPremium = true)
        addCoins(50, multiplyIfPremium = true)
        _newlyUnlockedBadge.tryEmit(badgeId)
    }

    fun addCoins(amount: Int, multiplyIfPremium: Boolean = true) {
        synchronized(prefs) {
            val multiplier = if (multiplyIfPremium && isPremium.value) 2 else 1
            val actualAmount = amount * multiplier
            val currentCoins = prefs.getInt(KEY_COINS, 0)
            prefs.edit().putInt(KEY_COINS, currentCoins + actualAmount).commit()
        }
        notifyAndSync()
    }

    fun deductCoins(amount: Int, multiplyIfPremium: Boolean = true) {
        synchronized(prefs) {
            val multiplier = if (multiplyIfPremium && isPremium.value) 2 else 1
            val actualAmount = amount * multiplier
            val currentCoins = prefs.getInt(KEY_COINS, 0)
            prefs.edit().putInt(KEY_COINS, maxOf(0, currentCoins - actualAmount)).commit()
        }
        notifyAndSync()
    }

    fun spendCoins(amount: Int): Boolean {
        val success = synchronized(prefs) {
            val currentCoins = prefs.getInt(KEY_COINS, 0)
            if (currentCoins >= amount) {
                prefs.edit().putInt(KEY_COINS, currentCoins - amount).commit()
                true
            } else false
        }
        if (success) notifyAndSync()
        return success
    }

    fun buyStreakFreeze(cost: Int = 50): Boolean {
        val success = synchronized(prefs) {
            val currentCoins = prefs.getInt(KEY_COINS, 0)
            val freezes = prefs.getInt(KEY_STREAK_FREEZES, 0)
            if (currentCoins >= cost) {
                prefs.edit()
                    .putInt(KEY_COINS, currentCoins - cost)
                    .putInt(KEY_STREAK_FREEZES, freezes + 1)
                    .commit()
                true
            } else false
        }
        if (success) notifyAndSync()
        return success
    }

    fun getStreakMultiplier(): Float {
        val streak = prefs.getInt(KEY_STREAK, 0)
        return when {
            streak >= 15 -> 2.0f
            streak >= 8 -> 1.5f
            streak >= 4 -> 1.2f
            else -> 1.0f
        }
    }

    fun getRemainingAiMessages(): Int {
        if (isPremium.value) return -1
        
        val lastAiDate = prefs.getLong(KEY_LAST_AI_DATE, 0L)
        val today = getTodayMidnight()
        
        if (lastAiDate < today) {
            prefs.edit().putInt(KEY_DAILY_AI_COUNT, 0).apply()
            return MAX_FREE_AI_MESSAGES
        }
        
        val count = prefs.getInt(KEY_DAILY_AI_COUNT, 0)
        return maxOf(0, MAX_FREE_AI_MESSAGES - count)
    }

    fun incrementAiMessageCount(): Boolean {
        if (isPremium.value) return true
        
        val lastAiDate = prefs.getLong(KEY_LAST_AI_DATE, 0L)
        val today = getTodayMidnight()
        
        if (lastAiDate < today) {
            prefs.edit()
                .putLong(KEY_LAST_AI_DATE, System.currentTimeMillis())
                .putInt(KEY_DAILY_AI_COUNT, 1)
                .apply()
            return true
        }
        
        val count = prefs.getInt(KEY_DAILY_AI_COUNT, 0)
        if (count >= MAX_FREE_AI_MESSAGES) {
            return false
        }
        
        prefs.edit().putInt(KEY_DAILY_AI_COUNT, count + 1).apply()
        return true
    }

    fun refundAiMessage() {
        if (isPremium.value) return
        val count = prefs.getInt(KEY_DAILY_AI_COUNT, 0)
        if (count > 0) {
            prefs.edit().putInt(KEY_DAILY_AI_COUNT, count - 1).apply()
        }
    }

    fun getRemainingFlashcardGenerations(): Int {
        if (isPremium.value) return -1
        val count = prefs.getInt(KEY_FLASHCARD_GENERATIONS_COUNT, 0)
        return maxOf(0, 2 - count)
    }

    fun incrementFlashcardGenerationCount(): Boolean {
        if (isPremium.value) return true
        val count = prefs.getInt(KEY_FLASHCARD_GENERATIONS_COUNT, 0)
        if (count >= 2) return false
        prefs.edit().putInt(KEY_FLASHCARD_GENERATIONS_COUNT, count + 1).apply()
        return true
    }

    private fun getTodayMidnight(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    companion object {
        private const val KEY_XP = "current_xp"
        private const val KEY_TASKS = "total_tasks"
        private const val KEY_FOCUS_MINS = "focus_mins"
        private const val KEY_STREAK = "current_streak"
        private const val KEY_COINS = "coins"
        private const val KEY_LAST_LOGIN = "last_login_date"
        private const val KEY_BADGES = "earned_badges"
        private const val KEY_STREAK_FREEZES = "streak_freezes"

        private const val KEY_HAS_PENDING_REWARD = "has_pending_reward"
        private const val KEY_PENDING_STREAK = "pending_reward_streak"
        private const val KEY_PENDING_COINS = "pending_reward_coins"
        private const val KEY_PENDING_XP = "pending_reward_xp"
        private const val KEY_IS_PREMIUM = "is_premium"
        private const val KEY_PENDING_FREEZES = "pending_reward_freezes"
        private const val KEY_PENDING_FROZEN = "pending_reward_frozen"
        private const val KEY_PENDING_RESET = "pending_reward_reset"

        private const val KEY_DAILY_AI_COUNT = "daily_ai_message_count"
        private const val KEY_LAST_AI_DATE = "last_ai_message_date"
        private const val MAX_FREE_AI_MESSAGES = 10
        private const val KEY_HABITS_COMPLETED = "total_habits_completed"
        private const val KEY_NOTES_CREATED = "total_notes_created"
        private const val KEY_FLASHCARD_GENERATIONS_COUNT = "flashcard_generations_count"
    }
}

data class DailyReward(
    val streak: Int,
    val coinsAwarded: Int,
    val xpAwarded: Int,
    val streakFreezesConsumed: Int,
    val streakFrozen: Boolean,
    val streakReset: Boolean
)
