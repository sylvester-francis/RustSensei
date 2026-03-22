package com.sylvester.rustsensei.viewmodel

import com.sylvester.rustsensei.content.ContentProvider
import com.sylvester.rustsensei.content.DocIndexEntry
import com.sylvester.rustsensei.content.RefactoringChallenge
import com.sylvester.rustsensei.data.PathProgress
import com.sylvester.rustsensei.data.ProgressDao
import com.sylvester.rustsensei.data.ProjectProgress
import com.sylvester.rustsensei.data.RefactoringResult
import com.sylvester.rustsensei.domain.RefactoringEvent
import com.sylvester.rustsensei.domain.ValidateRefactoringUseCase
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.llm.InferenceEngine
import com.sylvester.rustsensei.llm.ModelLifecycle
import com.sylvester.rustsensei.llm.ModelReadyState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RefactoringViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeContentProvider: FakeContentProvider
    private lateinit var fakeProgressDao: FakeProgressDao
    private lateinit var fakeValidateRefactoring: FakeValidateRefactoringUseCase
    private lateinit var viewModel: RefactoringViewModel

    private val sampleChallenges = listOf(
        RefactoringChallenge(
            id = "challenge-1",
            title = "Simplify match",
            difficulty = "beginner",
            description = "Simplify a nested match expression",
            uglyCode = "fn ugly() { match x { Some(v) => v, None => 0 } }",
            hints = listOf("Use unwrap_or", "Consider if-let"),
            idiomaticSolution = "fn clean() { x.unwrap_or(0) }",
            scoringCriteria = "Use of unwrap_or or unwrap_or_default"
        ),
        RefactoringChallenge(
            id = "challenge-2",
            title = "Use iterators",
            difficulty = "intermediate",
            description = "Replace manual loops with iterators",
            uglyCode = "let mut sum = 0; for i in vec { sum += i; }",
            hints = listOf("Use .iter().sum()"),
            idiomaticSolution = "let sum: i32 = vec.iter().sum();",
            scoringCriteria = "Use of iterator combinators"
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeContentProvider = FakeContentProvider(sampleChallenges)
        fakeProgressDao = FakeProgressDao()
        fakeValidateRefactoring = FakeValidateRefactoringUseCase()
        viewModel = RefactoringViewModel(fakeContentProvider, fakeProgressDao, fakeValidateRefactoring)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads challenges list`() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.challenges.size)
        assertEquals("challenge-1", state.challenges[0].id)
        assertEquals("challenge-2", state.challenges[1].id)
        assertFalse(state.isLoading)
    }

    @Test
    fun `openChallenge sets currentChallenge and userCode to uglyCode`() = runTest {
        advanceUntilIdle()

        viewModel.openChallenge("challenge-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.currentChallenge)
        assertEquals("challenge-1", state.currentChallenge!!.id)
        assertEquals(sampleChallenges[0].uglyCode, state.userCode)
        assertEquals(0, state.hintsRevealed)
        assertFalse(state.showSolution)
        assertNull(state.score)
        assertEquals("", state.feedback)
    }

    @Test
    fun `updateCode updates userCode`() = runTest {
        advanceUntilIdle()

        viewModel.openChallenge("challenge-1")
        advanceUntilIdle()

        viewModel.updateCode("fn refactored() { x.unwrap_or(0) }")
        advanceUntilIdle()

        assertEquals("fn refactored() { x.unwrap_or(0) }", viewModel.uiState.value.userCode)
    }

    @Test
    fun `submitRefactoring sets isValidating to true`() = runTest {
        advanceUntilIdle()

        // Use a flow that suspends so isValidating stays true
        fakeValidateRefactoring.eventFlow = flow {
            // Never emit -- keeps the collector suspended so isValidating stays true
            kotlinx.coroutines.awaitCancellation()
        }

        viewModel.openChallenge("challenge-1")
        advanceUntilIdle()

        viewModel.submitForScoring()
        // Advance just enough for the launch to start but not complete
        testDispatcher.scheduler.advanceTimeBy(1)

        assertTrue(viewModel.uiState.value.isValidating)
    }

    @Test
    fun `submitRefactoring updates feedback and score on completion`() = runTest {
        advanceUntilIdle()

        fakeValidateRefactoring.eventFlow = flow {
            emit(RefactoringEvent.Token("Good"))
            emit(RefactoringEvent.Completed(fullText = "Good refactoring! 85/100", score = 85))
        }

        viewModel.openChallenge("challenge-1")
        advanceUntilIdle()

        viewModel.submitForScoring()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isValidating)
        assertEquals("Good refactoring! 85/100", state.feedback)
        assertEquals(85, state.score)
    }

    @Test
    fun `revealHint increments hintsRevealed`() = runTest {
        advanceUntilIdle()

        viewModel.openChallenge("challenge-1")
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.hintsRevealed)

        viewModel.revealHint()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.hintsRevealed)
    }

    @Test
    fun `revealHint does not exceed total hints`() = runTest {
        advanceUntilIdle()

        viewModel.openChallenge("challenge-1")
        advanceUntilIdle()

        // challenge-1 has 2 hints
        viewModel.revealHint()
        advanceUntilIdle()
        viewModel.revealHint()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.hintsRevealed)

        // Should not exceed 2
        viewModel.revealHint()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.hintsRevealed)
    }

    @Test
    fun `toggleSolution toggles showSolution`() = runTest {
        advanceUntilIdle()

        viewModel.openChallenge("challenge-1")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showSolution)

        viewModel.showSolution()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showSolution)
    }

    @Test
    fun `goBack clears currentChallenge`() = runTest {
        advanceUntilIdle()

        viewModel.openChallenge("challenge-1")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.currentChallenge)

        viewModel.navigateBack()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.currentChallenge)
        assertNull(viewModel.uiState.value.score)
        assertEquals("", viewModel.uiState.value.feedback)
        assertFalse(viewModel.uiState.value.isValidating)
        assertFalse(viewModel.uiState.value.showSolution)
    }

    // --- Fakes ---

    private class FakeValidateRefactoringUseCase : ValidateRefactoringUseCase(
        engine = StubInferenceEngine,
        modelLifecycle = StubModelLifecycle
    ) {
        var eventFlow: Flow<RefactoringEvent> = flow {
            emit(RefactoringEvent.Completed(fullText = "OK", score = 50))
        }

        override operator fun invoke(
            originalCode: String,
            userCode: String,
            idiomaticSolution: String,
            scoringCriteria: String,
            config: InferenceConfig
        ): Flow<RefactoringEvent> = eventFlow
    }

    private object StubInferenceEngine : InferenceEngine {
        override suspend fun loadModel(modelPath: String, contextSize: Int) = false
        override fun generate(prompt: String, config: InferenceConfig, onStats: ((Float, Float, Float) -> Unit)?): Flow<String> = flow {}
        override fun stopGeneration() = Unit
        override fun clearCache() = Unit
        override suspend fun unloadModel() = Unit
        override fun isModelLoaded() = false
    }

    private object StubModelLifecycle : ModelLifecycle {
        override val state: StateFlow<ModelReadyState> = MutableStateFlow(ModelReadyState.NOT_DOWNLOADED)
        override suspend fun ensureLoaded() = false
        override suspend fun unload() = Unit
        override fun scheduleIdleUnload() = Unit
        override fun cancelIdleTimer() = Unit
        override fun refreshState() = Unit
        override fun getActiveBackend() = "CPU"
        override fun isModelLoaded() = false
    }

    private class FakeContentProvider(
        private val challenges: List<RefactoringChallenge>
    ) : ContentProvider {
        override suspend fun getRefactoringChallenges(): List<RefactoringChallenge> = challenges
        override suspend fun getRefactoringChallenge(id: String): RefactoringChallenge? =
            challenges.find { it.id == id }

        // Unused -- only refactoring methods are needed by this ViewModel
        override suspend fun getBookIndex() = throw NotImplementedError()
        override suspend fun getChapter(chapterId: String) = throw NotImplementedError()
        override suspend fun getSection(chapterId: String, sectionId: String) = throw NotImplementedError()
        override suspend fun getExerciseCategories() = throw NotImplementedError()
        override suspend fun getExercise(exerciseId: String) = throw NotImplementedError()
        override suspend fun getReferenceIndex() = throw NotImplementedError()
        override suspend fun getReferenceItem(sectionId: String, itemId: String) = throw NotImplementedError()
        override suspend fun getTotalSectionsCount() = throw NotImplementedError()
        override suspend fun getTotalExercisesCount() = throw NotImplementedError()
        override suspend fun getLearningPaths() = throw NotImplementedError()
        override suspend fun getLearningPath(pathId: String) = throw NotImplementedError()
        override suspend fun getQuizIndex() = throw NotImplementedError()
        override suspend fun getQuiz(quizId: String) = throw NotImplementedError()
        override suspend fun getDocIndex() = emptyList<DocIndexEntry>()
        override suspend fun getDoc(typeId: String) = null
        override suspend fun loadVisualizationJson(filename: String) = null
        override suspend fun loadProjectJson(filename: String) = null
    }

    private class FakeProgressDao : ProgressDao {
        val savedRefactoringResults = mutableListOf<RefactoringResult>()
        val bestScores = mutableMapOf<String, RefactoringResult>()

        override suspend fun insertRefactoringResult(result: RefactoringResult) {
            savedRefactoringResults.add(result)
        }

        override suspend fun getBestRefactoringResult(challengeId: String): RefactoringResult? =
            bestScores[challengeId]

        override fun getAllRefactoringResults(): Flow<List<RefactoringResult>> =
            MutableStateFlow(savedRefactoringResults)

        // Stubs for unused DAO methods
        override suspend fun upsertPathProgress(progress: PathProgress) = Unit
        override suspend fun getPathProgress(pathId: String) = emptyList<PathProgress>()
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
        override suspend fun upsertLearningStats(stats: com.sylvester.rustsensei.data.LearningStats) = Unit
        override suspend fun getLearningStats(date: String) = null
        override fun getRecentStats(days: Int): Flow<List<com.sylvester.rustsensei.data.LearningStats>> = MutableStateFlow(emptyList())
        override fun getTotalStudyTime(): Flow<Long?> = MutableStateFlow(null)
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
        override suspend fun getStatsForDate(date: String) = null
        override suspend fun insertDailyChallengeResult(result: com.sylvester.rustsensei.data.DailyChallengeResult) = Unit
        override suspend fun getDailyChallengeResult(date: String) = null
        override suspend fun getDailyChallengeCompletedCount() = 0
        override suspend fun upsertProjectProgress(progress: ProjectProgress) = Unit
        override suspend fun getProjectProgress(projectId: String) = emptyList<ProjectProgress>()
        override fun observeProjectProgress(projectId: String): Flow<List<ProjectProgress>> = MutableStateFlow(emptyList())
    }
}
