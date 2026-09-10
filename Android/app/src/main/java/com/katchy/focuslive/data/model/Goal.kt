package com.katchy.focuslive.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId

@Keep
@androidx.compose.runtime.Immutable
data class Goal(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "HABIT", // "SAVINGS", "HABIT", "PROJECT"
    
    // Savings specific
    val currentAmount: Double = 0.0,
    val targetAmount: Double = 0.0,
    val icon: String = "💰", // Default emoji
    
    // Habit specific
    val currentStreak: Int = 0,
    val lastCompletedDate: Long = 0,
    val reminderTime: String? = null, // HH:mm format
    
    // Project/Milestone specific
    val milestones: List<Milestone> = emptyList(),
    val content: String = "",
    val colorHex: String = "#FFF9C4", // Default yellow sticky note
    @get:com.google.firebase.firestore.PropertyName("isPinned")
    @field:com.google.firebase.firestore.PropertyName("isPinned")
    val isPinned: Boolean = false,
    val deadline: Long = 0,
    
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
@androidx.compose.runtime.Immutable
data class Milestone(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    @get:com.google.firebase.firestore.PropertyName("isCompleted")
    @field:com.google.firebase.firestore.PropertyName("isCompleted")
    val isCompleted: Boolean = false,
    val targetDate: Long = 0
)
