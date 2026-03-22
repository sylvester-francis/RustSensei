package com.sylvester.rustsensei.viewmodel

import com.sylvester.rustsensei.content.ContentProvider
import com.sylvester.rustsensei.data.LearningPath
import com.sylvester.rustsensei.data.PathProgress
import com.sylvester.rustsensei.data.PathStep
import com.sylvester.rustsensei.data.ProgressDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LearningPathViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeContentRepo: FakeContentProvider
    private lateinit var fakeProgressDao: FakeProgressDao
    private lateinit var viewModel: LearningPathViewModel

    private val sampleSteps = listOf(
        PathStep("step-1", "read", "ch01-getting-started", "Getting Started", "Install Rust"),
        PathStep("step-2", "exercise", "intro1", "Hello World", "Complete the intro exercise"),
        PathStep("step-3", "review", "review", "Review", "Review flashcards")
    )

    private val samplePath = LearningPath(
        id = "rust-fundamentals",
        title = "Rust Fundamentals",
        description = "Master the basics",
        estimatedDays = 14,
        steps = sampleSteps
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeContentRepo = FakeContentProvider(listOf(samplePath))
        fakeProgressDao = FakeProgressDao()
        viewModel = LearningPathViewModel(fakeContentRepo, fakeProgressDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Bug regression: tapping a step must NOT mark it complete immediately ---

    @Test
    fun `setPendingStep does not mark step complete`() = runTest {
        advanceUntilIdle()

        viewModel.setPendingStep("rust-fundamentals", sampleSteps[0])
        advanceUntilIdle()

        val progress = viewModel.uiState.value.stepProgress
        val compositeId = "rust-fundamentals:step-1"
        assertTrue(
            "Step should NOT be complete after setPendingStep",
            progress[compositeId] != true
        )
    }

    @Test
    fun `setPendingStep stores correct pending step info`() = runTest {
        advanceUntilIdle()

        viewModel.setPendingStep("rust-fundamentals", sampleSteps[1])

        val pending = viewModel.pendingStep.value
        assertNotNull(pending)
        assertEquals("rust-fundamentals", pending!!.pathId)
        assertEquals("step-2", pending.stepId)
        assertEquals("exercise", pending.type)
        assertEquals("intro1", pending.targetId)
    }

    @Test
    fun `completePendingStep marks the step complete and clears pending`() = runTest {
        advanceUntilIdle()

        viewModel.setPendingStep("rust-fundamentals", sampleSteps[0])
        viewModel.completePendingStep()
        advanceUntilIdle()

        assertNull(viewModel.pendingStep.value)

        val saved = fakeProgressDao.savedProgress["rust-fundamentals:step-1"]
        assertNotNull("Step should be persisted to DAO", saved)
        assertTrue(saved!!.isCompleted)
    }

    @Test
    fun `completePendingStep is no-op when nothing is pending`() = runTest {
        advanceUntilIdle()

        viewModel.completePendingStep()
        advanceUntilIdle()

        assertTrue(fakeProgressDao.savedProgress.isEmpty())
    }

    @Test
    fun `clearPendingStep clears without marking complete`() = runTest {
        advanceUntilIdle()

        viewModel.setPendingStep("rust-fundamentals", sampleSteps[0])
        viewModel.clearPendingStep()
        advanceUntilIdle()

        assertNull(viewModel.pendingStep.value)
        assertTrue(
            "Step should NOT be marked complete after clearPendingStep",
            fakeProgressDao.savedProgress.isEmpty()
        )
    }

    // --- Step completion logic ---

    @Test
    fun `markStepComplete persists progress to DAO`() = runTest {
        advanceUntilIdle()

        viewModel.markStepComplete("rust-fundamentals", "step-1")
        advanceUntilIdle()

        val saved = fakeProgressDao.savedProgress["rust-fundamentals:step-1"]
        assertNotNull(saved)
        assertEquals("rust-fundamentals", saved!!.pathId)
        assertTrue(saved.isCompleted)
        assertNotNull(saved.completedAt)
    }

    @Test
    fun `markStepComplete updates local ui state immediately`() = runTest {
        advanceUntilIdle()

        viewModel.markStepComplete("rust-fundamentals", "step-2")
        advanceUntilIdle()

        val progress = viewModel.uiState.value.stepProgress
        assertTrue(progress["rust-fundamentals:step-2"] == true)
    }

    // --- Tab navigation signals ---

    @Test
    fun `requestTabNavigation sets requested tab`() = runTest {
        viewModel.requestTabNavigation("learn")
        assertEquals("learn", viewModel.requestedTab.value)
    }

    @Test
    fun `clearTabRequest clears requested tab`() = runTest {
        viewModel.requestTabNavigation("practice")
        viewModel.clearTabRequest()
        assertNull(viewModel.requestedTab.value)
    }

    // --- Path completion percentage ---

    @Test
    fun `getPathCompletionPercent returns zero for no progress`() = runTest {
        advanceUntilIdle()
        assertEquals(0f, viewModel.getPathCompletionPercent(samplePath))
    }

    @Test
    fun `getPathCompletionPercent returns correct fraction`() = runTest {
        advanceUntilIdle()

        viewModel.markStepComplete("rust-fundamentals", "step-1")
        advanceUntilIdle()

        val percent = viewModel.getPathCompletionPercent(samplePath)
        assertEquals(1f / 3f, percent, 0.01f)
    }

    @Test
    fun `getPathCompletionPercent returns 1 when all steps complete`() = runTest {
        advanceUntilIdle()

        sampleSteps.forEach { viewModel.markStepComplete("rust-fundamentals", it.id) }
        advanceUntilIdle()

        assertEquals(1f, viewModel.getPathCompletionPercent(samplePath), 0.01f)
    }

    // --- Path selection / mode ---

    @Test
    fun `selectPath switches to DETAIL mode`() = runTest {
        advanceUntilIdle()

        viewModel.selectPath("rust-fundamentals")
        advanceUntilIdle()

        assertEquals(PathMode.DETAIL, viewModel.uiState.value.mode)
        assertEquals(samplePath, viewModel.uiState.value.selectedPath)
    }

    @Test
    fun `navigateBack returns to LIST mode`() = runTest {
        advanceUntilIdle()

        viewModel.selectPath("rust-fundamentals")
        advanceUntilIdle()
        viewModel.navigateBack()

        assertEquals(PathMode.LIST, viewModel.uiState.value.mode)
        assertNull(viewModel.uiState.value.selectedPath)
    }

    // --- Fakes ---

    private class FakeContentProvider(
        private val paths: List<LearningPath>
    ) : ContentProvider {
        override suspend fun getLearningPaths(): List<LearningPath> = paths
        override suspend fun getLearningPath(pathId: String): LearningPath? =
            paths.find { it.id == pathId }

        // Unused — only learning path methods are needed by this ViewModel
        override suspend fun getBookIndex() = throw NotImplementedError()
        override suspend fun getChapter(chapterId: String) = throw NotImplementedError()
        override suspend fun getSection(chapterId: String, sectionId: String) = throw NotImplementedError()
        override suspend fun getExerciseCategories() = throw NotImplementedError()
        override suspend fun getExercise(exerciseId: String) = throw NotImplementedError()
        override suspend fun getReferenceIndex() = throw NotImplementedError()
        override suspend fun getReferenceItem(sectionId: String, itemId: String) = throw NotImplementedError()
        override suspend fun getTotalSectionsCount() = throw NotImplementedError()
        override suspend fun getTotalExercisesCount() = throw NotImplementedError()
        override suspend fun getQuizIndex() = throw NotImplementedError()
        override suspend fun getQuiz(quizId: String) = throw NotImplementedError()
        override suspend fun getRefactoringChallenges() = emptyList<com.sylvester.rustsensei.content.RefactoringChallenge>()
        override suspend fun getRefactoringChallenge(id: String) = null
        override suspend fun getDocIndex() = emptyList<com.sylvester.rustsensei.content.DocIndexEntry>()
        override suspend fun getDoc(typeId: String) = null
        override suspend fun loadVisualizationJson(filename: String) = null
        override suspend fun loadProjectJson(filename: String) = null
    }

    private class FakeProgressDao : ProgressDao {
        val savedProgress = mutableMapOf<String, PathProgress>()
        private val allProgressFlow = MutableStateFlow<List<PathProgress>>(emptyList())

        override suspend fun upsertPathProgress(progress: PathProgress) {
            savedProgress[progress.stepId] = progress
            allProgressFlow.value = savedProgress.values.toList()
        }

        override suspend fun getPathProgress(pathId: String): List<PathProgress> =
            savedProgress.values.filter { it.pathId == pathId }

        override fun observePathProgress(pathId: String): Flow<List<PathProgress>> =
            MutableStateFlow(savedProgress.values.filter { it.pathId == pathId })

        override fun observeAllPathProgress(): Flow<List<PathProgress>> = allProgressFlow

        // Stubs for unused DAO methods
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
        override suspend fun insertRefactoringResult(result: com.sylvester.rustsensei.data.RefactoringResult) = Unit
        override suspend fun getBestRefactoringResult(challengeId: String) = null
        override fun getAllRefactoringResults(): Flow<List<com.sylvester.rustsensei.data.RefactoringResult>> = MutableStateFlow(emptyList())
        override suspend fun upsertProjectProgress(progress: com.sylvester.rustsensei.data.ProjectProgress) = Unit
        override suspend fun getProjectProgress(projectId: String) = emptyList<com.sylvester.rustsensei.data.ProjectProgress>()
        override fun observeProjectProgress(projectId: String): Flow<List<com.sylvester.rustsensei.data.ProjectProgress>> = MutableStateFlow(emptyList())
    }
}
