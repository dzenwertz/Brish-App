package com.katchy.focuslive.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.katchy.focuslive.data.repository.AppPreferencesRepository
import com.katchy.focuslive.data.repository.AuthRepository
import com.katchy.focuslive.data.repository.AuthRepositoryImpl

import com.katchy.focuslive.data.repository.FinanceRepository
import com.katchy.focuslive.data.repository.FinanceRepositoryImpl
import com.katchy.focuslive.data.repository.GoalRepository
import com.katchy.focuslive.data.repository.GoalRepositoryImpl
import com.katchy.focuslive.data.repository.HabitRepository
import com.katchy.focuslive.data.repository.HabitRepositoryImpl
import com.katchy.focuslive.data.repository.NoteRepository
import com.katchy.focuslive.data.repository.NoteRepositoryImpl
import com.katchy.focuslive.data.repository.TaskRepository
import com.katchy.focuslive.data.repository.TaskRepositoryImpl
import com.katchy.focuslive.data.repository.ThemeRepository
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.katchy.focuslive.data.local.dao.TaskDao
import com.katchy.focuslive.data.local.dao.FinanceDao
import com.katchy.focuslive.data.local.dao.NoteDao
import com.katchy.focuslive.data.local.dao.HabitDao
import com.katchy.focuslive.data.local.dao.CourseDao
import com.katchy.focuslive.data.repository.CourseRepository
import com.katchy.focuslive.data.repository.CourseRepositoryImpl
import com.katchy.focuslive.data.local.BrishDatabase

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                    .setSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
            )
            .build()
        firestore.firestoreSettings = settings
        return firestore
    }

    @Provides
    @Singleton
    fun provideBrishDatabase(
        @ApplicationContext context: Context
    ): BrishDatabase {
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add daysOfWeek column with default value (JSON array of 1..7)
                database.execSQL("ALTER TABLE habits ADD COLUMN daysOfWeek TEXT NOT NULL DEFAULT '[1,2,3,4,5,6,7]'")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Create NoteCategory table
                database.execSQL("CREATE TABLE IF NOT EXISTS `note_categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `isDefault` INTEGER NOT NULL, PRIMARY KEY(`id`))")

                // Add categoryId to Notes
                database.execSQL("ALTER TABLE notes ADD COLUMN categoryId TEXT")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add userId column to all tables for multi-account support
                database.execSQL("ALTER TABLE tasks ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE notes ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE habits ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE transactions ADD COLUMN userId TEXT NOT NULL DEFAULT ''")

                // Create indexes for faster queries by userId
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_userId ON tasks(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notes_userId ON notes(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_habits_userId ON habits(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_userId ON transactions(userId)")
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add userId and isSynced columns to note_categories
                database.execSQL("ALTER TABLE note_categories ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE note_categories ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 1")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_note_categories_userId ON note_categories(userId)")
            }
        }

        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes ADD COLUMN flashcards TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Tombstones for pending deletes
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_deletes` (" +
                        "`itemId` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`userId` TEXT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`itemId`, `type`, `userId`))"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_pending_deletes_userId_type ON pending_deletes(userId, type)")
            }
        }

        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `chat_sessions` (" +
                        "`id` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "`previewText` TEXT NOT NULL, " +
                        "`messageCount` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `chat_messages` (" +
                        "`id` TEXT NOT NULL, " +
                        "`sessionId` TEXT NOT NULL, " +
                        "`sender` TEXT NOT NULL, " +
                        "`text` TEXT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, " +
                        "`actionPayload` TEXT, " +
                        "`actionsExecuted` INTEGER NOT NULL, " +
                        "`actionsDismissed` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_sessionId ON chat_messages(sessionId)")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_ai_memory` (" +
                        "`id` TEXT NOT NULL, " +
                        "`summaryBio` TEXT NOT NULL, " +
                        "`keyFacts` TEXT NOT NULL, " +
                        "`preferences` TEXT NOT NULL, " +
                        "`lastUpdated` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN courseId TEXT")
                database.execSQL("ALTER TABLE tasks ADD COLUMN deliverableType TEXT")
                database.execSQL("ALTER TABLE notes ADD COLUMN courseId TEXT")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `courses` (" +
                        "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `color` INTEGER NOT NULL, " +
                        "`semester` TEXT NOT NULL, `userId` TEXT NOT NULL, `isSynced` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_courses_userId ON courses(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_courseId ON tasks(courseId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notes_courseId ON notes(courseId)")
            }
        }

        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN hasDueDate INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE courses ADD COLUMN spaceId TEXT NOT NULL DEFAULT ''")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `learning_spaces` (" +
                        "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `userId` TEXT NOT NULL, " +
                        "`isArchived` INTEGER NOT NULL DEFAULT 0, `sortOrder` INTEGER NOT NULL DEFAULT 0, " +
                        "`createdAt` INTEGER NOT NULL DEFAULT 0, `isSynced` INTEGER NOT NULL DEFAULT 1, " +
                        "PRIMARY KEY(`id`))"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_learning_spaces_userId ON learning_spaces(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_courses_spaceId ON courses(spaceId)")
            }
        }

        return androidx.room.Room.databaseBuilder(
            context,
            BrishDatabase::class.java,
            "brish_database"
        )
        .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
        .build()
    }

    @Provides
    fun provideLearningSpaceDao(db: BrishDatabase): com.katchy.focuslive.data.local.dao.LearningSpaceDao = db.learningSpaceDao()

    @Provides
    fun provideTaskDao(db: BrishDatabase) = db.taskDao()

    @Provides
    fun provideFinanceDao(db: BrishDatabase) = db.financeDao()

    @Provides
    fun provideNoteDao(db: BrishDatabase) = db.noteDao()

    @Provides
    fun provideNoteCategoryDao(db: BrishDatabase) = db.noteCategoryDao()

    @Provides
    fun provideHabitDao(db: BrishDatabase) = db.habitDao()

    @Provides
    fun provideCourseDao(db: BrishDatabase): CourseDao = db.courseDao()

    @Provides
    fun providePendingDeleteDao(db: BrishDatabase) = db.pendingDeleteDao()

    @Provides
    fun provideChatDao(db: BrishDatabase): com.katchy.focuslive.data.local.dao.ChatDao = db.chatDao()

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        database: BrishDatabase,
        userStatsRepository: com.katchy.focuslive.data.repository.UserStatsRepository,
        mascotRepository: com.katchy.focuslive.data.repository.MascotRepository,
        @ApplicationContext context: Context
    ): AuthRepository = AuthRepositoryImpl(
        firebaseAuth,
        firestore,
        database,
        userStatsRepository,
        mascotRepository,
        context
    )

    @Provides
    @Singleton
    fun provideThemeRepository(
        @ApplicationContext context: Context
    ): ThemeRepository = ThemeRepository(context)

    @Provides
    @Singleton
    fun provideTaskRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        taskDao: TaskDao,
        pendingDeleteDao: com.katchy.focuslive.data.local.dao.PendingDeleteDao,
        @ApplicationContext context: Context,
        notificationSchedulerProvider: javax.inject.Provider<com.katchy.focuslive.scheduler.NotificationScheduler>,
        userStatsRepository: com.katchy.focuslive.data.repository.UserStatsRepository,
        mascotRepository: com.katchy.focuslive.data.repository.MascotRepository,
        timerManager: com.katchy.focuslive.data.manager.TimerManager
    ): TaskRepository = TaskRepositoryImpl(
        firestore, auth, taskDao, pendingDeleteDao, context,
        notificationSchedulerProvider, userStatsRepository, mascotRepository, timerManager
    )

    @Provides
    @Singleton
    fun provideGoalRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): GoalRepository = GoalRepositoryImpl(firestore, auth)

    @Provides
    @Singleton
    fun provideFinanceRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        financeDao: FinanceDao,
        pendingDeleteDao: com.katchy.focuslive.data.local.dao.PendingDeleteDao,
        @ApplicationContext context: Context
    ): FinanceRepository {
        return com.katchy.focuslive.data.repository.FinanceRepositoryImpl(firestore, auth, financeDao, pendingDeleteDao, context)
    }

    @Provides
    @Singleton
    fun provideNoteRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        noteDao: NoteDao,
        noteCategoryDao: com.katchy.focuslive.data.local.dao.NoteCategoryDao,
        pendingDeleteDao: com.katchy.focuslive.data.local.dao.PendingDeleteDao,
        @ApplicationContext context: Context
    ): NoteRepository = NoteRepositoryImpl(firestore, auth, noteDao, noteCategoryDao, pendingDeleteDao, context)

    @Provides
    @Singleton
    fun provideCourseRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        courseDao: CourseDao,
        pendingDeleteDao: com.katchy.focuslive.data.local.dao.PendingDeleteDao,
        @ApplicationContext context: Context
    ): CourseRepository = CourseRepositoryImpl(firestore, auth, courseDao, pendingDeleteDao, context)

    @Provides
    @Singleton
    fun provideLearningSpaceRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        spaceDao: com.katchy.focuslive.data.local.dao.LearningSpaceDao,
        pendingDeleteDao: com.katchy.focuslive.data.local.dao.PendingDeleteDao,
        @ApplicationContext context: Context
    ): com.katchy.focuslive.data.repository.LearningSpaceRepository =
        com.katchy.focuslive.data.repository.LearningSpaceRepositoryImpl(firestore, auth, spaceDao, pendingDeleteDao, context)

    @Provides
    @Singleton
    fun provideAppPreferencesRepository(
        @ApplicationContext context: Context
    ): AppPreferencesRepository = AppPreferencesRepository(context)



    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): com.katchy.focuslive.ui.util.NetworkMonitor = com.katchy.focuslive.ui.util.NetworkMonitor(context)



    @Provides
    @Singleton
    fun provideHabitRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        habitDao: HabitDao,
        pendingDeleteDao: com.katchy.focuslive.data.local.dao.PendingDeleteDao,
        @ApplicationContext context: Context
    ): HabitRepository = HabitRepositoryImpl(firestore, auth, habitDao, pendingDeleteDao, context)

    @Provides
    @Singleton
    fun provideTimerManager(
        appPreferencesRepository: AppPreferencesRepository
    ): com.katchy.focuslive.data.manager.TimerManager = com.katchy.focuslive.data.manager.TimerManager(appPreferencesRepository)

    @Provides
    @Singleton
    fun provideOpenCodeApi(): com.katchy.focuslive.data.api.OpenCodeApi {
        val userAgentInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "Brish/1.2.22 (Android)")
                .build()
            chain.proceed(request)
        }

        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        return retrofit2.Retrofit.Builder()
            .baseUrl("https://opencode.ai/zen/v1/")
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.katchy.focuslive.data.api.OpenCodeApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenCodeRepository(
        openCodeApi: com.katchy.focuslive.data.api.OpenCodeApi,
        credentialsStore: com.katchy.focuslive.data.security.OpenCodeCredentialsStore
    ): com.katchy.focuslive.data.repository.OpenCodeRepository {
        return com.katchy.focuslive.data.repository.OpenCodeRepositoryImpl(openCodeApi, credentialsStore)
    }

    @Provides
    @Singleton
    fun provideWidgetManager(
        @ApplicationContext context: Context
    ): com.katchy.focuslive.data.manager.WidgetManager = com.katchy.focuslive.data.manager.WidgetManagerImpl(context)

    @Provides
    @Singleton
    fun provideBillingRepository(
        @ApplicationContext context: Context,
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        userStatsRepository: com.katchy.focuslive.data.repository.UserStatsRepository
    ): com.katchy.focuslive.data.repository.BillingRepository {
        return com.katchy.focuslive.data.repository.BillingRepositoryImpl(context, firebaseAuth, firestore, userStatsRepository)
    }
}
