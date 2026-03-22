package com.sylvester.rustsensei.domain

import com.sylvester.rustsensei.content.ContentProvider
import com.sylvester.rustsensei.content.ExerciseData
import com.sylvester.rustsensei.data.DailyChallengeResult
import com.sylvester.rustsensei.data.ProgressDao
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

open class GetDailyChallengeUseCase @Inject constructor(
    private val contentProvider: ContentProvider,
    private val progressDao: ProgressDao
) {

    /**
     * Returns today's challenge exercise, deterministically selected from
     * the exercise pool using the date as seed. Same challenge for everyone
     * on the same day, no server needed.
     */
    open suspend operator fun invoke(): DailyChallengeData {
        val today = LocalDate.now()
        val todayStr = today.toString()

        // Check if already completed today
        val existingResult = progressDao.getDailyChallengeResult(todayStr)

        // Deterministic selection: epoch day as seed
        val seed = today.toEpochDay()
        val categories = contentProvider.getExerciseCategories()
        val allExerciseIds = categories.flatMap { it.exercises }

        if (allExerciseIds.isEmpty()) {
            return DailyChallengeData(exercise = null, isCompleted = false)
        }

        val shuffled = allExerciseIds.shuffled(Random(seed))
        val selectedId = shuffled.first()
        val exercise = contentProvider.getExercise(selectedId)

        return DailyChallengeData(
            exercise = exercise,
            isCompleted = existingResult != null,
            completionTime = existingResult?.timeTakenSeconds
        )
    }
}

data class DailyChallengeData(
    val exercise: ExerciseData?,
    val isCompleted: Boolean,
    val completionTime: Long? = null
)
