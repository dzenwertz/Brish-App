package com.katchy.focuslive.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo
import com.google.firebase.firestore.DocumentId
import java.util.UUID

@Keep
@Entity(
    tableName = "note_categories",
    indices = [Index(value = ["userId"], name = "index_note_categories_userId")]
)
data class NoteCategory(
    @PrimaryKey
    @DocumentId val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val sortOrder: Int = 0,
    @get:com.google.firebase.firestore.PropertyName("isDefault")
    @field:com.google.firebase.firestore.PropertyName("isDefault")
    val isDefault: Boolean = false,
    @ColumnInfo(defaultValue = "")
    val userId: String = "",
    @get:com.google.firebase.firestore.Exclude
    @ColumnInfo(defaultValue = "1")
    val isSynced: Boolean = true
)
