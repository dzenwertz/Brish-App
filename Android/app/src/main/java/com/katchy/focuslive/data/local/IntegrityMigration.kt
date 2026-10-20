package com.katchy.focuslive.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive migration: keep every legacy finance/habit row intact. */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Earlier migrations created indexes absent from the v13 entity declarations.
        // Normalize upgraded databases to the same schema as fresh installs.
        database.execSQL("DROP INDEX IF EXISTS index_courses_spaceId")
        database.execSQL("DROP INDEX IF EXISTS index_tasks_userId")
        database.execSQL("DROP INDEX IF EXISTS index_notes_userId")
        database.execSQL("DROP INDEX IF EXISTS index_habits_userId")
        database.execSQL("DROP INDEX IF EXISTS index_transactions_userId")
        database.execSQL("ALTER TABLE tasks ADD COLUMN kind TEXT NOT NULL DEFAULT 'AGENDA'")
        database.execSQL("UPDATE tasks SET kind = 'DELIVERY' WHERE (courseId IS NOT NULL AND TRIM(courseId) != '') OR (deliverableType IS NOT NULL AND TRIM(deliverableType) != '')")
        database.execSQL("ALTER TABLE note_categories ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE courses ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE learning_spaces ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE tasks ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE notes ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        database.execSQL("UPDATE notes SET updatedAt = timestamp")
        database.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER")
        database.execSQL("ALTER TABLE notes ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")
        database.execSQL("CREATE TABLE IF NOT EXISTS note_drafts (userId TEXT NOT NULL, noteId TEXT NOT NULL, payload TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(userId, noteId))")
    }
}
