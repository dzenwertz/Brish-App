package com.katchy.focuslive.data.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import java.util.UUID

@Keep
@Immutable
@Entity(
    tableName = "learning_spaces",
    indices = [Index(value = ["userId"], name = "index_learning_spaces_userId")]
)
data class LearningSpace(
    @PrimaryKey
    @DocumentId val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    @ColumnInfo(defaultValue = "")
    val userId: String = "",
    @get:com.google.firebase.firestore.PropertyName("isArchived")
    @field:com.google.firebase.firestore.PropertyName("isArchived")
    @ColumnInfo(defaultValue = "0")
    val isArchived: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis(),
    @get:com.google.firebase.firestore.Exclude
    @ColumnInfo(defaultValue = "1")
    val isSynced: Boolean = true
)
