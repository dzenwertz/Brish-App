package com.katchy.focuslive.data.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.DocumentId

@Keep
@Immutable
@androidx.room.Entity(tableName = "habits")
data class Habit(
    @androidx.room.PrimaryKey
    @DocumentId val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val icon: String = "🌱", // Emoji or icon name
    val currentStreak: Int = 0,
    val completedDates: List<Long> = emptyList(), // Timestamps of completion
    val reminderTime: String? = null, // "HH:mm" format, e.g., "15:30"
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // Default: All days (Calendar.SUNDAY=1 to SATURDAY=7)
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "", // User ID for multi-account support
    @get:com.google.firebase.firestore.Exclude
    val isSynced: Boolean = true
)
