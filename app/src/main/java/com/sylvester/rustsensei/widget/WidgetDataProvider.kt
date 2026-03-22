package com.sylvester.rustsensei.widget

import com.sylvester.rustsensei.data.FlashCardDao
import com.sylvester.rustsensei.data.ProgressRepository
import javax.inject.Inject

data class WidgetData(
    val streakDays: Int,
    val dueFlashcards: Int,
    val completedExercises: Int
)

class WidgetDataProvider @Inject constructor(
    private val flashCardDao: FlashCardDao,
    private val progressRepository: ProgressRepository
) {
    suspend fun getWidgetData(): WidgetData {
        val streak = progressRepository.calculateStreak()
        val dueCards = flashCardDao.getDueCardCountSync(System.currentTimeMillis())
        return WidgetData(
            streakDays = streak,
            dueFlashcards = dueCards,
            completedExercises = 0
        )
    }
}
