package com.sylvester.rustsensei.viewmodel

import com.sylvester.rustsensei.data.InferenceConfigProvider
import com.sylvester.rustsensei.domain.SimulateExecutionUseCase
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.testdoubles.FakeInferenceEngine
import com.sylvester.rustsensei.testdoubles.FakeModelLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class PlaygroundViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeEngine: FakeInferenceEngine
    private lateinit var fakeLifecycle: FakeModelLifecycle
    private lateinit var fakeConfigProvider: FakeInferenceConfigProvider
    private lateinit var viewModel: PlaygroundViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeEngine = FakeInferenceEngine()
        fakeLifecycle = FakeModelLifecycle()
        fakeConfigProvider = FakeInferenceConfigProvider()

        val useCase = SimulateExecutionUseCase(fakeEngine, fakeLifecycle)
        viewModel = PlaygroundViewModel(useCase, fakeConfigProvider, fakeLifecycle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Initial state ---

    @Test
    fun `initial state has default code and empty output`() = runTest {
        val state = viewModel.uiState.value
        assertEquals(PlaygroundUiState.DEFAULT_CODE, state.code)
        assertEquals("", state.output)
        assertFalse(state.isRunning)
        assertEquals(0L, state.elapsedMs)
        assertNull(state.errorMessage)
    }

    // --- updateCode ---

    @Test
    fun `updateCode updates the code in state`() = runTest {
        viewModel.updateCode("fn main() { let x = 42; }")
        advanceUntilIdle()

        assertEquals("fn main() { let x = 42; }", viewModel.uiState.value.code)
    }

    // --- run ---

    @Test
    fun `run sets isRunning to true`() = runTest {
        fakeEngine.tokensToEmit = listOf("Hello, Rust!")

        viewModel.run()

        // The run() method sets isRunning=true synchronously before launching the coroutine
        assertTrue(viewModel.uiState.value.isRunning)

        advanceUntilIdle()
    }

    @Test
    fun `run updates output with streamed tokens`() = runTest {
        fakeEngine.tokensToEmit = listOf("Hello", ", ", "Rust!")

        viewModel.run()
        advanceUntilIdle()

        // The use case concatenates tokens and strips think tags
        assertEquals("Hello, Rust!", viewModel.uiState.value.output)
    }

    @Test
    fun `run sets isRunning to false and elapsedMs on completion`() = runTest {
        fakeEngine.tokensToEmit = listOf("output")

        viewModel.run()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRunning)
        assertTrue(state.elapsedMs >= 0)
        assertEquals("output", state.output)
    }

    @Test
    fun `run sets errorMessage on failure`() = runTest {
        fakeLifecycle.ensureLoadedResult = false

        viewModel.run()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRunning)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("not available"))
    }

    @Test
    fun `run does nothing when code is blank`() = runTest {
        fakeEngine.tokensToEmit = listOf("Should not appear")

        viewModel.updateCode("   ")
        viewModel.run()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.output)
        assertFalse(state.isRunning)
        assertEquals(0, fakeEngine.generateCallCount)
    }

    // --- stop ---

    @Test
    fun `stop cancels the running job and sets isRunning to false`() = runTest {
        fakeEngine.tokensToEmit = listOf("partial", " output")

        viewModel.run()
        // The run() method synchronously sets isRunning = true
        assertTrue(viewModel.uiState.value.isRunning)

        viewModel.stop()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRunning)
    }

    // --- clearOutput ---

    @Test
    fun `clearOutput clears output and error`() = runTest {
        fakeEngine.tokensToEmit = listOf("some output")

        viewModel.run()
        advanceUntilIdle()

        // Verify there is output
        assertEquals("some output", viewModel.uiState.value.output)
        assertTrue(viewModel.uiState.value.elapsedMs >= 0)

        viewModel.clearOutput()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.output)
        assertNull(state.errorMessage)
        assertEquals(0L, state.elapsedMs)
    }

    // --- Fakes ---

    private class FakeInferenceConfigProvider : InferenceConfigProvider {
        override fun loadInferenceConfig(): InferenceConfig = InferenceConfig()
    }
}
