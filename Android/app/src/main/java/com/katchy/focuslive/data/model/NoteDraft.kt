package com.katchy.focuslive.data.model

/** Local-only editor recovery data, never uploaded to Firestore. */
@androidx.room.Entity(tableName = "note_drafts", primaryKeys = ["userId", "noteId"])
data class NoteDraft(
    val userId: String,
    val noteId: String,
    val payload: String,
    val updatedAt: Long
)
