package com.katchy.focuslive.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import androidx.compose.runtime.Immutable

@Keep
@Immutable
@androidx.room.Entity(tableName = "notes", indices = [androidx.room.Index(value = ["courseId"])])
data class Note(
    @androidx.room.PrimaryKey
    @DocumentId val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val colorHex: String = "#FFF9C4", // Default yellow sticky note
    @get:com.google.firebase.firestore.PropertyName("isPinned")
    @field:com.google.firebase.firestore.PropertyName("isPinned")
    val isPinned: Boolean = false,
    val priority: String = "LOW", // LOW, MEDIUM, HIGH
    val timestamp: Long = System.currentTimeMillis(),
    val categoryId: String? = null, // Link to NoteCategory
    val courseId: String? = null,
    val userId: String = "", // User ID for multi-account support
    val flashcards: List<FlashcardItem> = emptyList(),
    @get:com.google.firebase.firestore.Exclude
    val isSynced: Boolean = true
)
