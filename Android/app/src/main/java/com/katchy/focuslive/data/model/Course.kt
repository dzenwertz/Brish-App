package com.katchy.focuslive.data.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.DocumentId

@Keep
@Immutable
@androidx.room.Entity(
    tableName = "courses",
    indices = [androidx.room.Index(value = ["userId"])]
)
data class Course(
    @androidx.room.PrimaryKey
    @DocumentId val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val color: Long = 0xFF06B6D4,
    val semester: String = "",
    @androidx.room.ColumnInfo(defaultValue = "")
    val spaceId: String = "",
    val userId: String = "",
    @get:com.google.firebase.firestore.Exclude
    val isSynced: Boolean = true
)
