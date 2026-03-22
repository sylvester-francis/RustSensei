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
import com.sylvester.rustsensei.data.InferenceConfigProvider
import com.sylvester.rustsensei.data.LearningPath
import com.sylvester.rustsensei.data.Quiz
import com.sylvester.rustsensei.data.QuizIndexEntry
import com.sylvester.rustsensei.domain.ExplainErrorUseCase
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.llm.ModelLifecycle
import com.sylvester.rustsensei.llm.ModelReadyState
import com.sylvester.rustsensei.testdoubles.FakeInferenceEngine
import com.sylvester.rustsensei.testdoubles.FakeModelLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExplainErrorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeEngine: FakeInferenceEngine
    private lateinit var fakeLifecycle: FakeModelLifecycle
    private lateinit var fakeContentProvider: FakeContentProvider
    private lateinit var fakeConfigProvider: FakeInferenceConfigProvider
    private lateinit var viewModel: ExplainErrorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeEngine = FakeInferenceEngine()
        fakeLifecycle = FakeModelLifecycle()
        fakeContentProvider = FakeContentProvider()
        fakeConfigProvider = FakeInferenceConfigProvider()

        val useCase = ExplainErrorUseCase(fakeContentProvider, fakeEngine, fakeLifecycle)
        viewModel = ExplainErrorViewModel(useCase, fakeConfigProvider, fakeLifecycle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Initial state ---

    @Test
    fun `initial state has empty input and no explanation`() = runTest {
        val state = viewModel.uiState.value
        assertEquals("", state.inputText)
        assertEquals("", state.explanationText)
        assertNull(state.detectedCode)
        assertFalse(state.isExplaining)
        assertNull(state.errorMessage)
    }

    // --- updateInput ---

    @Test
    fun `updateInput sets input text and detects error codes`() = runTest {
        viewModel.updateInput("error[E0308]: mismatched types")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("error[E0308]: mismatched types", state.inputText)
        assertEquals("E0308", state.detectedCode)
    }

    @Test
    fun `updateInput detects E0382 from borrow of moved value`() = runTest {
        viewModel.updateInput("error[E0382]: borrow of moved value")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("error[E0382]: borrow of moved value", state.inputText)
        assertEquals("E0382", state.detectedCode)
    }

    @Test
    fun `updateInput returns null detectedCode for text without error codes`() = runTest {
        viewModel.updateInput("some random text without errors")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("some random text without errors", state.inputText)
        assertNull(state.detectedCode)
    }

    // --- explain ---

    @Test
    fun `explain sets isExplaining to true`() = runTest {
        fakeEngine.tokensToEmit = listOf("Explaining", "...")

        viewModel.updateInput("error[E0308]: mismatched types")
        viewModel.explain()

        // Before advancing the coroutine, isExplaining should be set synchronously
        // The explain() method sets isExplaining=true before launching the coroutine
        assertTrue(viewModel.uiState.value.isExplaining)

        advanceUntilIdle()
    }

    @Test
    fun `explain updates explanationText with streamed tokens`() = runTest {
        fakeEngine.tokensToEmit = listOf("The ", "error ", "means")

        viewModel.updateInput("error[E0308]: mismatched types")
        viewModel.explain()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // The use case concatenates tokens and strips think tags
        assertEquals("The error means", state.explanationText)
    }

    @Test
    fun `explain sets isExplaining to false on completion`() = runTest {
        fakeEngine.tokensToEmit = listOf("Partial")

        viewModel.updateInput("error[E0382]: borrow of moved value")
        viewModel.explain()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isExplaining)
        assertNotNull(state.explanationText)
    }

    @Test
    fun `explain sets errorMessage on failure`() = runTest {
        fakeLifecycle.ensureLoadedResult = false

        viewModel.updateInput("error[E0308]: mismatched types")
        viewModel.explain()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isExplaining)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("not available"))
    }

    // --- clearError ---

    @Test
    fun `clearError clears the error message`() = runTest {
        fakeLifecycle.ensureLoadedResult = false

        viewModel.updateInput("error[E0308]: mismatched types")
        viewModel.explain()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    // --- reset ---

    @Test
    fun `reset clears all state back to initial`() = runTest {
        fakeEngine.tokensToEmit = listOf("Explanation")

        viewModel.updateInput("error[E0382]: borrow of moved value")
        viewModel.explain()
        advanceUntilIdle()

        // Verify state is non-initial
        assertTrue(viewModel.uiState.value.explanationText.isNotEmpty())

        viewModel.reset()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.inputText)
        assertNull(state.detectedCode)
        assertEquals("", state.explanationText)
        assertFalse(state.isExplaining)
        assertNull(state.errorMessage)
    }

    // --- explain does nothing when input is blank ---

    @Test
    fun `explain does nothing when input is blank`() = runTest {
        fakeEngine.tokensToEmit = listOf("Should not appear")

        // Leave input empty (default "")
        viewModel.explain()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.explanationText)
        assertFalse(state.isExplaining)
        assertEquals(0, fakeEngine.generateCallCount)
    }

    // --- Fakes ---

    private class FakeInferenceConfigProvider : InferenceConfigProvider {
        override fun loadInferenceConfig(): InferenceConfig = InferenceConfig()
    }

    private class FakeContentProvider : ContentProvider {
        override suspend fun getReferenceItem(sectionId: String, itemId: String): JSONObject? = null
        override suspend fun getBookIndex(): BookIndex = BookIndex(emptyList())
        override suspend fun getChapter(chapterId: String): BookChapter? = null
        override suspend fun getSection(chapterId: String, sectionId: String): BookSection? = null
        override suspend fun getExerciseCategories(): List<ExerciseCategory> = emptyList()
        override suspend fun getExercise(exerciseId: String): ExerciseData? = null
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
}
