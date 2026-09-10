package com.katchy.focuslive.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.katchy.focuslive.data.local.dao.FinanceDao
import com.katchy.focuslive.data.local.dao.HabitDao
import com.katchy.focuslive.data.local.dao.NoteDao
import com.katchy.focuslive.data.local.dao.PendingDeleteDao
import com.katchy.focuslive.data.local.dao.TaskDao
import com.katchy.focuslive.data.model.Habit
import com.katchy.focuslive.data.model.Note
import com.katchy.focuslive.data.model.PendingDelete
import com.katchy.focuslive.data.model.Task
import com.katchy.focuslive.data.model.Transaction
import com.katchy.focuslive.data.model.Course

@Database(
    entities = [
        Task::class, Transaction::class, Note::class, Habit::class,
        com.katchy.focuslive.data.model.NoteCategory::class, PendingDelete::class,
        com.katchy.focuslive.ui.chat.ChatMessage::class,
        com.katchy.focuslive.ui.chat.ChatSession::class,
        com.katchy.focuslive.ui.chat.UserAiMemory::class,
        Course::class,
        com.katchy.focuslive.data.model.LearningSpace::class
    ],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BrishDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun financeDao(): FinanceDao
    abstract fun noteDao(): NoteDao
    abstract fun habitDao(): HabitDao
    abstract fun noteCategoryDao(): com.katchy.focuslive.data.local.dao.NoteCategoryDao
    abstract fun pendingDeleteDao(): PendingDeleteDao
    abstract fun chatDao(): com.katchy.focuslive.data.local.dao.ChatDao
    abstract fun courseDao(): com.katchy.focuslive.data.local.dao.CourseDao
    abstract fun learningSpaceDao(): com.katchy.focuslive.data.local.dao.LearningSpaceDao
}
