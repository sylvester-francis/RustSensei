package com.sylvester.rustsensei.viewmodel

import com.sylvester.rustsensei.data.InferenceConfigProvider
import com.sylvester.rustsensei.domain.CompileCodeUseCase
import com.sylvester.rustsensei.domain.SimulateExecutionUseCase
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.network.PlaygroundResponse
import com.sylvester.rustsensei.testdoubles.FakeInferenceEngine
import com.sylvester.rustsensei.testdoubles.FakeModelLifecycle
import com.sylvester.rustsensei.testdoubles.FakePlaygroundService
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
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class PlaygroundViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeEngine: FakeInferenceEngine
    private lateinit var fakeLifecycle: FakeModelLifecycle
    private lateinit var fakeConfigProvider: FakeInferenceConfigProvider
    private lateinit var fakePlaygroundService: FakePlaygroundService
    private lateinit var viewModel: PlaygroundViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeEngine = FakeInferenceEngine()
        fakeLifecycle = FakeModelLifecycle()
        fakeConfigProvider = FakeInferenceConfigProvider()
        fakePlaygroundService = FakePlaygroundService()

        val simulateUseCase = SimulateExecutionUseCase(fakeEngine, fakeLifecycle)
        val compileUseCase = CompileCodeUseCase(fakePlaygroundService)
        viewModel = PlaygroundViewModel(simulateUseCase, compileUseCase, fakeConfigProvider, fakeLifecycle)
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
        assertFalse(state.isCompiling)
        assertEquals(0L, state.elapsedMs)
        assertNull(state.errorMessage)
        assertNull(state.compilationSuccess)
        assertEquals(OutputSource.NONE, state.outputSource)
    }

    // --- updateCode ---

    @Test
    fun `updateCode updates the code in state`() = runTest {
        viewModel.updateCode("fn main() { let x = 42; }")
        advanceUntilIdle()

        assertEquals("fn main() { let x = 42; }", viewModel.uiState.value.code)
    }

    // --- run (AI simulation) ---

    @Test
    fun `run sets isRunning to true`() = runTest {
        fakeEngine.tokensToEmit = listOf("Hello, Rust!")

        viewModel.run()

        assertTrue(viewModel.uiState.value.isRunning)
        assertEquals(OutputSource.AI_SIMULATION, viewModel.uiState.value.outputSource)

        advanceUntilIdle()
    }

    @Test
    fun `run updates output with streamed tokens`() = runTest {
        fakeEngine.tokensToEmit = listOf("Hello", ", ", "Rust!")

        viewModel.run()
        advanceUntilIdle()

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

        assertEquals("some output", viewModel.uiState.value.output)
        assertTrue(viewModel.uiState.value.elapsedMs >= 0)

        viewModel.clearOutput()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.output)
        assertNull(state.errorMessage)
        assertEquals(0L, state.elapsedMs)
        assertNull(state.compilationSuccess)
        assertEquals(OutputSource.NONE, state.outputSource)
    }

    // --- compile ---

    @Test
    fun `compile sets isCompiling and outputSource to COMPILATION`() = runTest {
        fakePlaygroundService.responseToReturn = PlaygroundResponse(true, "output", "")

        viewModel.compile()

        assertTrue(viewModel.uiState.value.isCompiling)
        assertEquals(OutputSource.COMPILATION, viewModel.uiState.value.outputSource)

        advanceUntilIdle()
    }

    @Test
    fun `compile successful shows stdout in output`() = runTest {
        fakePlaygroundService.responseToReturn = PlaygroundResponse(
            success = true,
            stdout = "Hello, world!\n",
            stderr = ""
        )

        viewModel.compile()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCompiling)
        assertEquals("Hello, world!\n", state.output)
        assertTrue(state.compilationSuccess!!)
        assertTrue(state.elapsedMs >= 0)
        assertEquals(OutputSource.COMPILATION, state.outputSource)
    }

    @Test
    fun `compile failure shows stderr in output`() = runTest {
        fakePlaygroundService.responseToReturn = PlaygroundResponse(
            success = false,
            stdout = "",
            stderr = "error[E0308]: mismatched types"
        )

        viewModel.compile()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCompiling)
        assertTrue(state.output.contains("E0308"))
        assertFalse(state.compilationSuccess!!)
    }

    @Test
    fun `compile with no internet shows error message`() = runTest {
        fakePlaygroundService.shouldThrow = UnknownHostException("No host")

        viewModel.compile()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCompiling)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("No internet"))
    }

    @Test
    fun `compile does nothing when code is blank`() = runTest {
        viewModel.updateCode("   ")
        viewModel.compile()
        advanceUntilIdle()

        assertEquals(0, fakePlaygroundService.executeCallCount)
        assertFalse(viewModel.uiState.value.isCompiling)
    }

    @Test
    fun `stopCompile cancels compile job`() = runTest {
        fakePlaygroundService.responseToReturn = PlaygroundResponse(true, "output", "")

        viewModel.compile()
        assertTrue(viewModel.uiState.value.isCompiling)

        viewModel.stopCompile()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCompiling)
    }

    // --- Fakes ---

    private class FakeInferenceConfigProvider : InferenceConfigProvider {
        override fun loadInferenceConfig(): InferenceConfig = InferenceConfig()
    }
}
