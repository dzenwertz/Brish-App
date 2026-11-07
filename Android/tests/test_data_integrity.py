"""Run with Python stdlib unittest; executes production migration/DAO SQL in SQLite.

Useful when an Android SDK/emulator is unavailable. Room/Kotlin compilation remains
a separate required Android build check.
"""
import json
from pathlib import Path
import re
import sqlite3
import unittest

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/java/com/katchy/focuslive"


def query(dao, method):
    source = (ROOT / "data/local/dao" / f"{dao}.kt").read_text(encoding="utf-8")
    pattern = r'@Query\("([^"]*)"\)\s+(?:suspend\s+)?fun\s+' + method + r'\('
    return re.search(pattern, source).group(1)


class IntegritySqlTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        self.db.executescript("""
            CREATE TABLE tasks(id TEXT PRIMARY KEY, courseId TEXT, deliverableType TEXT,
                userId TEXT, timestamp INTEGER, dueDate INTEGER, isSynced INTEGER);
            CREATE TABLE notes(id TEXT PRIMARY KEY, courseId TEXT, categoryId TEXT,
                userId TEXT, timestamp INTEGER, isSynced INTEGER);
            CREATE TABLE note_categories(id TEXT PRIMARY KEY, userId TEXT, isSynced INTEGER);
            CREATE TABLE courses(id TEXT PRIMARY KEY, name TEXT, userId TEXT,
                spaceId TEXT, isSynced INTEGER);
            CREATE TABLE learning_spaces(id TEXT PRIMARY KEY, userId TEXT,
                isArchived INTEGER, isSynced INTEGER);
            CREATE TABLE habits(id TEXT PRIMARY KEY, legacy TEXT);
            CREATE TABLE transactions(id TEXT PRIMARY KEY, legacy TEXT);
            CREATE TABLE pending_deletes(itemId TEXT, type TEXT, userId TEXT,
                timestamp INTEGER, PRIMARY KEY(itemId,type,userId));
            INSERT INTO tasks VALUES('agenda',NULL,NULL,'alice',100,200,0);
            INSERT INTO tasks VALUES('delivery','course',NULL,'alice',100,200,1);
            INSERT INTO tasks VALUES('legacy-delivery',NULL,'essay','alice',100,200,1);
            INSERT INTO tasks VALUES('blank-course','',NULL,'alice',100,200,1);
            INSERT INTO notes VALUES('note','course','folder','alice',1234,1);
            INSERT INTO notes VALUES('bob-note','course','folder','bob',5678,1);
            INSERT INTO habits VALUES('habit','keep completion history');
            INSERT INTO transactions VALUES('transaction','keep balance history');
        """)
        source = (ROOT / "data/local/IntegrityMigration.kt").read_text(encoding="utf-8")
        for sql in re.findall(r'database\.execSQL\("([^"]*)"\)', source):
            self.db.execute(sql)
        self.db.commit()

    def tearDown(self):
        self.db.close()

    def test_migration_classifies_tasks_without_changing_legacy_rows(self):
        kinds = dict(self.db.execute("SELECT id,kind FROM tasks"))
        self.assertEqual(kinds, {"agenda": "AGENDA", "delivery": "DELIVERY",
                                "legacy-delivery": "DELIVERY", "blank-course": "AGENDA"})
        self.assertEqual(self.db.execute("SELECT updatedAt,deletedAt,revision FROM notes WHERE id='note'").fetchone(),
                         (1234, None, 0))
        self.assertEqual(self.db.execute("SELECT * FROM habits").fetchall(), [("habit", "keep completion history")])
        self.assertEqual(self.db.execute("SELECT * FROM transactions").fetchall(), [("transaction", "keep balance history")])

    def test_agenda_and_delivery_queries_separate_legacy_links_and_scope_owner(self):
        # Legacy remote tasks can still lack a stored delivery kind.
        self.db.execute("UPDATE tasks SET kind='AGENDA' WHERE id IN ('delivery','legacy-delivery')")
        self.db.execute("INSERT INTO tasks VALUES('bob-task',NULL,NULL,'bob',100,200,1,'AGENDA',0)")
        self.db.execute("INSERT INTO tasks VALUES('explicit-delivery',NULL,NULL,'alice',100,200,1,'DELIVERY',0)")
        agenda = self.db.execute(query("TaskDao", "getAgendaTasks"), {"userId": "alice"}).fetchall()
        deliveries = self.db.execute(query("TaskDao", "getDeliveries"), {"userId": "alice"}).fetchall()
        self.assertEqual({row[0] for row in agenda}, {"agenda", "blank-course"})
        self.assertEqual({row[0] for row in deliveries}, {"delivery", "legacy-delivery", "explicit-delivery"})
        self.assertEqual({row[0] for row in self.db.execute(query("TaskDao", "getAgendaTasks"), {"userId": "bob"})}, {"bob-task"})

    def test_upload_acknowledges_only_uploaded_revision_and_owner(self):
        self.db.execute("UPDATE notes SET revision=2,isSynced=0 WHERE id='note'")
        sql = query("NoteDao", "acknowledgeRevision")
        self.assertEqual(self.db.execute(sql, {"noteId": "note", "userId": "alice", "revision": 1}).rowcount, 0)
        self.assertEqual(self.db.execute(sql, {"noteId": "note", "userId": "bob", "revision": 2}).rowcount, 0)
        self.assertEqual(self.db.execute("SELECT isSynced FROM notes WHERE id='note'").fetchone(), (0,))
        self.assertEqual(self.db.execute(sql, {"noteId": "note", "userId": "alice", "revision": 2}).rowcount, 1)
        task_sql = query("TaskDao", "acknowledgeRevision")
        self.db.execute("UPDATE tasks SET revision=9,isSynced=0 WHERE id='agenda'")
        self.assertEqual(self.db.execute(task_sql, {"taskId": "agenda", "userId": "alice", "revision": 8}).rowcount, 0)

    def test_trash_persists_and_does_not_leak_between_accounts(self):
        self.db.execute("UPDATE notes SET deletedAt=2000,isSynced=0,revision=1 WHERE id='note'")
        self.db.commit()
        self.assertEqual(self.db.execute(query("NoteDao", "getAllNotes"), {"userId": "alice"}).fetchall(), [])
        trash_sql = query("NoteDao", "getTrashNotes")
        self.assertEqual(len(self.db.execute(trash_sql, {"userId": "alice"}).fetchall()), 1)
        self.assertEqual(self.db.execute(trash_sql, {"userId": "bob"}).fetchall(), [])
        # A persisted trash note remains a course member.
        self.assertEqual(self.db.execute(query("CourseDao", "countCourseContents"),
                                        {"courseId": "course", "userId": "alice"}).fetchone(), (2,))

    def test_folder_detach_marks_active_and_trashed_notes_dirty_atomically(self):
        self.db.execute("UPDATE notes SET deletedAt=2000 WHERE id='note'")
        self.db.commit()
        with self.db:
            self.db.execute(query("NoteCategoryDao", "detachNotesFromCategory"),
                            {"categoryId": "folder", "userId": "alice", "updatedAt": 3000})
        self.assertEqual(self.db.execute("SELECT categoryId,isSynced,revision,updatedAt,deletedAt FROM notes WHERE id='note'").fetchone(),
                         (None, 0, 1, 3000, 2000))
        self.assertEqual(self.db.execute("SELECT categoryId,isSynced,revision FROM notes WHERE id='bob-note'").fetchone(),
                         ("folder", 1, 0))

    def test_new_drafts_are_scoped_durable_and_hidden_while_note_is_trashed(self):
        payload = json.dumps({"id": "new", "userId": "alice", "content": "unsaved text"})
        self.db.execute("INSERT INTO note_drafts VALUES('alice','new',?,123)", (payload,))
        self.db.execute("INSERT INTO note_drafts VALUES('alice','note',?,456)", (payload,))
        self.db.commit()
        sql = query("NoteDraftDao", "observeDrafts")
        self.assertEqual(len(self.db.execute(sql, {"userId": "alice"}).fetchall()), 2)
        self.assertEqual(self.db.execute(sql, {"userId": "bob"}).fetchall(), [])
        self.db.execute("UPDATE notes SET deletedAt=2000 WHERE id='note'")
        self.assertEqual([row[1] for row in self.db.execute(sql, {"userId": "alice"})], ["new"])

    def test_old_delete_ack_does_not_remove_newer_delete(self):
        self.db.execute("INSERT INTO pending_deletes VALUES('note','note','alice',222)")
        sql = query("PendingDeleteDao", "acknowledgeDelete")
        self.assertEqual(self.db.execute(sql, {"itemId": "note", "type": "note", "userId": "alice", "timestamp": 111}).rowcount, 0)
        self.assertEqual(self.db.execute(sql, {"itemId": "note", "type": "note", "userId": "alice", "timestamp": 222}).rowcount, 1)

    def test_server_absence_prunes_synced_notes_and_trash_but_preserves_drafts_dirty_and_other_owner(self):
        self.db.execute("UPDATE notes SET deletedAt=2000 WHERE id='note'")
        sql = query("NoteDao", "deleteSyncedNotesNotInSet").replace("(:validIds)", "()")
        self.db.execute("INSERT INTO note_drafts VALUES('alice','note','{}',2000)")
        self.assertEqual(self.db.execute(sql, {"userId": "alice"}).rowcount, 0)
        self.db.execute("DELETE FROM note_drafts")
        self.db.execute("UPDATE notes SET isSynced=0 WHERE id='note'")
        self.assertEqual(self.db.execute(sql, {"userId": "alice"}).rowcount, 0)
        self.db.execute("UPDATE notes SET isSynced=1 WHERE id='note'")
        self.assertEqual(self.db.execute(sql, {"userId": "alice"}).rowcount, 1)
        self.assertEqual(self.db.execute("SELECT userId FROM notes WHERE id='bob-note'").fetchone(), ("bob",))
        task_sql = query("TaskDao", "deleteSyncedTasksNotInSet").replace("(:validIds)", "()")
        self.db.execute(task_sql, {"userId": "alice"})
        self.assertEqual(self.db.execute("SELECT id FROM tasks WHERE userId='alice'").fetchall(), [("agenda",)])

    def test_server_absence_parent_pruning_preserves_references_and_draft_ids(self):
        self.db.execute("INSERT INTO courses VALUES('course','Course','alice','space',1,0)")
        self.db.execute("INSERT INTO courses VALUES('draft-course','Draft course','alice','space',1,0)")
        self.db.execute("INSERT INTO courses VALUES('empty-course','Empty course','alice','space',1,0)")
        self.db.execute("INSERT INTO learning_spaces VALUES('space','alice',0,1,0)")
        self.db.execute("INSERT INTO note_categories VALUES('folder','alice',1,0)")
        # Draft reference IDs join the authoritative server ID set in the transaction.
        course_sql = query("CourseDao", "deleteSyncedCoursesNotInSet").replace("(:validIds)", "('draft-course')")
        self.db.execute(course_sql, {"userId": "alice"})
        self.assertEqual(sorted(row[0] for row in self.db.execute("SELECT id FROM courses")), ["course", "draft-course"])
        category_sql = query("NoteCategoryDao", "deleteSyncedCategoriesNotInSet").replace("(:validIds)", "()")
        self.assertEqual(self.db.execute(category_sql, {"userId": "alice"}).rowcount, 0)
        space_sql = query("LearningSpaceDao", "deleteSyncedSpacesNotInSet").replace("(:validIds)", "()")
        self.assertEqual(self.db.execute(space_sql, {"userId": "alice"}).rowcount, 0)
        # Once children disappear, permanent parent deletion can propagate.
        self.db.execute("DELETE FROM tasks WHERE userId='alice'")
        self.db.execute("DELETE FROM notes WHERE userId='alice'")
        self.assertEqual(self.db.execute(category_sql, {"userId": "alice"}).rowcount, 1)
        self.assertEqual(self.db.execute(course_sql, {"userId": "alice"}).rowcount, 1)
        self.db.execute("DELETE FROM courses WHERE id='draft-course'")
        self.assertEqual(self.db.execute(space_sql, {"userId": "alice"}).rowcount, 1)

    def test_full_historical_schema_matches_exported_room15(self):
        android = ROOT.parents[6]
        fixture = android / "app/src/androidTest/assets/brish-v13.sql"
        schema_path = android / "app/schemas/com.katchy.focuslive.data.local.BrishDatabase/15.json"
        schema = json.loads(schema_path.read_text(encoding="utf-8"))["database"]
        actual = sqlite3.connect(":memory:")
        expected = sqlite3.connect(":memory:")
        try:
            actual.executescript(fixture.read_text(encoding="utf-8"))
            for table, column in [("courses", "spaceId"), ("tasks", "userId"),
                                  ("notes", "userId"), ("habits", "userId"), ("transactions", "userId")]:
                actual.execute(f"CREATE INDEX index_{table}_{column} ON {table}({column})")
            migration = (ROOT / "data/local/IntegrityMigration.kt").read_text(encoding="utf-8")
            for sql in re.findall(r'database\.execSQL\("([^"]*)"\)', migration):
                actual.execute(sql)
            for entity in schema["entities"]:
                table = entity["tableName"]
                expected.execute(entity["createSql"].replace("${TABLE_NAME}", table))
                for index in entity["indices"]:
                    expected.execute(index["createSql"].replace("${TABLE_NAME}", table))
                # Column order is immaterial to Room's TableInfo validation.
                actual_columns = {row[1]: row[2:] for row in actual.execute(f"PRAGMA table_info({table})")}
                expected_columns = {row[1]: row[2:] for row in expected.execute(f"PRAGMA table_info({table})")}
                self.assertEqual(actual_columns, expected_columns, table)
                actual_indexes = sorted(row[1:] for row in actual.execute(f"PRAGMA index_list({table})"))
                expected_indexes = sorted(row[1:] for row in expected.execute(f"PRAGMA index_list({table})"))
                self.assertEqual(actual_indexes, expected_indexes, table)
        finally:
            actual.close()
            expected.close()


if __name__ == "__main__":
    unittest.main()
