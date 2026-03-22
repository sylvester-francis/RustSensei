package com.sylvester.rustsensei.domain

import com.sylvester.rustsensei.data.DailyChallengeResult
import com.sylvester.rustsensei.data.LearningStats
import com.sylvester.rustsensei.data.ProgressDao
import java.time.LocalDate
import javax.inject.Inject

class CompleteDailyChallengeUseCase @Inject constructor(
    private val progressDao: ProgressDao
) {

    suspend operator fun invoke(exerciseId: String, timeTakenSeconds: Long) {
        val todayStr = LocalDate.now().toString()

        // Record the daily challenge completion
        progressDao.insertDailyChallengeResult(
            DailyChallengeResult(
                date = todayStr,
                exerciseId = exerciseId,
                completedAt = System.currentTimeMillis(),
                timeTakenSeconds = timeTakenSeconds,
                score = 100
            )
        )

        // Update learning stats for today
        val existing = progressDao.getLearningStats(todayStr)
        val updated = existing?.copy(
            exercisesCompleted = existing.exercisesCompleted + 1,
            studyTimeSeconds = existing.studyTimeSeconds + timeTakenSeconds
        ) ?: LearningStats(
            date = todayStr,
            sectionsRead = 0,
            exercisesCompleted = 1,
            studyTimeSeconds = timeTakenSeconds
        )
        progressDao.upsertLearningStats(updated)
    }
}
