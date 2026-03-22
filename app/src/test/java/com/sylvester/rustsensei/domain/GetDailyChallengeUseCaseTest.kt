package com.sylvester.rustsensei.domain

import com.sylvester.rustsensei.content.*
import com.sylvester.rustsensei.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetDailyChallengeUseCaseTest {

    private lateinit var fakeContentProvider: FakeContentProvider
    private lateinit var fakeProgressDao: FakeProgressDao
    private lateinit var useCase: GetDailyChallengeUseCase

    private val sampleExercise = ExerciseData(
        id = "ex-1",
        title = "Hello World",
        category = "basics",
        difficulty = "beginner",
        description = "Print hello world",
        instructions = "Use println!",
        starterCode = "fn main() {}",
        hints = listOf("Use println!"),
        solution = "fn main() { println!(\"Hello\"); }",
        explanation = "println! prints to stdout",
        relatedBookSection = "ch01-01"
    )

    @Before
    fun setup() {
        fakeContentProvider = FakeContentProvider()
        fakeProgressDao = FakeProgressDao()
        useCase = GetDailyChallengeUseCase(fakeContentProvider, fakeProgressDao)
    }

    @Test
    fun `returns an exercise for today`() = runTest {
        fakeContentProvider.categories = listOf(
            ExerciseCategory("basics", "Basics", "Basic exercises", listOf("ex-1", "ex-2", "ex-3"))
        )
        fakeContentProvider.exercises["ex-1"] = sampleExercise
        fakeContentProvider.exercises["ex-2"] = sampleExercise.copy(id = "ex-2")
        fakeContentProvider.exercises["ex-3"] = sampleExercise.copy(id = "ex-3")

        val result = useCase()

        assertNotNull(result.exercise)
    }

    @Test
    fun `returns isCompleted true when result exists for today`() = runTest {
        fakeContentProvider.categories = listOf(
            ExerciseCategory("basics", "Basics", "desc", listOf("ex-1"))
        )
        fakeContentProvider.exercises["ex-1"] = sampleExercise

        // Simulate existing result for today
        fakeProgressDao.dailyChallengeResults[java.time.LocalDate.now().toString()] =
            DailyChallengeResult(
                date = java.time.LocalDate.now().toString(),
                exerciseId = "ex-1",
                completedAt = System.currentTimeMillis(),
                timeTakenSeconds = 120,
                score = 100
            )

        val result = useCase()

        assertTrue(result.isCompleted)
    }

    @Test
    fun `returns isCompleted false when no result exists`() = runTest {
        fakeContentProvider.categories = listOf(
            ExerciseCategory("basics", "Basics", "desc", listOf("ex-1"))
        )
        fakeContentProvider.exercises["ex-1"] = sampleExercise

        val result = useCase()

        assertFalse(result.isCompleted)
    }

    @Test
    fun `deterministic same date always returns same exercise`() = runTest {
        fakeContentProvider.categories = listOf(
            ExerciseCategory("basics", "Basics", "desc", listOf("ex-1", "ex-2", "ex-3", "ex-4", "ex-5"))
        )
        for (i in 1..5) {
            fakeContentProvider.exercises["ex-$i"] = sampleExercise.copy(id = "ex-$i")
        }

        val result1 = useCase()
        val result2 = useCase()

        assertEquals(result1.exercise?.id, result2.exercise?.id)
    }

    @Test
    fun `handles empty exercise list gracefully`() = runTest {
        fakeContentProvider.categories = emptyList()

        val result = useCase()

        assertNull(result.exercise)
        assertFalse(result.isCompleted)
    }

    @Test
    fun `returns completionTime when result exists`() = runTest {
        fakeContentProvider.categories = listOf(
            ExerciseCategory("basics", "Basics", "desc", listOf("ex-1"))
        )
        fakeContentProvider.exercises["ex-1"] = sampleExercise

        fakeProgressDao.dailyChallengeResults[java.time.LocalDate.now().toString()] =
            DailyChallengeResult(
                date = java.time.LocalDate.now().toString(),
                exerciseId = "ex-1",
                completedAt = System.currentTimeMillis(),
                timeTakenSeconds = 300,
                score = 100
            )

        val result = useCase()

        assertEquals(300L, result.completionTime)
    }

    @Test
    fun `completionTime is null when not completed`() = runTest {
        fakeContentProvider.categories = listOf(
            ExerciseCategory("basics", "Basics", "desc", listOf("ex-1"))
        )
        fakeContentProvider.exercises["ex-1"] = sampleExercise

        val result = useCase()

        assertNull(result.completionTime)
    }

    /**
     * Minimal fake [ContentProvider] for daily challenge tests.
     */
    private class FakeContentProvider : ContentProvider {
        var categories: List<ExerciseCategory> = emptyList()
        val exercises = mutableMapOf<String, ExerciseData>()

        override suspend fun getExerciseCategories(): List<ExerciseCategory> = categories
        override suspend fun getExercise(exerciseId: String): ExerciseData? = exercises[exerciseId]

        override suspend fun getBookIndex(): BookIndex = BookIndex(emptyList())
        override suspend fun getChapter(chapterId: String): BookChapter? = null
        override suspend fun getSection(chapterId: String, sectionId: String): BookSection? = null
        override suspend fun getReferenceIndex(): ReferenceIndex = ReferenceIndex(emptyList())
        override suspend fun getReferenceItem(sectionId: String, itemId: String): JSONObject? = null
        override suspend fun getTotalSectionsCount(): Int = 0
        override suspend fun getTotalExercisesCount(): Int = 0
        override suspend fun getLearningPaths(): List<LearningPath> = emptyList()
        override suspend fun getLearningPath(pathId: String): LearningPath? = null
        override suspend fun getQuizIndex(): List<QuizIndexEntry> = emptyList()
        override suspend fun getQuiz(quizId: String): Quiz? = null
        override suspend fun getRefactoringChallenges(): List<RefactoringChallenge> = emptyList()
        override suspend fun getRefactoringChallenge(id: String): RefactoringChallenge? = null
        override suspend fun getDocIndex(): List<DocIndexEntry> = emptyList()
        override suspend fun getDoc(typeId: String): DocEntry? = null
        override suspend fun loadVisualizationJson(filename: String): JSONObject? = null
        override suspend fun loadProjectJson(filename: String): JSONObject? = null
    }

    /**
     * Minimal fake [ProgressDao] for daily challenge tests.
     * Only implements the methods used by [GetDailyChallengeUseCase].
     */
    private class FakeProgressDao : ProgressDao {
        val dailyChallengeResults = mutableMapOf<String, DailyChallengeResult>()
        val learningStatsMap = mutableMapOf<String, LearningStats>()

        override suspend fun getDailyChallengeResult(date: String): DailyChallengeResult? =
            dailyChallengeResults[date]

        override suspend fun insertDailyChallengeResult(result: DailyChallengeResult) {
            dailyChallengeResults[result.date] = result
        }

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
