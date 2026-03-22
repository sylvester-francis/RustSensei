package com.sylvester.rustsensei.domain

import app.cash.turbine.test
import com.sylvester.rustsensei.content.*
import com.sylvester.rustsensei.data.LearningPath
import com.sylvester.rustsensei.data.Quiz
import com.sylvester.rustsensei.data.QuizIndexEntry
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.testdoubles.FakeInferenceEngine
import com.sylvester.rustsensei.testdoubles.FakeModelLifecycle
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExplainErrorUseCaseTest {

    private lateinit var fakeEngine: FakeInferenceEngine
    private lateinit var fakeLifecycle: FakeModelLifecycle
    private lateinit var fakeContentProvider: FakeContentProvider
    private lateinit var useCase: ExplainErrorUseCase

    private val defaultConfig = InferenceConfig()

    @Before
    fun setup() {
        fakeEngine = FakeInferenceEngine()
        fakeLifecycle = FakeModelLifecycle()
        fakeContentProvider = FakeContentProvider()
        useCase = ExplainErrorUseCase(fakeContentProvider, fakeEngine, fakeLifecycle)
    }

    @Test
    fun `emits Token events progressively then Completed`() = runTest {
        fakeEngine.tokensToEmit = listOf("This ", "error ", "means...")

        useCase("error[E0382]: borrow of moved value", defaultConfig).test {
            val t1 = awaitItem(); assertTrue(t1 is ErrorExplanationEvent.Token)
            val t2 = awaitItem(); assertTrue(t2 is ErrorExplanationEvent.Token)
            val t3 = awaitItem(); assertTrue(t3 is ErrorExplanationEvent.Token)

            val completed = awaitItem() as ErrorExplanationEvent.Completed
            assertEquals("This error means...", completed.fullText)
            assertTrue(completed.elapsedMs >= 0)

            awaitComplete()
        }
    }

    @Test
    fun `emits Error when model not available`() = runTest {
        fakeLifecycle.ensureLoadedResult = false

        useCase("error[E0382]: borrow of moved value", defaultConfig).test {
            val event = awaitItem() as ErrorExplanationEvent.Error
            assertTrue(event.message.contains("not available"))
            awaitComplete()
        }
    }

    @Test
    fun `enriches with bundled reference data when error code matches`() = runTest {
        val referenceJson = JSONObject().apply {
            put("code", "E0382")
            put("title", "Borrow of moved value")
            put("explanation", "A value was used after being moved.")
            put("fixes", JSONArray().apply {
                put(JSONObject().apply {
                    put("description", "Clone the value")
                    put("code", "let y = x.clone();")
                })
            })
            put("tip", "Use references instead of moves when possible.")
        }
        fakeContentProvider.referenceItems["compiler-errors:E0382"] = referenceJson
        fakeEngine.tokensToEmit = listOf("Explanation")

        useCase("error[E0382]: borrow of moved value", defaultConfig).test {
            awaitItem() // Token
            val completed = awaitItem() as ErrorExplanationEvent.Completed
            assertEquals("Explanation", completed.fullText)
            awaitComplete()
        }

        // The engine should have been called (model was available)
        assertEquals(1, fakeEngine.generateCallCount)
    }

    @Test
    fun `works without reference data when code not in bundled set`() = runTest {
        // FakeContentProvider returns null by default for getReferenceItem
        fakeEngine.tokensToEmit = listOf("Generic ", "explanation")

        useCase("error[E0999]: unknown error", defaultConfig).test {
            awaitItem() // Token 1
            awaitItem() // Token 2

            val completed = awaitItem() as ErrorExplanationEvent.Completed
            assertEquals("Generic explanation", completed.fullText)

            awaitComplete()
        }
    }

    @Test
    fun `handles engine generation failure gracefully`() = runTest {
        fakeEngine.shouldFailGenerate = true

        useCase("error[E0382]: borrow of moved value", defaultConfig).test {
            val event = awaitItem() as ErrorExplanationEvent.Error
            assertTrue(event.message.contains("Fake generation error"))
            awaitComplete()
        }
    }

    @Test
    fun `does not call engine when model unavailable`() = runTest {
        fakeLifecycle.ensureLoadedResult = false

        useCase("error[E0382]", defaultConfig).test {
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, fakeLifecycle.ensureLoadedCallCount)
        assertEquals(0, fakeEngine.generateCallCount)
    }

    @Test
    fun `works with input that has no error code`() = runTest {
        fakeEngine.tokensToEmit = listOf("No ", "code ", "found")

        useCase("some error without code pattern", defaultConfig).test {
            awaitItem() // Token 1
            awaitItem() // Token 2
            awaitItem() // Token 3

            val completed = awaitItem() as ErrorExplanationEvent.Completed
            assertEquals("No code found", completed.fullText)

            awaitComplete()
        }
    }

    /**
     * Minimal fake [ContentProvider] for testing. Returns null for
     * [getReferenceItem] unless explicitly configured via [referenceItems].
     */
    private class FakeContentProvider : ContentProvider {
        val referenceItems = mutableMapOf<String, JSONObject>()

        override suspend fun getReferenceItem(sectionId: String, itemId: String): JSONObject? {
            return referenceItems["$sectionId:$itemId"]
        }

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
