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
        LearningStats::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun progressDao(): ProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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
                    .addMigrations(MIGRATION_1_2)
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
