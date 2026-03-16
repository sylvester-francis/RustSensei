package com.sylvester.rustsensei.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProgressRepository(private val progressDao: ProgressDao) {

    // P0 Fix #4: ThreadLocal prevents concurrent access to SimpleDateFormat's mutable Calendar
    private val dateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    // Bug 3: ThreadLocal.withInitial guarantees a non-null value, so !! is safe here.
    // The previous fallback was unnecessary and masked the intent.
    private fun todayString(): String = dateFormat.get()!!.format(Date())

    // Book Progress
    suspend fun updateReadProgress(sectionId: String, chapterId: String, readPercent: Float) {
        val existing = progressDao.getBookProgress(sectionId)
        val progress = existing?.copy(
            readPercent = readPercent,
            lastReadAt = System.currentTimeMillis()
        ) ?: BookProgress(
            sectionId = sectionId,
            chapterId = chapterId,
            readPercent = readPercent
        )
        progressDao.upsertBookProgress(progress)
    }

    suspend fun markSectionComplete(sectionId: String, chapterId: String) {
        val existing = progressDao.getBookProgress(sectionId)
        val progress = existing?.copy(
            readPercent = 1f,
            isCompleted = true,
            lastReadAt = System.currentTimeMillis()
        ) ?: BookProgress(
            sectionId = sectionId,
            chapterId = chapterId,
            readPercent = 1f,
            isCompleted = true
        )
        progressDao.upsertBookProgress(progress)
        incrementSectionsRead()
    }

    suspend fun toggleBookmark(sectionId: String, chapterId: String) {
        val existing = progressDao.getBookProgress(sectionId)
        val progress = existing?.copy(
            bookmarked = !existing.bookmarked
        ) ?: BookProgress(
            sectionId = sectionId,
            chapterId = chapterId,
            bookmarked = true
        )
        progressDao.upsertBookProgress(progress)
    }

    suspend fun addReadTime(sectionId: String, chapterId: String, seconds: Long) {
        val existing = progressDao.getBookProgress(sectionId)
        val progress = existing?.copy(
            timeSpentSeconds = existing.timeSpentSeconds + seconds,
            lastReadAt = System.currentTimeMillis()
        ) ?: BookProgress(
            sectionId = sectionId,
            chapterId = chapterId,
            timeSpentSeconds = seconds
        )
        progressDao.upsertBookProgress(progress)
        addStudyTime(seconds)
    }

    fun getChapterProgress(chapterId: String): Flow<List<BookProgress>> =
        progressDao.getChapterProgress(chapterId)

    fun getCompletedSectionsCount(): Flow<Int> = progressDao.getCompletedSectionsCount()
    fun getCompletedChaptersCount(): Flow<Int> = progressDao.getCompletedChaptersCount()
    fun getBookmarks(): Flow<List<BookProgress>> = progressDao.getBookmarks()
    suspend fun getLastReadSection(): BookProgress? = progressDao.getLastReadSection()

    // Exercise Progress
    suspend fun updateExerciseCode(exerciseId: String, category: String, code: String) {
        val existing = progressDao.getExerciseProgress(exerciseId)
        val progress = existing?.copy(
            userCode = code,
            status = if (existing.status == "not_started") "in_progress" else existing.status,
            lastAttemptAt = System.currentTimeMillis()
        ) ?: ExerciseProgress(
            exerciseId = exerciseId,
            category = category,
            userCode = code,
            status = "in_progress"
        )
        progressDao.upsertExerciseProgress(progress)
    }

    suspend fun recordAttempt(exerciseId: String, category: String) {
        val existing = progressDao.getExerciseProgress(exerciseId)
        val progress = existing?.copy(
            attempts = existing.attempts + 1,
            lastAttemptAt = System.currentTimeMillis()
        ) ?: ExerciseProgress(
            exerciseId = exerciseId,
            category = category,
            attempts = 1,
            status = "in_progress"
        )
        progressDao.upsertExerciseProgress(progress)
    }

    suspend fun markExerciseComplete(exerciseId: String, category: String) {
        val existing = progressDao.getExerciseProgress(exerciseId)
        val progress = existing?.copy(
            status = "completed",
            completedAt = System.currentTimeMillis()
        ) ?: ExerciseProgress(
            exerciseId = exerciseId,
            category = category,
            status = "completed",
            completedAt = System.currentTimeMillis()
        )
        progressDao.upsertExerciseProgress(progress)
        incrementExercisesCompleted()
    }

    suspend fun revealHint(exerciseId: String, category: String) {
        val existing = progressDao.getExerciseProgress(exerciseId)
        val progress = existing?.copy(
            hintsViewed = existing.hintsViewed + 1
        ) ?: ExerciseProgress(
            exerciseId = exerciseId,
            category = category,
            hintsViewed = 1,
            status = "in_progress"
        )
        progressDao.upsertExerciseProgress(progress)
    }

    suspend fun getExerciseProgress(exerciseId: String): ExerciseProgress? =
        progressDao.getExerciseProgress(exerciseId)

    fun getExerciseProgressByCategory(category: String): Flow<List<ExerciseProgress>> =
        progressDao.getExerciseProgressByCategory(category)

    fun getCompletedExercisesCount(): Flow<Int> = progressDao.getCompletedExercisesCount()
    suspend fun getLastIncompleteExercise(): ExerciseProgress? = progressDao.getLastIncompleteExercise()

    // Learning Stats
    private suspend fun incrementSectionsRead() {
        val today = todayString()
        val existing = progressDao.getLearningStats(today)
        val stats = existing?.copy(
            sectionsRead = existing.sectionsRead + 1
        ) ?: LearningStats(date = today, sectionsRead = 1)
        progressDao.upsertLearningStats(stats)
    }

    private suspend fun incrementExercisesCompleted() {
        val today = todayString()
        val existing = progressDao.getLearningStats(today)
        val stats = existing?.copy(
            exercisesCompleted = existing.exercisesCompleted + 1
        ) ?: LearningStats(date = today, exercisesCompleted = 1)
        progressDao.upsertLearningStats(stats)
    }

    private suspend fun addStudyTime(seconds: Long) {
        val today = todayString()
        val existing = progressDao.getLearningStats(today)
        val stats = existing?.copy(
            studyTimeSeconds = existing.studyTimeSeconds + seconds
        ) ?: LearningStats(date = today, studyTimeSeconds = seconds)
        progressDao.upsertLearningStats(stats)
    }

    fun getRecentStats(days: Int = 7): Flow<List<LearningStats>> = progressDao.getRecentStats(days)
    fun getTotalStudyTime(): Flow<Long?> = progressDao.getTotalStudyTime()
}
