package com.sylvester.rustsensei.domain

import app.cash.turbine.test
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.llm.InferenceEngine
import com.sylvester.rustsensei.testdoubles.FakeModelLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SimulateExecutionUseCaseTest {

    private lateinit var fakeEngine: ConfigCapturingFakeEngine
    private lateinit var fakeLifecycle: FakeModelLifecycle
    private lateinit var useCase: SimulateExecutionUseCase

    private val defaultConfig = InferenceConfig()

    @Before
    fun setup() {
        fakeEngine = ConfigCapturingFakeEngine()
        fakeLifecycle = FakeModelLifecycle()
        useCase = SimulateExecutionUseCase(fakeEngine, fakeLifecycle)
    }

    @Test
    fun `emits Output events progressively then Completed with elapsed time`() = runTest {
        fakeEngine.tokensToEmit = listOf("Hello", ", ", "world!")

        useCase("fn main() { println!(\"Hello, world!\"); }", defaultConfig).test {
            val o1 = awaitItem(); assertTrue(o1 is ExecutionEvent.Output)
            val o2 = awaitItem(); assertTrue(o2 is ExecutionEvent.Output)
            val o3 = awaitItem(); assertTrue(o3 is ExecutionEvent.Output)

            val completed = awaitItem() as ExecutionEvent.Completed
            assertEquals("Hello, world!", completed.fullOutput)
            assertTrue(completed.elapsedMs >= 0)

            awaitComplete()
        }
    }

    @Test
    fun `emits Error when model not available`() = runTest {
        fakeLifecycle.ensureLoadedResult = false

        useCase("fn main() {}", defaultConfig).test {
            val event = awaitItem() as ExecutionEvent.Error
            assertTrue(event.message.contains("not available"))
            awaitComplete()
        }
    }

    @Test
    fun `handles generation failure`() = runTest {
        fakeEngine.shouldFailGenerate = true

        useCase("fn main() {}", defaultConfig).test {
            val event = awaitItem() as ExecutionEvent.Error
            assertTrue(event.message.contains("Fake generation error"))
            awaitComplete()
        }
    }

    @Test
    fun `uses low temperature 0_1f for deterministic output`() = runTest {
        fakeEngine.tokensToEmit = listOf("output")

        useCase("fn main() {}", defaultConfig).test {
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(0.1f, fakeEngine.lastConfig?.temperature)
    }

    @Test
    fun `clears cache before generation`() = runTest {
        fakeEngine.tokensToEmit = listOf("output")

        useCase("fn main() {}", defaultConfig).test {
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(fakeEngine.clearCacheCalled)
        assertTrue(
            "clearCache must be called before generate",
            fakeEngine.clearCacheCalledBeforeGenerate
        )
    }

    @Test
    fun `does not call engine when model unavailable`() = runTest {
        fakeLifecycle.ensureLoadedResult = false

        useCase("fn main() {}", defaultConfig).test {
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, fakeLifecycle.ensureLoadedCallCount)
        assertEquals(0, fakeEngine.generateCallCount)
    }

    @Test
    fun `Output events contain accumulated text`() = runTest {
        fakeEngine.tokensToEmit = listOf("line1\n", "line2")

        useCase("code", defaultConfig).test {
            val o1 = awaitItem() as ExecutionEvent.Output
            assertEquals("line1\n", o1.text)

            val o2 = awaitItem() as ExecutionEvent.Output
            assertEquals("line1\nline2", o2.text)

            val completed = awaitItem() as ExecutionEvent.Completed
            assertEquals("line1\nline2", completed.fullOutput)

            awaitComplete()
        }
    }

    /**
     * Extended fake that captures the [InferenceConfig] passed to [generate]
     * and tracks [clearCache] call ordering relative to [generate].
     */
    private class ConfigCapturingFakeEngine : InferenceEngine {
        var loaded = false
        var shouldFailGenerate = false
        var tokensToEmit: List<String> = listOf("Hello", " ", "World")
        var generateCallCount = 0
            private set
        var lastConfig: InferenceConfig? = null
            private set
        var clearCacheCalled = false
            private set
        var clearCacheCalledBeforeGenerate = false
            private set

        override suspend fun loadModel(modelPath: String, contextSize: Int): Boolean {
            loaded = true
            return true
        }

        override fun generate(
            prompt: String,
            config: InferenceConfig,
            onStats: ((Float, Float, Float) -> Unit)?
        ): Flow<String> = flow {
            generateCallCount++
            lastConfig = config
            if (clearCacheCalled && generateCallCount == 1) {
                clearCacheCalledBeforeGenerate = true
            }
            if (shouldFailGenerate) throw RuntimeException("Fake generation error")
            tokensToEmit.forEach { emit(it) }
            onStats?.invoke(0f, 10f, 50f)
        }

        override fun stopGeneration() {}

        override fun clearCache() {
            clearCacheCalled = true
        }

        override suspend fun unloadModel() {
            loaded = false
        }

        override fun isModelLoaded(): Boolean = loaded

        override fun getActiveBackend(): String = "FAKE"
    }
}
