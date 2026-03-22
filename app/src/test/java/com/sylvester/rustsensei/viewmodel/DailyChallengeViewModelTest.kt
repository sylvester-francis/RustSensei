package com.sylvester.rustsensei.viewmodel

import com.sylvester.rustsensei.content.BookChapter
import com.sylvester.rustsensei.content.BookIndex
import com.sylvester.rustsensei.content.BookSection
import com.sylvester.rustsensei.content.ContentProvider
import com.sylvester.rustsensei.content.DocEntry
import com.sylvester.rustsensei.content.DocIndexEntry
import com.sylvester.rustsensei.content.ExerciseCategory
import com.sylvester.rustsensei.content.ExerciseData
import com.sylvester.rustsensei.content.RefactoringChallenge
import com.sylvester.rustsensei.content.ReferenceIndex
import com.sylvester.rustsensei.data.DailyChallengeResult
import com.sylvester.rustsensei.data.LearningPath
import com.sylvester.rustsensei.data.LearningStats
import com.sylvester.rustsensei.data.PathProgress
import com.sylvester.rustsensei.data.ProgressDao
import com.sylvester.rustsensei.data.Quiz
import com.sylvester.rustsensei.data.QuizIndexEntry
import com.sylvester.rustsensei.domain.CompleteDailyChallengeUseCase
import com.sylvester.rustsensei.domain.GetDailyChallengeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DailyChallengeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeContentProvider: FakeContentProvider
    private lateinit var fakeProgressDao: FakeProgressDao
    private lateinit var viewModel: DailyChallengeViewModel

    private val sampleExercise = ExerciseData(
        id = "ownership1",
        title = "Ownership Basics",
        category = "ownership",
        difficulty = "beginner",
        description = "Learn about Rust ownership",
        instructions = "Fix the ownership error",
        starterCode = "fn main() {\n    let s = String::from(\"hello\");\n}",
        hints = listOf("Think about move semantics"),
        solution = "fn main() {\n    let s = String::from(\"hello\");\n    let s2 = s.clone();\n}",
        explanation = "Ownership rules require explicit cloning",
        relatedBookSection = "ch04-01"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeContentProvider = FakeContentProvider()
        fakeProgressDao = FakeProgressDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): DailyChallengeViewModel {
        val getDailyChallenge = GetDailyChallengeUseCase(fakeContentProvider, fakeProgressDao)
        val completeDailyChallenge = CompleteDailyChallengeUseCase(fakeProgressDao)
        return DailyChallengeViewModel(getDailyChallenge, completeDailyChallenge)
    }

    // --- init ---

    @Test
    fun `init loads today's challenge`() = runTest {
        fakeContentProvider.exerciseCategories = listOf(
            ExerciseCategory("ownership", "Ownership", "desc", listOf("ownership1"))
        )
        fakeContentProvider.exercises["ownership1"] = sampleExercise

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.exercise)
        assertEquals("ownership1", state.exercise!!.id)
        assertEquals("Ownership Basics", state.exercise!!.title)
        assertFalse(state.isLoading)
    }

    // --- loadChallenge ---

    @Test
    fun `loadChallenge sets exercise from use case`() = runTest {
        fakeContentProvider.exerciseCategories = listOf(
            ExerciseCategory("ownership", "Ownership", "desc", listOf("ownership1"))
        )
        fakeContentProvider.exercises["ownership1"] = sampleExercise

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.exercise)
        assertEquals("ownership1", state.exercise!!.id)
        assertEquals(sampleExercise.starterCode, state.userCode)
        assertFalse(state.isLoading)
        assertFalse(state.isCompleted)
    }

    @Test
    fun `loadChallenge sets isCompleted when already done`() = runTest {
        fakeContentProvider.exerciseCategories = listOf(
            ExerciseCategory("ownership", "Ownership", "desc", listOf("ownership1"))
        )
        fakeContentProvider.exercises["ownership1"] = sampleExercise

        // Pre-insert a result for today
        val today = java.time.LocalDate.now().toString()
        fakeProgressDao.dailyChallengeResults[today] = DailyChallengeResult(
            date = today,
            exerciseId = "ownership1",
            completedAt = System.currentTimeMillis(),
            timeTakenSeconds = 120L,
            score = 100
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isCompleted)
        assertEquals(120L, state.completionTimeSeconds)
        assertFalse(state.isLoading)
    }

    // --- updateCode ---

    @Test
    fun `updateCode updates userCode in state`() = runTest {
        fakeContentProvider.exerciseCategories = listOf(
            ExerciseCategory("ownership", "Ownership", "desc", listOf("ownership1"))
        )
        fakeContentProvider.exercises["ownership1"] = sampleExercise

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateCode("fn main() { println!(\"modified\"); }")
        advanceUntilIdle()

        assertEquals("fn main() { println!(\"modified\"); }", viewModel.uiState.value.userCode)
    }

    // --- submitChallenge ---

    @Test
    fun `submitChallenge marks completion`() = runTest {
        fakeContentProvider.exerciseCategories = listOf(
            ExerciseCategory("ownership", "Ownership", "desc", listOf("ownership1"))
        )
        fakeContentProvider.exercises["ownership1"] = sampleExercise

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.submitChallenge()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isCompleted)
        assertNotNull(state.completionTimeSeconds)

        // Verify the DAO received the result
        val today = java.time.LocalDate.now().toString()
        assertNotNull(fakeProgressDao.dailyChallengeResults[today])
    }

    @Test
    fun `submitChallenge triggers showConfetti`() = runTest {
        fakeContentProvider.exerciseCategories = listOf(
            ExerciseCategory("ownership", "Ownership", "desc", listOf("ownership1"))
        )
        fakeContentProvider.exercises["ownership1"] = sampleExercise

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.submitChallenge()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showConfetti)
    }

    // --- dismissConfetti ---

    @Test
    fun `dismissConfetti clears the flag`() = runTest {
        fakeContentProvider.exerciseCategories = listOf(
            ExerciseCategory("ownership", "Ownership", "desc", listOf("ownership1"))
        )
        fakeContentProvider.exercises["ownership1"] = sampleExercise

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.submitChallenge()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showConfetti)

        viewModel.dismissConfetti()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showConfetti)
    }

    // --- Fakes ---

    private class FakeContentProvider : ContentProvider {
        var exerciseCategories: List<ExerciseCategory> = emptyList()
        var exercises: MutableMap<String, ExerciseData> = mutableMapOf()

        override suspend fun getExerciseCategories(): List<ExerciseCategory> = exerciseCategories
        override suspend fun getExercise(exerciseId: String): ExerciseData? = exercises[exerciseId]

        override suspend fun getReferenceItem(sectionId: String, itemId: String): JSONObject? = null
        override suspend fun getBookIndex(): BookIndex = BookIndex(emptyList())
        override suspend fun getChapter(chapterId: String): BookChapter? = null
        override suspend fun getSection(chapterId: String, sectionId: String): BookSection? = null
        override suspend fun getReferenceIndex(): ReferenceIndex = ReferenceIndex(emptyList())
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

    private class FakeProgressDao : ProgressDao {
        val dailyChallengeResults = mutableMapOf<String, DailyChallengeResult>()
        val learningStats = mutableMapOf<String, LearningStats>()

        override suspend fun getDailyChallengeResult(date: String): DailyChallengeResult? =
            dailyChallengeResults[date]

        override suspend fun insertDailyChallengeResult(result: DailyChallengeResult) {
            dailyChallengeResults[result.date] = result
        }

        override suspend fun getLearningStats(date: String): LearningStats? = learningStats[date]
        override suspend fun getStatsForDate(date: String): LearningStats? = learningStats[date]

        override suspend fun upsertLearningStats(stats: LearningStats) {
            learningStats[stats.date] = stats
        }

        override suspend fun getDailyChallengeCompletedCount(): Int = dailyChallengeResults.size

        // Stubs for unused DAO methods
        override suspend fun upsertPathProgress(progress: PathProgress) = Unit
        override suspend fun getPathProgress(pathId: String): List<PathProgress> = emptyList()
        override fun observePathProgress(pathId: String): Flow<List<PathProgress>> = MutableStateFlow(emptyList())
        override fun observeAllPathProgress(): Flow<List<PathProgress>> = MutableStateFlow(emptyList())
        override suspend fun upsertBookProgress(progress: com.sylvester.rustsensei.data.BookProgress) = Unit
        override suspend fun getBookProgress(sectionId: String) = null
        override fun getChapterProgress(chapterId: String): Flow<List<com.sylvester.rustsensei.data.BookProgress>> = MutableStateFlow(emptyList())
        override fun getCompletedSectionsCount(): Flow<Int> = MutableStateFlow(0)
        override fun getCompletedChaptersCount(): Flow<Int> = MutableStateFlow(0)
        override fun getBookmarks(): Flow<List<com.sylvester.rustsensei.data.BookProgress>> = MutableStateFlow(emptyList())
        override suspend fun getLastReadSection() = null
        override suspend fun upsertExerciseProgress(progress: com.sylvester.rustsensei.data.ExerciseProgress) = Unit
        override suspend fun getExerciseProgress(exerciseId: String) = null
        override fun getExerciseProgressByCategory(category: String): Flow<List<com.sylvester.rustsensei.data.ExerciseProgress>> = MutableStateFlow(emptyList())
        override fun getCompletedExercisesCount(): Flow<Int> = MutableStateFlow(0)
        override suspend fun getLastIncompleteExercise() = null
        override suspend fun insertQuizResult(result: com.sylvester.rustsensei.data.QuizResult) = Unit
        override suspend fun getBestQuizResult(quizId: String) = null
        override fun getQuizResults(quizId: String): Flow<List<com.sylvester.rustsensei.data.QuizResult>> = MutableStateFlow(emptyList())
        override fun getAllQuizResults(): Flow<List<com.sylvester.rustsensei.data.QuizResult>> = MutableStateFlow(emptyList())
        override suspend fun upsertNote(note: com.sylvester.rustsensei.data.UserNote) = Unit
        override suspend fun getNotesForSection(sectionId: String) = emptyList<com.sylvester.rustsensei.data.UserNote>()
        override fun getAllNotes(): Flow<List<com.sylvester.rustsensei.data.UserNote>> = MutableStateFlow(emptyList())
        override suspend fun deleteNote(noteId: Long) = Unit
        override suspend fun searchNotes(query: String) = emptyList<com.sylvester.rustsensei.data.UserNote>()
        override suspend fun getCompletedExerciseCountByCategory(category: String) = 0
        override suspend fun getCompletedExercisesCountSync() = 0
        override suspend fun getCompletedSectionsCountSync() = 0
        override suspend fun insertRefactoringResult(result: com.sylvester.rustsensei.data.RefactoringResult) = Unit
        override suspend fun getBestRefactoringResult(challengeId: String) = null
        override fun getAllRefactoringResults(): Flow<List<com.sylvester.rustsensei.data.RefactoringResult>> = MutableStateFlow(emptyList())
        override suspend fun upsertProjectProgress(progress: com.sylvester.rustsensei.data.ProjectProgress) = Unit
        override suspend fun getProjectProgress(projectId: String) = emptyList<com.sylvester.rustsensei.data.ProjectProgress>()
        override fun observeProjectProgress(projectId: String): Flow<List<com.sylvester.rustsensei.data.ProjectProgress>> = MutableStateFlow(emptyList())
        override fun getRecentStats(days: Int): Flow<List<LearningStats>> = MutableStateFlow(emptyList())
        override fun getTotalStudyTime(): Flow<Long?> = MutableStateFlow(null)
    }
}
