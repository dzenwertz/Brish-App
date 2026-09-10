package com.katchy.focuslive.data.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.DocumentId

@Keep
@Immutable
data class SubtaskItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    @get:com.google.firebase.firestore.PropertyName("isCompleted")
    @field:com.google.firebase.firestore.PropertyName("isCompleted")
    val isCompleted: Boolean = false,
    val completedDates: List<Long> = emptyList()
)

@Keep
@Immutable
@androidx.room.Entity(tableName = "tasks", indices = [androidx.room.Index(value = ["courseId"])])
data class Task(
    @androidx.room.PrimaryKey
    @DocumentId val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val category: String = "General",
    val courseId: String? = null,
    val deliverableType: String? = null,
    @get:com.google.firebase.firestore.PropertyName("isCompleted")
    @field:com.google.firebase.firestore.PropertyName("isCompleted")
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis(),
    @get:com.google.firebase.firestore.PropertyName("hasDueDate")
    @field:com.google.firebase.firestore.PropertyName("hasDueDate")
    @androidx.room.ColumnInfo(defaultValue = "1")
    val hasDueDate: Boolean = true,
    val time: String? = null,
    val endTime: String? = null,
    val repeatMode: String = "NONE",
    val repeatDays: List<Int> = emptyList(),
    @get:com.google.firebase.firestore.PropertyName("isMyDay")
    @field:com.google.firebase.firestore.PropertyName("isMyDay")
    val isMyDay: Boolean = false,
    val completedDates: List<Long> = emptyList(),
    val priority: String = "MEDIUM",
    val description: String = "",
    val subtasks: List<SubtaskItem> = emptyList(),
    val userId: String = "", // User ID for multi-account support
    @get:com.google.firebase.firestore.Exclude
    val isSynced: Boolean = true
)
