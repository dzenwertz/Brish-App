package com.katchy.focuslive.data.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class UserStats(
    val totalTasksCompleted: Int = 0,
    val totalFocusMinutes: Int = 0,
    val currentXp: Int = 0,
    val currentLevel: Int = 1,
    val nextLevelXp: Int = 100,
    val progressToNextLevel: Float = 0f,
    val streak: Int = 0,
    val coins: Int = 0,
    val lastLoginDate: Long = 0,
    val badges: List<String> = emptyList(),
    val streakFreezes: Int = 0
)
