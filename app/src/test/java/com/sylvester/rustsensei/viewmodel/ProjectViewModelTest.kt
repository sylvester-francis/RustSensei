package com.sylvester.rustsensei.viewmodel

import com.sylvester.rustsensei.content.ContentProvider
import com.sylvester.rustsensei.content.DocIndexEntry
import com.sylvester.rustsensei.content.Project
import com.sylvester.rustsensei.content.ProjectStep
import com.sylvester.rustsensei.content.RefactoringChallenge
import com.sylvester.rustsensei.data.PathProgress
import com.sylvester.rustsensei.data.ProgressDao
import com.sylvester.rustsensei.data.ProjectProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeContentProvider: FakeContentProvider
    private lateinit var fakeProgressDao: FakeProgressDao
    private lateinit var viewModel: ProjectViewModel

    private val sampleSteps = listOf(
        ProjectStep(
            id = "step-1",
            title = "Create struct",
            instructions = "Define a Rustacean struct",
            starterCode = "struct Rustacean { }",
            relatedChapter = "ch05-structs"
        ),
        ProjectStep(
            id = "step-2",
            title = "Add methods",
            instructions = "Implement methods on the struct",
            starterCode = "impl Rustacean { }",
            relatedChapter = "ch05-structs"
        ),
        ProjectStep(
            id = "step-3",
            title = "Test it",
            instructions = "Write unit tests",
            starterCode = "#[cfg(test)] mod tests { }",
            relatedChapter = null
        )
    )

    private val sampleProject = Project(
        id = "todo-cli",
        title = "Todo CLI",
        description = "Build a command-line todo app",
        difficulty = "beginner",
        estimatedHours = 4,
        concepts = listOf("structs", "enums", "pattern-matching"),
        steps = sampleSteps
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeContentProvider = FakeContentProvider(listOf(sampleProject))
        fakeProgressDao = FakeProgressDao()
        viewModel = ProjectViewModel(fakeContentProvider, fakeProgressDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `openProject sets currentProject and loads step progress`() = runTest {
        advanceUntilIdle()

        // Seed some prior progress
        fakeProgressDao.savedProjectProgress["todo-cli"] = listOf(
            ProjectProgress(
                id = "todo-cli_step-1",
                projectId = "todo-cli",
                stepId = "step-1",
                isCompleted = true,
                userCode = "done",
                completedAt = 1000L
            )
        )

        viewModel.openProject(sampleProject)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.currentProject)
        assertEquals("todo-cli", state.currentProject!!.id)
        assertEquals(0, state.currentStepIndex)
        assertTrue(state.completedStepIds.contains("step-1"))
        assertEquals("struct Rustacean { }", state.userCode)
    }

    @Test
    fun `selectStep changes current step index and sets starter code`() = runTest {
        advanceUntilIdle()

        viewModel.openProject(sampleProject)
        advanceUntilIdle()

        viewModel.selectStep(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.currentStepIndex)
        assertEquals("impl Rustacean { }", state.userCode)
    }

    @Test
    fun `updateCode updates userCode in state`() = runTest {
        advanceUntilIdle()

        viewModel.openProject(sampleProject)
        advanceUntilIdle()

        viewModel.updateCode("fn main() { println!(\"hello\"); }")
        advanceUntilIdle()

        assertEquals("fn main() { println!(\"hello\"); }", viewModel.uiState.value.userCode)
    }

    @Test
    fun `completeStep persists progress to DAO`() = runTest {
        advanceUntilIdle()

        viewModel.openProject(sampleProject)
        advanceUntilIdle()

        viewModel.completeStep()
        advanceUntilIdle()

        val saved = fakeProgressDao.savedUpserts
        assertTrue(saved.isNotEmpty())
        val progress = saved.first()
        assertEquals("todo-cli", progress.projectId)
        assertEquals("step-1", progress.stepId)
        assertTrue(progress.isCompleted)
    }

    @Test
    fun `completeStep adds step ID to completedStepIds`() = runTest {
        advanceUntilIdle()

        viewModel.openProject(sampleProject)
        advanceUntilIdle()

        viewModel.completeStep()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.completedStepIds.contains("step-1"))
    }

    @Test
    fun `goBack clears currentProject`() = runTest {
        advanceUntilIdle()

        viewModel.openProject(sampleProject)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.currentProject)

        viewModel.goBack()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.currentProject)
        assertEquals(0, viewModel.uiState.value.currentStepIndex)
        assertEquals("", viewModel.uiState.value.userCode)
    }

    // --- Fakes ---

    private class FakeContentProvider(
        private val projects: List<Project>
    ) : ContentProvider {

        override suspend fun loadProjectJson(filename: String): JSONObject? {
            if (filename == "index") {
                val json = JSONObject()
                val arr = JSONArray()
                for (project in projects) {
                    val entry = JSONObject()
                    entry.put("id", project.id)
                    arr.put(entry)
                }
                json.put("projects", arr)
                return json
            }
            val project = projects.find { it.id == filename } ?: return null
            return buildProjectJson(project)
        }

        private fun buildProjectJson(project: Project): JSONObject {
            val json = JSONObject()
            json.put("id", project.id)
            json.put("title", project.title)
            json.put("description", project.description)
            json.put("difficulty", project.difficulty)
            json.put("estimatedHours", project.estimatedHours)

            val conceptsArr = JSONArray()
            for (concept in project.concepts) {
                conceptsArr.put(concept)
            }
            json.put("concepts", conceptsArr)

            val stepsArr = JSONArray()
            for (step in project.steps) {
                val stepJson = JSONObject()
                stepJson.put("id", step.id)
                stepJson.put("title", step.title)
                stepJson.put("instructions", step.instructions)
                stepJson.put("starterCode", step.starterCode)
                stepJson.put("relatedChapter", step.relatedChapter ?: "")
                stepsArr.put(stepJson)
            }
            json.put("steps", stepsArr)
            return json
        }

        // Unused -- only project methods are needed by this ViewModel
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
        override suspend fun loadVisualizationJson(filename: String) = null
    }

    private class FakeProgressDao : ProgressDao {
        val savedUpserts = mutableListOf<ProjectProgress>()
        val savedProjectProgress = mutableMapOf<String, List<ProjectProgress>>()

        override suspend fun upsertProjectProgress(progress: ProjectProgress) {
            savedUpserts.add(progress)
        }

        override suspend fun getProjectProgress(projectId: String): List<ProjectProgress> =
            savedProjectProgress[projectId] ?: emptyList()

        override fun observeProjectProgress(projectId: String): Flow<List<ProjectProgress>> =
            MutableStateFlow(savedProjectProgress[projectId] ?: emptyList())

        // Stubs for unused DAO methods
        override suspend fun upsertPathProgress(progress: PathProgress) = Unit
        override suspend fun getPathProgress(pathId: String) = emptyList<PathProgress>()
        override fun observePathProgress(pathId: String): Flow<List<PathProgress>> = MutableStateFlow(emptyList())
        override fun observeAllPathProgress(): Flow<List<PathProgress>> = MutableStateFlow(emptyList())
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
    }
}
