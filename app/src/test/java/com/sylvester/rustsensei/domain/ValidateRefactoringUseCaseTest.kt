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

class ValidateRefactoringUseCaseTest {

    private lateinit var fakeEngine: ConfigCapturingFakeEngine
    private lateinit var fakeLifecycle: FakeModelLifecycle
    private lateinit var useCase: ValidateRefactoringUseCase

    private val defaultConfig = InferenceConfig()

    @Before
    fun setup() {
        fakeEngine = ConfigCapturingFakeEngine()
        fakeLifecycle = FakeModelLifecycle()
        useCase = ValidateRefactoringUseCase(fakeEngine, fakeLifecycle)
    }

    @Test
    fun `emits Token events then Completed with parsed score`() = runTest {
        fakeEngine.tokensToEmit = listOf("SCORE: ", "85/100", "\nGood use of iterators.")

        useCase("original", "refactored", "idiomatic", "criteria", defaultConfig).test {
            val t1 = awaitItem(); assertTrue(t1 is RefactoringEvent.Token)
            val t2 = awaitItem(); assertTrue(t2 is RefactoringEvent.Token)
            val t3 = awaitItem(); assertTrue(t3 is RefactoringEvent.Token)

            val completed = awaitItem() as RefactoringEvent.Completed
            assertEquals(85, completed.score)
            assertEquals("SCORE: 85/100\nGood use of iterators.", completed.fullText)

            awaitComplete()
        }
    }

    @Test
    fun `parses SCORE 85 slash 100 format correctly`() = runTest {
        fakeEngine.tokensToEmit = listOf("SCORE: 85/100\nExcellent refactoring.")

        useCase("original", "user", "idiomatic", "criteria", defaultConfig).test {
            awaitItem() // Token

            val completed = awaitItem() as RefactoringEvent.Completed
            assertEquals(85, completed.score)

            awaitComplete()
        }
    }

    @Test
    fun `parses Score colon 90 format correctly`() = runTest {
        fakeEngine.tokensToEmit = listOf("Score: 90\nVery idiomatic code.")

        useCase("original", "user", "idiomatic", "criteria", defaultConfig).test {
            awaitItem() // Token

            val completed = awaitItem() as RefactoringEvent.Completed
            assertEquals(90, completed.score)

            awaitComplete()
        }
    }

    @Test
    fun `defaults to 50 when no score pattern found`() = runTest {
        fakeEngine.tokensToEmit = listOf("Your code looks good but I can't score it.")

        useCase("original", "user", "idiomatic", "criteria", defaultConfig).test {
            awaitItem() // Token

            val completed = awaitItem() as RefactoringEvent.Completed
            assertEquals(50, completed.score)

            awaitComplete()
        }
    }

    @Test
    fun `clamps score to 0 when negative pattern not possible but 0 is valid`() = runTest {
        fakeEngine.tokensToEmit = listOf("SCORE: 0/100\nNeeds complete rewrite.")

        useCase("original", "user", "idiomatic", "criteria", defaultConfig).test {
            awaitItem() // Token

            val completed = awaitItem() as RefactoringEvent.Completed
            assertEquals(0, completed.score)

            awaitComplete()
        }
    }

    @Test
    fun `clamps score to 100 when value exceeds range`() = runTest {
        // 150/100 -- regex matches \d{1,3}/100, so "150" is captured, then clamped
        fakeEngine.tokensToEmit = listOf("150/100\nOverkill.")

        useCase("original", "user", "idiomatic", "criteria", defaultConfig).test {
            awaitItem() // Token

            val completed = awaitItem() as RefactoringEvent.Completed
            assertEquals(100, completed.score)

            awaitComplete()
        }
    }

    @Test
    fun `emits Error when model not available`() = runTest {
        fakeLifecycle.ensureLoadedResult = false

        useCase("original", "user", "idiomatic", "criteria", defaultConfig).test {
            val event = awaitItem() as RefactoringEvent.Error
            assertTrue(event.message.contains("not available"))
            awaitComplete()
        }
    }

    @Test
    fun `uses temperature 0_3f for scoring`() = runTest {
        fakeEngine.tokensToEmit = listOf("SCORE: 70/100")

        useCase("original", "user", "idiomatic", "criteria", defaultConfig).test {
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(0.3f, fakeEngine.lastConfig?.temperature)
    }

    @Test
    fun `handles engine generation failure gracefully`() = runTest {
        fakeEngine.shouldFailGenerate = true

        useCase("original", "user", "idiomatic", "criteria", defaultConfig).test {
            val event = awaitItem() as RefactoringEvent.Error
            assertTrue(event.message.contains("Fake generation error"))
            awaitComplete()
        }
    }

    @Test
    fun `does not call engine when model unavailable`() = runTest {
        fakeLifecycle.ensureLoadedResult = false

        useCase("original", "user", "idiomatic", "criteria", defaultConfig).test {
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, fakeLifecycle.ensureLoadedCallCount)
        assertEquals(0, fakeEngine.generateCallCount)
    }

    @Test
    fun `score 100 slash 100 parses as 100`() = runTest {
        fakeEngine.tokensToEmit = listOf("SCORE: 100/100\nPerfect.")

        useCase("original", "user", "idiomatic", "criteria", defaultConfig).test {
            awaitItem() // Token

            val completed = awaitItem() as RefactoringEvent.Completed
            assertEquals(100, completed.score)

            awaitComplete()
        }
    }

    /**
     * Extended fake that captures the [InferenceConfig] passed to [generate].
     */
    private class ConfigCapturingFakeEngine : InferenceEngine {
        var shouldFailGenerate = false
        var tokensToEmit: List<String> = listOf("Hello", " ", "World")
        var generateCallCount = 0
            private set
        var lastConfig: InferenceConfig? = null
            private set

        override suspend fun loadModel(modelPath: String, contextSize: Int): Boolean = true

        override fun generate(
            prompt: String,
            config: InferenceConfig,
            onStats: ((Float, Float, Float) -> Unit)?
        ): Flow<String> = flow {
            generateCallCount++
            lastConfig = config
            if (shouldFailGenerate) throw RuntimeException("Fake generation error")
            tokensToEmit.forEach { emit(it) }
            onStats?.invoke(0f, 10f, 50f)
        }

        override fun stopGeneration() {}
        override fun clearCache() {}
        override suspend fun unloadModel() {}
        override fun isModelLoaded(): Boolean = true
        override fun getActiveBackend(): String = "FAKE"
    }
}
