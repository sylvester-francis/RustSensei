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

    // Path Progress
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPathProgress(progress: PathProgress)

    @Query("SELECT * FROM path_progress WHERE pathId = :pathId")
    suspend fun getPathProgress(pathId: String): List<PathProgress>

    @Query("SELECT * FROM path_progress WHERE pathId = :pathId")
    fun observePathProgress(pathId: String): Flow<List<PathProgress>>

    @Query("SELECT * FROM path_progress")
    fun observeAllPathProgress(): Flow<List<PathProgress>>

    // Quiz Results
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizResult(result: QuizResult)

    @Query("SELECT * FROM quiz_results WHERE quizId = :quizId ORDER BY score DESC LIMIT 1")
    suspend fun getBestQuizResult(quizId: String): QuizResult?

    @Query("SELECT * FROM quiz_results WHERE quizId = :quizId ORDER BY completedAt DESC")
    fun getQuizResults(quizId: String): Flow<List<QuizResult>>

    @Query("SELECT * FROM quiz_results ORDER BY completedAt DESC")
    fun getAllQuizResults(): Flow<List<QuizResult>>

    // User Notes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: UserNote)

    @Query("SELECT * FROM user_notes WHERE sectionId = :sectionId ORDER BY updatedAt DESC")
    suspend fun getNotesForSection(sectionId: String): List<UserNote>

    @Query("SELECT * FROM user_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<UserNote>>

    @Query("DELETE FROM user_notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: Long)

    @Query("SELECT * FROM user_notes WHERE content LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun searchNotes(query: String): List<UserNote>

    // Exercise progress queries for achievements
    @Query("SELECT COUNT(*) FROM exercise_progress WHERE category = :category AND status = 'completed'")
    suspend fun getCompletedExerciseCountByCategory(category: String): Int

    @Query("SELECT COUNT(*) FROM exercise_progress WHERE status = 'completed'")
    suspend fun getCompletedExercisesCountSync(): Int

    @Query("SELECT COUNT(*) FROM book_progress WHERE isCompleted = 1")
    suspend fun getCompletedSectionsCountSync(): Int

    // Study streak helper
    @Query("SELECT * FROM learning_stats WHERE date = :date")
    suspend fun getStatsForDate(date: String): LearningStats?

    // Daily Challenge Results
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyChallengeResult(result: DailyChallengeResult)

    @Query("SELECT * FROM daily_challenge_results WHERE date = :date LIMIT 1")
    suspend fun getDailyChallengeResult(date: String): DailyChallengeResult?

    @Query("SELECT COUNT(*) FROM daily_challenge_results")
    suspend fun getDailyChallengeCompletedCount(): Int

    // Refactoring Results
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefactoringResult(result: RefactoringResult)

    @Query("SELECT * FROM refactoring_results WHERE challengeId = :challengeId ORDER BY score DESC LIMIT 1")
    suspend fun getBestRefactoringResult(challengeId: String): RefactoringResult?

    @Query("SELECT * FROM refactoring_results ORDER BY completedAt DESC")
    fun getAllRefactoringResults(): Flow<List<RefactoringResult>>

    // Project Progress
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProjectProgress(progress: ProjectProgress)

    @Query("SELECT * FROM project_progress WHERE projectId = :projectId")
    suspend fun getProjectProgress(projectId: String): List<ProjectProgress>

    @Query("SELECT * FROM project_progress WHERE projectId = :projectId")
    fun observeProjectProgress(projectId: String): Flow<List<ProjectProgress>>
}
