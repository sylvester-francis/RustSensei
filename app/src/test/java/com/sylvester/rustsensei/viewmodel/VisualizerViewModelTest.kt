package com.sylvester.rustsensei.viewmodel

import com.sylvester.rustsensei.content.ContentProvider
import com.sylvester.rustsensei.content.DocIndexEntry
import com.sylvester.rustsensei.content.OwnershipScenario
import com.sylvester.rustsensei.content.RefactoringChallenge
import com.sylvester.rustsensei.content.VisualizationStep
import com.sylvester.rustsensei.data.LearningPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONArray
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
class VisualizerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeContentProvider: FakeContentProvider
    private lateinit var viewModel: VisualizerViewModel

    private val sampleSteps = listOf(
        VisualizationStep(
            label = "Step 1: Create variable",
            code = "let s = String::from(\"hello\");",
            stackVariables = emptyList(),
            heapAllocations = emptyList(),
            annotation = "s owns the heap-allocated String"
        ),
        VisualizationStep(
            label = "Step 2: Move ownership",
            code = "let t = s;",
            stackVariables = emptyList(),
            heapAllocations = emptyList(),
            annotation = "Ownership moves from s to t"
        ),
        VisualizationStep(
            label = "Step 3: Use after move",
            code = "println!(\"{}\", t);",
            stackVariables = emptyList(),
            heapAllocations = emptyList(),
            annotation = "t is valid; s is moved"
        )
    )

    private val sampleScenario = OwnershipScenario(
        id = "move-semantics",
        title = "Move Semantics",
        description = "Understanding move semantics in Rust",
        relatedChapter = "ch04-ownership",
        steps = sampleSteps
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeContentProvider = FakeContentProvider(listOf(sampleScenario))
        viewModel = VisualizerViewModel(fakeContentProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `openScenario sets currentScenario and resets step index to 0`() = runTest {
        advanceUntilIdle()

        viewModel.openScenario(sampleScenario)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.currentScenario)
        assertEquals("move-semantics", state.currentScenario!!.id)
        assertEquals(0, state.currentStepIndex)
    }

    @Test
    fun `nextStep increments step index`() = runTest {
        advanceUntilIdle()

        viewModel.openScenario(sampleScenario)
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.currentStepIndex)

        viewModel.nextStep()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun `nextStep does nothing at last step`() = runTest {
        advanceUntilIdle()

        viewModel.openScenario(sampleScenario)
        advanceUntilIdle()

        // Navigate to last step (index 2)
        viewModel.nextStep()
        advanceUntilIdle()
        viewModel.nextStep()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.currentStepIndex)

        // Try to go past last step
        viewModel.nextStep()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun `previousStep decrements step index`() = runTest {
        advanceUntilIdle()

        viewModel.openScenario(sampleScenario)
        advanceUntilIdle()

        viewModel.nextStep()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.currentStepIndex)

        viewModel.previousStep()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun `previousStep does nothing at first step`() = runTest {
        advanceUntilIdle()

        viewModel.openScenario(sampleScenario)
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.currentStepIndex)

        viewModel.previousStep()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun `goBack clears currentScenario`() = runTest {
        advanceUntilIdle()

        viewModel.openScenario(sampleScenario)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.currentScenario)

        viewModel.goBack()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.currentScenario)
        assertEquals(0, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun `hasNext returns true when not at last step`() = runTest {
        advanceUntilIdle()

        viewModel.openScenario(sampleScenario)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasNext)
    }

    @Test
    fun `hasPrevious returns true when not at first step`() = runTest {
        advanceUntilIdle()

        viewModel.openScenario(sampleScenario)
        advanceUntilIdle()

        viewModel.nextStep()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasPrevious)
    }

    @Test
    fun `currentStep returns the correct step for current index`() = runTest {
        advanceUntilIdle()

        viewModel.openScenario(sampleScenario)
        advanceUntilIdle()

        val step0 = viewModel.uiState.value.currentStep
        assertNotNull(step0)
        assertEquals("Step 1: Create variable", step0!!.label)

        viewModel.nextStep()
        advanceUntilIdle()

        val step1 = viewModel.uiState.value.currentStep
        assertNotNull(step1)
        assertEquals("Step 2: Move ownership", step1!!.label)

        viewModel.nextStep()
        advanceUntilIdle()

        val step2 = viewModel.uiState.value.currentStep
        assertNotNull(step2)
        assertEquals("Step 3: Use after move", step2!!.label)
    }

    // --- Fake ---

    private class FakeContentProvider(
        private val scenarios: List<OwnershipScenario>
    ) : ContentProvider {

        override suspend fun loadVisualizationJson(filename: String): JSONObject? {
            if (filename == "index") {
                val indexJson = JSONObject()
                val arr = JSONArray()
                for (scenario in scenarios) {
                    val entry = JSONObject()
                    entry.put("id", scenario.id)
                    arr.put(entry)
                }
                indexJson.put("scenarios", arr)
                return indexJson
            }
            val scenario = scenarios.find { it.id == filename } ?: return null
            return buildScenarioJson(scenario)
        }

        private fun buildScenarioJson(scenario: OwnershipScenario): JSONObject {
            val json = JSONObject()
            json.put("id", scenario.id)
            json.put("title", scenario.title)
            json.put("description", scenario.description)
            json.put("relatedChapter", scenario.relatedChapter)

            val stepsArr = JSONArray()
            for (step in scenario.steps) {
                val stepJson = JSONObject()
                stepJson.put("label", step.label)
                stepJson.put("code", step.code)
                stepJson.put("annotation", step.annotation)
                stepJson.put("stackVariables", JSONArray())
                stepJson.put("heapAllocations", JSONArray())
                stepsArr.put(stepJson)
            }
            json.put("steps", stepsArr)
            return json
        }

        // Unused -- only visualization methods are needed by this ViewModel
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
        override suspend fun getRefactoringChallenges() = emptyList<RefactoringChallenge>()
        override suspend fun getRefactoringChallenge(id: String) = null
        override suspend fun getDocIndex() = emptyList<DocIndexEntry>()
        override suspend fun getDoc(typeId: String) = null
        override suspend fun loadProjectJson(filename: String) = null
    }
}
