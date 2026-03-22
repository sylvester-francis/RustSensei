package com.sylvester.rustsensei.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ChatMessage::class,
        Conversation::class,
        BookProgress::class,
        ExerciseProgress::class,
        LearningStats::class,
        FlashCard::class,
        PathProgress::class,
        QuizResult::class,
        UserNote::class,
        DailyChallengeResult::class,
        RefactoringResult::class,
        ProjectProgress::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun progressDao(): ProgressDao
    abstract fun flashCardDao(): FlashCardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `project_progress` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `projectId` TEXT NOT NULL,
                        `stepId` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `userCode` TEXT NOT NULL DEFAULT '',
                        `completedAt` INTEGER
                    )
                """)
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_project_progress_projectId`
                    ON `project_progress` (`projectId`)
                """)
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `daily_challenge_results` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `exerciseId` TEXT NOT NULL,
                        `completedAt` INTEGER NOT NULL DEFAULT 0,
                        `timeTakenSeconds` INTEGER NOT NULL DEFAULT 0,
                        `score` INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_challenge_results_date`
                    ON `daily_challenge_results` (`date`)
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `refactoring_results` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `challengeId` TEXT NOT NULL,
                        `userCode` TEXT NOT NULL DEFAULT '',
                        `score` INTEGER NOT NULL DEFAULT 0,
                        `feedback` TEXT NOT NULL DEFAULT '',
                        `completedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sectionId` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `quiz_results` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `quizId` TEXT NOT NULL,
                        `score` INTEGER NOT NULL,
                        `totalQuestions` INTEGER NOT NULL,
                        `completedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `path_progress` (
                        `stepId` TEXT NOT NULL PRIMARY KEY,
                        `pathId` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `completedAt` INTEGER
                    )
                """)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `flash_cards` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `front` TEXT NOT NULL,
                        `back` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `sourceId` TEXT NOT NULL DEFAULT '',
                        `nextReviewAt` INTEGER NOT NULL DEFAULT 0,
                        `interval` INTEGER NOT NULL DEFAULT 0,
                        `easeFactor` REAL NOT NULL DEFAULT 2.5,
                        `repetitions` INTEGER NOT NULL DEFAULT 0,
                        `lastReviewedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `book_progress` (
                        `sectionId` TEXT NOT NULL PRIMARY KEY,
                        `chapterId` TEXT NOT NULL,
                        `readPercent` REAL NOT NULL DEFAULT 0,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `timeSpentSeconds` INTEGER NOT NULL DEFAULT 0,
                        `bookmarked` INTEGER NOT NULL DEFAULT 0,
                        `lastReadAt` INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `exercise_progress` (
                        `exerciseId` TEXT NOT NULL PRIMARY KEY,
                        `category` TEXT NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'not_started',
                        `userCode` TEXT NOT NULL DEFAULT '',
                        `hintsViewed` INTEGER NOT NULL DEFAULT 0,
                        `attempts` INTEGER NOT NULL DEFAULT 0,
                        `completedAt` INTEGER,
                        `lastAttemptAt` INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `learning_stats` (
                        `date` TEXT NOT NULL PRIMARY KEY,
                        `sectionsRead` INTEGER NOT NULL DEFAULT 0,
                        `exercisesCompleted` INTEGER NOT NULL DEFAULT 0,
                        `studyTimeSeconds` INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rustsensei_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    // WARNING: Do NOT use fallbackToDestructiveMigration() here — it wipes
                    // all user data (chat history, progress, stats) on ANY missing migration.
                    // Only allow destructive migration on version downgrade (e.g. debug builds).
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
