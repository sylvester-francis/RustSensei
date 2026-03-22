package com.sylvester.rustsensei.widget

import com.sylvester.rustsensei.data.FlashCard
import com.sylvester.rustsensei.data.FlashCardDao
import com.sylvester.rustsensei.data.ProgressRepository
import com.sylvester.rustsensei.data.ProgressDao
import com.sylvester.rustsensei.data.BookProgress
import com.sylvester.rustsensei.data.DailyChallengeResult
import com.sylvester.rustsensei.data.ExerciseProgress
import com.sylvester.rustsensei.data.LearningStats
import com.sylvester.rustsensei.data.PathProgress
import com.sylvester.rustsensei.data.ProjectProgress
import com.sylvester.rustsensei.data.QuizResult
import com.sylvester.rustsensei.data.RefactoringResult
import com.sylvester.rustsensei.data.UserNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetDataProviderTest {

    // -- Fake FlashCardDao --

    private class FakeFlashCardDao(
        private val dueCardCount: Int = 0
    ) : FlashCardDao {
        override suspend fun insert(card: FlashCard): Long = 0
        override suspend fun insertAll(cards: List<FlashCard>) {}
        override suspend fun update(card: FlashCard) {}
        override suspend fun getDueCards(now: Long, limit: Int): List<FlashCard> = emptyList()
        override fun getDueCardCount(now: Long): Flow<Int> = flowOf(dueCardCount)
        override suspend fun getDueCardCountSync(now: Long): Int = dueCardCount
        override fun getTotalCardCount(): Flow<Int> = flowOf(0)
        override suspend fun getCardsByCategory(category: String): List<FlashCard> = emptyList()
        override suspend fun getCategories(): List<String> = emptyList()
        override suspend fun deleteAll() {}
    }

    // -- Fake ProgressDao --

    private class FakeProgressDao(
        private val learningStats: LearningStats? = null
    ) : ProgressDao {
        override suspend fun upsertBookProgress(progress: BookProgress) {}
        override suspend fun getBookProgress(sectionId: String): BookProgress? = null
        override fun getChapterProgress(chapterId: String): Flow<List<BookProgress>> = flowOf(emptyList())
        override fun getCompletedSectionsCount(): Flow<Int> = flowOf(0)
        override fun getCompletedChaptersCount(): Flow<Int> = flowOf(0)
        override fun getBookmarks(): Flow<List<BookProgress>> = flowOf(emptyList())
        override suspend fun getLastReadSection(): BookProgress? = null
        override suspend fun upsertExerciseProgress(progress: ExerciseProgress) {}
        override suspend fun getExerciseProgress(exerciseId: String): ExerciseProgress? = null
        override fun getExerciseProgressByCategory(category: String): Flow<List<ExerciseProgress>> = flowOf(emptyList())
        override fun getCompletedExercisesCount(): Flow<Int> = flowOf(0)
        override suspend fun getLastIncompleteExercise(): ExerciseProgress? = null
        override suspend fun upsertLearningStats(stats: LearningStats) {}
        override suspend fun getLearningStats(date: String): LearningStats? = learningStats
        override fun getRecentStats(days: Int): Flow<List<LearningStats>> = flowOf(emptyList())
        override fun getTotalStudyTime(): Flow<Long?> = flowOf(null)
        override suspend fun insertQuizResult(result: QuizResult) {}
        override suspend fun getBestQuizResult(quizId: String): QuizResult? = null
        override fun getQuizResults(quizId: String): Flow<List<QuizResult>> = flowOf(emptyList())
        override fun getAllQuizResults(): Flow<List<QuizResult>> = flowOf(emptyList())
        override suspend fun upsertNote(note: UserNote) {}
        override suspend fun getNotesForSection(sectionId: String): List<UserNote> = emptyList()
        override fun getAllNotes(): Flow<List<UserNote>> = flowOf(emptyList())
        override suspend fun deleteNote(noteId: Long) {}
        override suspend fun searchNotes(query: String): List<UserNote> = emptyList()
        override suspend fun getCompletedExerciseCountByCategory(category: String): Int = 0
        override suspend fun getCompletedExercisesCountSync(): Int = 0
        override suspend fun getCompletedSectionsCountSync(): Int = 0
        override suspend fun getStatsForDate(date: String): LearningStats? = learningStats
        override suspend fun upsertPathProgress(progress: PathProgress) {}
        override suspend fun getPathProgress(pathId: String): List<PathProgress> = emptyList()
        override fun observePathProgress(pathId: String): Flow<List<PathProgress>> = flowOf(emptyList())
        override fun observeAllPathProgress(): Flow<List<PathProgress>> = flowOf(emptyList())
        override suspend fun insertDailyChallengeResult(result: DailyChallengeResult) {}
        override suspend fun getDailyChallengeResult(date: String): DailyChallengeResult? = null
        override suspend fun getDailyChallengeCompletedCount(): Int = 0
        override suspend fun insertRefactoringResult(result: RefactoringResult) {}
        override suspend fun getBestRefactoringResult(challengeId: String): RefactoringResult? = null
        override fun getAllRefactoringResults(): Flow<List<RefactoringResult>> = flowOf(emptyList())
        override suspend fun upsertProjectProgress(progress: ProjectProgress) {}
        override suspend fun getProjectProgress(projectId: String): List<ProjectProgress> = emptyList()
        override fun observeProjectProgress(projectId: String): Flow<List<ProjectProgress>> = flowOf(emptyList())
    }

    @Test
    fun `getWidgetData returns correct due flashcard count`() = runTest {
        val fakeDao = FakeFlashCardDao(dueCardCount = 7)
        val fakeProgressDao = FakeProgressDao()
        val progressRepo = ProgressRepository(fakeProgressDao)
        val provider = WidgetDataProvider(fakeDao, progressRepo)

        val data = provider.getWidgetData()

        assertEquals(7, data.dueFlashcards)
    }

    @Test
    fun `getWidgetData returns correct streak count`() = runTest {
        // With no learning stats, streak should be 0
        val fakeDao = FakeFlashCardDao(dueCardCount = 0)
        val fakeProgressDao = FakeProgressDao(learningStats = null)
        val progressRepo = ProgressRepository(fakeProgressDao)
        val provider = WidgetDataProvider(fakeDao, progressRepo)

        val data = provider.getWidgetData()

        assertEquals(0, data.streakDays)
    }

    @Test
    fun `getWidgetData returns zero completedExercises`() = runTest {
        val fakeDao = FakeFlashCardDao(dueCardCount = 3)
        val fakeProgressDao = FakeProgressDao()
        val progressRepo = ProgressRepository(fakeProgressDao)
        val provider = WidgetDataProvider(fakeDao, progressRepo)

        val data = provider.getWidgetData()

        assertEquals(0, data.completedExercises)
    }
}
