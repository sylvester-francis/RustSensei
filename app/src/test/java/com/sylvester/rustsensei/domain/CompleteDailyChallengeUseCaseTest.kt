package com.sylvester.rustsensei.domain

import com.sylvester.rustsensei.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class CompleteDailyChallengeUseCaseTest {

    private lateinit var fakeProgressDao: FakeProgressDao
    private lateinit var useCase: CompleteDailyChallengeUseCase

    @Before
    fun setup() {
        fakeProgressDao = FakeProgressDao()
        useCase = CompleteDailyChallengeUseCase(fakeProgressDao)
    }

    @Test
    fun `inserts DailyChallengeResult with correct date and exerciseId`() = runTest {
        useCase("ex-1", 120L)

        val today = LocalDate.now().toString()
        val result = fakeProgressDao.dailyChallengeResults[today]
        assertNotNull(result)
        assertEquals(today, result!!.date)
        assertEquals("ex-1", result.exerciseId)
        assertEquals(100, result.score)
        assertEquals(120L, result.timeTakenSeconds)
    }

    @Test
    fun `updates existing LearningStats for today`() = runTest {
        val today = LocalDate.now().toString()
        fakeProgressDao.learningStatsMap[today] = LearningStats(
            date = today,
            sectionsRead = 3,
            exercisesCompleted = 2,
            studyTimeSeconds = 600
        )

        useCase("ex-1", 180L)

        val updated = fakeProgressDao.learningStatsMap[today]
        assertNotNull(updated)
        assertEquals(3, updated!!.exercisesCompleted)  // 2 + 1
        assertEquals(780L, updated.studyTimeSeconds)   // 600 + 180
        assertEquals(3, updated.sectionsRead)           // unchanged
    }

    @Test
    fun `creates new LearningStats when none exists for today`() = runTest {
        val today = LocalDate.now().toString()

        useCase("ex-1", 90L)

        val stats = fakeProgressDao.learningStatsMap[today]
        assertNotNull(stats)
        assertEquals(today, stats!!.date)
        assertEquals(0, stats.sectionsRead)
        assertEquals(1, stats.exercisesCompleted)
        assertEquals(90L, stats.studyTimeSeconds)
    }

    @Test
    fun `records time taken correctly`() = runTest {
        useCase("ex-1", 300L)

        val today = LocalDate.now().toString()
        val result = fakeProgressDao.dailyChallengeResults[today]
        assertNotNull(result)
        assertEquals(300L, result!!.timeTakenSeconds)

        val stats = fakeProgressDao.learningStatsMap[today]
        assertNotNull(stats)
        assertEquals(300L, stats!!.studyTimeSeconds)
    }

    @Test
    fun `inserts result with non-zero completedAt timestamp`() = runTest {
        useCase("ex-1", 60L)

        val today = LocalDate.now().toString()
        val result = fakeProgressDao.dailyChallengeResults[today]
        assertNotNull(result)
        assert(result!!.completedAt > 0) { "completedAt should be a positive timestamp" }
    }

    @Test
    fun `multiple completions accumulate in LearningStats`() = runTest {
        useCase("ex-1", 100L)
        useCase("ex-2", 200L)

        val today = LocalDate.now().toString()
        val stats = fakeProgressDao.learningStatsMap[today]
        assertNotNull(stats)
        assertEquals(2, stats!!.exercisesCompleted)
        assertEquals(300L, stats.studyTimeSeconds) // 100 + 200
    }

    /**
     * Minimal fake [ProgressDao] for [CompleteDailyChallengeUseCase] tests.
     */
    private class FakeProgressDao : ProgressDao {
        val dailyChallengeResults = mutableMapOf<String, DailyChallengeResult>()
        val learningStatsMap = mutableMapOf<String, LearningStats>()

        override suspend fun insertDailyChallengeResult(result: DailyChallengeResult) {
            dailyChallengeResults[result.date] = result
        }

        override suspend fun getDailyChallengeResult(date: String): DailyChallengeResult? =
            dailyChallengeResults[date]

        override suspend fun getDailyChallengeCompletedCount(): Int =
            dailyChallengeResults.size

        override suspend fun getLearningStats(date: String): LearningStats? =
            learningStatsMap[date]

        override suspend fun upsertLearningStats(stats: LearningStats) {
            learningStatsMap[stats.date] = stats
        }

        // Stub implementations for unused methods
        override suspend fun upsertBookProgress(progress: BookProgress) {}
        override suspend fun getBookProgress(sectionId: String): BookProgress? = null
        override fun getChapterProgress(chapterId: String): Flow<List<BookProgress>> = flowOf(emptyList())
        override fun getCompletedSectionsCount(): Flow<Int> = flowOf(0)
        override fun getCompletedChaptersCount(): Flow<Int> = flowOf(0)
        override fun getBookmarks(): Flow<List<BookProgress>> = flowOf(emptyList())
        override suspend fun getLastReadSection(): BookProgress? = null
        override suspend fun upsertExerciseProgress(progress: ExerciseProgress) {}
        override suspend fun getExerciseProgress(exerciseId: String): ExerciseProgress? = null
        override fun getExerciseProgressByCategory(category: String): Flow<List<ExerciseProgress>> = flowOf(emptyList())
        override fun getCompletedExercisesCount(): Flow<Int> = flowOf(0)
        override suspend fun getLastIncompleteExercise(): ExerciseProgress? = null
        override fun getRecentStats(days: Int): Flow<List<LearningStats>> = flowOf(emptyList())
        override fun getTotalStudyTime(): Flow<Long?> = flowOf(null)
        override suspend fun upsertPathProgress(progress: PathProgress) {}
        override suspend fun getPathProgress(pathId: String): List<PathProgress> = emptyList()
        override fun observePathProgress(pathId: String): Flow<List<PathProgress>> = flowOf(emptyList())
        override fun observeAllPathProgress(): Flow<List<PathProgress>> = flowOf(emptyList())
        override suspend fun insertQuizResult(result: QuizResult) {}
        override suspend fun getBestQuizResult(quizId: String): QuizResult? = null
        override fun getQuizResults(quizId: String): Flow<List<QuizResult>> = flowOf(emptyList())
        override fun getAllQuizResults(): Flow<List<QuizResult>> = flowOf(emptyList())
        override suspend fun upsertNote(note: UserNote) {}
        override suspend fun getNotesForSection(sectionId: String): List<UserNote> = emptyList()
        override fun getAllNotes(): Flow<List<UserNote>> = flowOf(emptyList())
        override suspend fun deleteNote(noteId: Long) {}
        override suspend fun searchNotes(query: String): List<UserNote> = emptyList()
        override suspend fun getCompletedExerciseCountByCategory(category: String): Int = 0
        override suspend fun getCompletedExercisesCountSync(): Int = 0
        override suspend fun getCompletedSectionsCountSync(): Int = 0
        override suspend fun getStatsForDate(date: String): LearningStats? = learningStatsMap[date]
        override suspend fun insertRefactoringResult(result: RefactoringResult) {}
        override suspend fun getBestRefactoringResult(challengeId: String): RefactoringResult? = null
        override fun getAllRefactoringResults(): Flow<List<RefactoringResult>> = flowOf(emptyList())
        override suspend fun upsertProjectProgress(progress: ProjectProgress) {}
        override suspend fun getProjectProgress(projectId: String): List<ProjectProgress> = emptyList()
        override fun observeProjectProgress(projectId: String): Flow<List<ProjectProgress>> = flowOf(emptyList())
    }
}
