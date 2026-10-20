package com.katchy.focuslive.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntegrityMigrationTest {
    @Test
    fun version13MigratesAndRoomOpensWithoutDiscardingData() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "integrity-migration-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        // Create the full, historical v13 schema, not a v14 DB with downgraded user_version.
        SQLiteDatabase.openOrCreateDatabase(path, null).use { legacy ->
            val sql = instrumentation.context.assets.open("brish-v13.sql").bufferedReader().use { it.readText() }
            sql.split(';').filter { it.isNotBlank() }.forEach { legacy.execSQL(it.trim()) }
            legacy.execSQL("INSERT INTO tasks VALUES('agenda','Read','General',NULL,NULL,0,100,200,1,NULL,NULL,'NONE','[]',0,'[]','MEDIUM','','[]','alice',0)")
            legacy.execSQL("INSERT INTO tasks VALUES('delivery','Essay','General','course','essay',0,100,200,1,NULL,NULL,'NONE','[]',0,'[]','MEDIUM','','[]','alice',1)")
            legacy.execSQL("INSERT INTO notes VALUES('note','Title','Offline body','#FFF9C4',0,'LOW',1234,NULL,'course','alice','[]',0)")
            legacy.execSQL("INSERT INTO habits VALUES('habit','Walk','walk',3,'[100,200]',NULL,'[1,2,3,4,5,6,7]',100,'alice',0)")
            legacy.execSQL("INSERT INTO transactions VALUES('finance','Legacy purchase',12.5,'EXPENSE','General',0,'NONE',100,'alice',0)")
            legacy.execSQL("INSERT INTO pending_deletes VALUES('removed','note','alice',777)")
            // Also cover extra indexes left behind by upgrade paths 6->7 and 12->13.
            legacy.execSQL("CREATE INDEX index_courses_spaceId ON courses(spaceId)")
            legacy.execSQL("CREATE INDEX index_tasks_userId ON tasks(userId)")
            legacy.execSQL("CREATE INDEX index_notes_userId ON notes(userId)")
            legacy.execSQL("CREATE INDEX index_habits_userId ON habits(userId)")
            legacy.execSQL("CREATE INDEX index_transactions_userId ON transactions(userId)")
            legacy.version = 13
        }
        val room = Room.databaseBuilder(context, BrishDatabase::class.java, name)
            .addMigrations(MIGRATION_13_14)
            .build()
        try {
            // writableDatabase triggers the migration AND Room's schema validation.
            val db = room.openHelper.writableDatabase
            assertEquals(14, db.version)
            db.query("SELECT kind,isSynced,revision FROM tasks WHERE id='agenda'").use {
                assertTrue(it.moveToFirst())
                assertEquals("AGENDA", it.getString(0))
                assertEquals(0, it.getInt(1))
                assertEquals(0L, it.getLong(2))
            }
            db.query("SELECT kind FROM tasks WHERE id='delivery'").use {
                assertTrue(it.moveToFirst()); assertEquals("DELIVERY", it.getString(0))
            }
            val note = room.noteDao().getNoteById("note", "alice")!!
            assertEquals("Offline body", note.content)
            assertEquals(1234L, note.updatedAt)
            assertNull(note.deletedAt)
            assertFalse(note.isSynced)
            assertNull(room.noteDao().getNoteById("note", "bob"))
            // Global primary keys must never let another account's refresh replace pending edits.
            room.noteDao().refreshNotesTransaction("bob", listOf(note.copy(userId = "bob", content = "collision", revision = 999)), emptySet())
            assertEquals("Offline body", room.noteDao().getNoteById("note", "alice")!!.content)
            assertNull(room.noteDao().getNoteById("note", "bob"))
            val task = room.taskDao().getTaskById("agenda", "alice")!!
            room.taskDao().refreshTasksTransaction("bob", listOf(task.copy(userId = "bob", revision = 999)), emptySet())
            assertNotNull(room.taskDao().getTaskById("agenda", "alice"))
            assertNull(room.taskDao().getTaskById("agenda", "bob"))
            val category = com.katchy.focuslive.data.model.NoteCategory(id = "folder", userId = "alice", isSynced = false)
            room.noteCategoryDao().insertCategory(category)
            room.noteCategoryDao().refreshCategoriesTransaction("bob", listOf(category.copy(userId = "bob", revision = 999)), emptySet())
            assertEquals("alice", room.noteCategoryDao().getOwner("folder"))
            val course = com.katchy.focuslive.data.model.Course(id = "course", userId = "alice", isSynced = false)
            room.courseDao().insertCourse(course)
            room.courseDao().refreshCoursesTransaction("bob", listOf(course.copy(userId = "bob", revision = 999)), emptySet())
            assertEquals("alice", room.courseDao().getOwner("course"))
            val space = com.katchy.focuslive.data.model.LearningSpace(id = "space", userId = "alice", isSynced = false)
            room.learningSpaceDao().insertSpace(space)
            room.learningSpaceDao().refreshSpacesTransaction("bob", listOf(space.copy(userId = "bob", revision = 999)), emptySet())
            assertEquals("alice", room.learningSpaceDao().getOwner("space"))
            db.query("SELECT currentStreak,completedDates FROM habits WHERE id='habit'").use {
                assertTrue(it.moveToFirst()); assertEquals(3, it.getInt(0)); assertEquals("[100,200]", it.getString(1))
            }
            db.query("SELECT amount FROM transactions WHERE id='finance'").use {
                assertTrue(it.moveToFirst()); assertEquals(12.5, it.getDouble(0), 0.001)
            }
            assertEquals(1, room.pendingDeleteDao().getPendingDeletes("alice", "note").size)
            val cleanTrash = note.copy(id = "remote-deleted-trash", deletedAt = 2000, isSynced = true)
            val protectedDraft = note.copy(id = "protected-draft", isSynced = true)
            room.noteDao().insertNote(cleanTrash)
            room.noteDao().insertNote(protectedDraft)
            room.noteDraftDao().saveDraft(com.katchy.focuslive.data.model.NoteDraft(
                "alice", protectedDraft.id, com.google.gson.Gson().toJson(protectedDraft), 2000
            ))
            room.noteDao().refreshNotesTransaction("alice", emptyList(), emptySet())
            assertNull(room.noteDao().getNoteById(cleanTrash.id, "alice"))
            assertNotNull(room.noteDao().getNoteById(protectedDraft.id, "alice"))
            assertNotNull(room.noteDao().getNoteById(note.id, "alice")) // Dirty offline note survives.
            room.noteDraftDao().saveDraft(com.katchy.focuslive.data.model.NoteDraft("alice", "new", "{\"content\":\"draft\"}", 2000))
        } finally {
            room.close()
        }
        val reopened = Room.databaseBuilder(context, BrishDatabase::class.java, name).build()
        try {
            assertNotNull(reopened.noteDraftDao().getDraft("alice", "new"))
            assertNull(reopened.noteDraftDao().getDraft("bob", "new"))
        } finally {
            reopened.close()
            context.deleteDatabase(name)
        }
    }
}
