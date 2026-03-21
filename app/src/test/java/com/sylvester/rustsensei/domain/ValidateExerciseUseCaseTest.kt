package com.sylvester.rustsensei.domain

import app.cash.turbine.test
import com.sylvester.rustsensei.content.ExerciseData
import com.sylvester.rustsensei.testdoubles.FakeInferenceEngine
import com.sylvester.rustsensei.testdoubles.FakeModelLifecycle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidateExerciseUseCaseTest {

    private lateinit var fakeEngine: FakeInferenceEngine
    private lateinit var fakeLifecycle: FakeModelLifecycle
    private lateinit var useCase: ValidateExerciseUseCase

    private val sampleExercise = ExerciseData(
        id = "test-1",
        title = "Hello World",
        category = "basics",
        difficulty = "beginner",
        description = "Print hello world in Rust",
        instructions = "Use println! macro to print 'Hello, world!'",
        starterCode = "fn main() {\n    // TODO\n}",
        hints = listOf("Use println! macro"),
        solution = "fn main() {\n    println!(\"Hello, world!\");\n}",
        explanation = "The println! macro prints to stdout.",
        relatedBookSection = "ch01-02"
    )

    @Before
    fun setup() {
        fakeEngine = FakeInferenceEngine()
        fakeLifecycle = FakeModelLifecycle()
        useCase = ValidateExerciseUseCase(fakeEngine, fakeLifecycle)
    }

    @Test
    fun `emits tokens progressively then Completed when model responds CORRECT`() = runTest {
        fakeEngine.tokensToEmit = listOf("CORRECT", " - ", "Good job!")

        useCase(sampleExercise, "fn main() { println!(\"Hello, world!\"); }").test {
            val t1 = awaitItem(); assertTrue(t1 is ValidationEvent.Token)
            val t2 = awaitItem(); assertTrue(t2 is ValidationEvent.Token)
            val t3 = awaitItem(); assertTrue(t3 is ValidationEvent.Token)

            val completed = awaitItem() as ValidationEvent.Completed
            assertTrue(completed.isCorrect)
            assertEquals("CORRECT - Good job!", completed.fullText)

            awaitComplete()
        }
    }

    @Test
    fun `marks as incorrect when model says INCORRECT`() = runTest {
        fakeEngine.tokensToEmit = listOf("INCORRECT", " - ", "Missing semicolon")

        useCase(sampleExercise, "fn main() {}").test {
            skipItems(3) // skip token events

            val completed = awaitItem() as ValidationEvent.Completed
            assertFalse(completed.isCorrect)

            awaitComplete()
        }
    }

    @Test
    fun `emits Error when model is not available`() = runTest {
        fakeLifecycle.ensureLoadedResult = false

        useCase(sampleExercise, "some code").test {
            val event = awaitItem() as ValidationEvent.Error
            assertTrue(event.message.contains("not available"))
            awaitComplete()
        }
    }

    @Test
    fun `emits Error when engine throws during generation`() = runTest {
        fakeEngine.shouldFailGenerate = true

        useCase(sampleExercise, "some code").test {
            val event = awaitItem() as ValidationEvent.Error
            assertTrue(event.message.contains("Fake generation error"))
            awaitComplete()
        }
    }

    @Test
    fun `calls ensureLoaded before generating`() = runTest {
        fakeEngine.tokensToEmit = listOf("CORRECT")

        useCase(sampleExercise, "code").test {
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, fakeLifecycle.ensureLoadedCallCount)
        assertEquals(1, fakeEngine.generateCallCount)
    }

    @Test
    fun `does not call engine when model unavailable`() = runTest {
        fakeLifecycle.ensureLoadedResult = false

        useCase(sampleExercise, "code").test {
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, fakeLifecycle.ensureLoadedCallCount)
        assertEquals(0, fakeEngine.generateCallCount)
    }
}
