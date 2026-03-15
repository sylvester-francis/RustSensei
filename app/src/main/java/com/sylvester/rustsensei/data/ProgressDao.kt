package com.sylvester.rustsensei.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    // Book Progress
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookProgress(progress: BookProgress)

    @Query("SELECT * FROM book_progress WHERE sectionId = :sectionId")
    suspend fun getBookProgress(sectionId: String): BookProgress?

    @Query("SELECT * FROM book_progress WHERE chapterId = :chapterId")
    fun getChapterProgress(chapterId: String): Flow<List<BookProgress>>

    @Query("SELECT COUNT(*) FROM book_progress WHERE isCompleted = 1")
    fun getCompletedSectionsCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT chapterId) FROM book_progress WHERE isCompleted = 1")
    fun getCompletedChaptersCount(): Flow<Int>

    @Query("SELECT * FROM book_progress WHERE bookmarked = 1 ORDER BY lastReadAt DESC")
    fun getBookmarks(): Flow<List<BookProgress>>

    @Query("SELECT * FROM book_progress ORDER BY lastReadAt DESC LIMIT 1")
    suspend fun getLastReadSection(): BookProgress?

    // Exercise Progress
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExerciseProgress(progress: ExerciseProgress)

    @Query("SELECT * FROM exercise_progress WHERE exerciseId = :exerciseId")
    suspend fun getExerciseProgress(exerciseId: String): ExerciseProgress?

    @Query("SELECT * FROM exercise_progress WHERE category = :category")
    fun getExerciseProgressByCategory(category: String): Flow<List<ExerciseProgress>>

    @Query("SELECT COUNT(*) FROM exercise_progress WHERE status = 'completed'")
    fun getCompletedExercisesCount(): Flow<Int>

    @Query("SELECT * FROM exercise_progress WHERE status != 'completed' ORDER BY lastAttemptAt DESC LIMIT 1")
    suspend fun getLastIncompleteExercise(): ExerciseProgress?

    // Learning Stats
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLearningStats(stats: LearningStats)

    @Query("SELECT * FROM learning_stats WHERE date = :date")
    suspend fun getLearningStats(date: String): LearningStats?

    @Query("SELECT * FROM learning_stats ORDER BY date DESC LIMIT :days")
    fun getRecentStats(days: Int = 7): Flow<List<LearningStats>>

    @Query("SELECT SUM(studyTimeSeconds) FROM learning_stats")
    fun getTotalStudyTime(): Flow<Long?>
}
