package com.sylvester.rustsensei.data

import kotlin.math.max

/**
 * SM-2 Spaced Repetition Algorithm.
 * quality: 0-5 (0=total blackout, 5=perfect response)
 * Returns updated FlashCard with new interval, ease factor, and next review time.
 */
object SpacedRepetition {

    fun review(card: FlashCard, quality: Int): FlashCard {
        val q = quality.coerceIn(0, 5)

        return if (q < 3) {
            // Failed — reset to beginning
            card.copy(
                repetitions = 0,
                interval = 1,
                nextReviewAt = System.currentTimeMillis() + (1 * 24 * 60 * 60 * 1000L), // 1 day
                lastReviewedAt = System.currentTimeMillis()
            )
        } else {
            // Successful review
            val newReps = card.repetitions + 1
            val newInterval = when (newReps) {
                1 -> 1
                2 -> 6
                else -> (card.interval * card.easeFactor).toInt()
            }
            val newEase = max(1.3f, card.easeFactor + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f)))

            card.copy(
                repetitions = newReps,
                interval = newInterval,
                easeFactor = newEase,
                nextReviewAt = System.currentTimeMillis() + (newInterval * 24 * 60 * 60 * 1000L),
                lastReviewedAt = System.currentTimeMillis()
            )
        }
    }
}
