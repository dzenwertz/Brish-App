package com.katchy.focuslive.data.model

import androidx.room.Entity
import androidx.room.Index

/**
 * Tombstone local para borrados pendientes de sincronizar con Firestore.
 * Cuando el usuario borra un ítem estando offline (o simplemente antes de que
 * el SyncWorker suba el cambio), guardamos su id aquí. syncPendingChanges()
 * intentará borrar el documento remoto; refreshX() evitará resucitar ítems
 * que aún tengan una tumba pendiente.
 */
@Entity(
    tableName = "pending_deletes",
    primaryKeys = ["itemId", "type", "userId"],
    indices = [Index(value = ["userId", "type"], name = "index_pending_deletes_userId_type")]
)
data class PendingDelete(
    val itemId: String,
    val type: String, // "task", "note", "habit", "transaction", "note_category"
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)
