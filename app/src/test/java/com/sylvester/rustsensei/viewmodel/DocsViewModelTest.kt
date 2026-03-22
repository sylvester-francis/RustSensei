package com.sylvester.rustsensei.viewmodel

import com.sylvester.rustsensei.content.ContentProvider
import com.sylvester.rustsensei.content.DocEntry
import com.sylvester.rustsensei.content.DocIndexEntry
import com.sylvester.rustsensei.content.DocMethod
import com.sylvester.rustsensei.content.RefactoringChallenge
import com.sylvester.rustsensei.data.LearningPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class DocsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeContentProvider: FakeContentProvider
    private lateinit var viewModel: DocsViewModel

    private val sampleDocIndex = listOf(
        DocIndexEntry(id = "string", name = "String", module = "std::string"),
        DocIndexEntry(id = "vec", name = "Vec", module = "std::vec"),
        DocIndexEntry(id = "hashmap", name = "HashMap", module = "std::collections"),
        DocIndexEntry(id = "option", name = "Option", module = "std::option")
    )

    private val sampleDocEntry = DocEntry(
        id = "string",
        typeName = "String",
        module = "std::string",
        signature = "pub struct String",
        description = "A growable UTF-8 encoded string",
        methods = listOf(
            DocMethod(
                name = "new",
                signature = "pub fn new() -> String",
                description = "Creates an empty String",
                example = "let s = String::new();"
            )
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeContentProvider = FakeContentProvider(sampleDocIndex, sampleDocEntry)
        viewModel = DocsViewModel(fakeContentProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads doc index`() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(4, state.docIndex.size)
        assertEquals("string", state.docIndex[0].id)
        assertEquals("vec", state.docIndex[1].id)
    }

    @Test
    fun `initial state has isLoading true then false after load`() = runTest {
        // Verify the default state starts with isLoading=true
        val defaultState = DocsUiState()
        assertTrue("Default DocsUiState should have isLoading=true", defaultState.isLoading)

        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `openDoc sets currentDoc`() = runTest {
        advanceUntilIdle()

        viewModel.openDoc("string")
        advanceUntilIdle()

        val currentDoc = viewModel.uiState.value.currentDoc
        assertNotNull(currentDoc)
        assertEquals("string", currentDoc!!.id)
        assertEquals("String", currentDoc.typeName)
        assertEquals("std::string", currentDoc.module)
    }

    @Test
    fun `goBack clears currentDoc`() = runTest {
        advanceUntilIdle()

        viewModel.openDoc("string")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.currentDoc)

        viewModel.goBack()

        assertNull(viewModel.uiState.value.currentDoc)
    }

    @Test
    fun `search filters index by query`() = runTest {
        advanceUntilIdle()

        viewModel.search("Vec")
        advanceUntilIdle()

        val filtered = viewModel.uiState.value.filteredIndex
        assertEquals(1, filtered.size)
        assertEquals("vec", filtered[0].id)
    }

    @Test
    fun `search with empty query shows all types`() = runTest {
        advanceUntilIdle()

        viewModel.search("Vec")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.filteredIndex.size)

        viewModel.search("")
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.filteredIndex.size)
    }

    @Test
    fun `search is case-insensitive`() = runTest {
        advanceUntilIdle()

        viewModel.search("hashmap")
        advanceUntilIdle()

        val filtered = viewModel.uiState.value.filteredIndex
        assertEquals(1, filtered.size)
        assertEquals("hashmap", filtered[0].id)
    }

    // --- Fake ---

    private class FakeContentProvider(
        private val docIndex: List<DocIndexEntry>,
        private val docEntry: DocEntry?
    ) : ContentProvider {
        override suspend fun getDocIndex(): List<DocIndexEntry> = docIndex
        override suspend fun getDoc(typeId: String): DocEntry? =
            if (docEntry?.id == typeId) docEntry else null

        // Unused -- only doc methods are needed by this ViewModel
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
        override suspend fun loadVisualizationJson(filename: String) = null
        override suspend fun loadProjectJson(filename: String) = null
    }
}
