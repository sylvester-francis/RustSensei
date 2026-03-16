package com.sylvester.rustsensei.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacedRepetitionTest {

    private fun baseCard() = FlashCard(
        id = 1,
        front = "What is ownership?",
        back = "Each value has exactly one owner.",
        category = "ownership",
        repetitions = 0,
        interval = 0,
        easeFactor = 2.5f,
        nextReviewAt = 0,
        lastReviewedAt = 0
    )

    @Test
    fun `review with quality less than 3 resets card`() {
        val card = baseCard().copy(repetitions = 3, interval = 10, easeFactor = 2.5f)
        val result = SpacedRepetition.review(card, quality = 2)
        assertEquals(0, result.repetitions)
        assertEquals(1, result.interval)
    }

    @Test
    fun `review with quality 3 advances interval`() {
        val card = baseCard().copy(repetitions = 0, interval = 0)
        val result = SpacedRepetition.review(card, quality = 3)
        assertEquals(1, result.repetitions)
        assertEquals(1, result.interval)
    }

    @Test
    fun `review with quality 5 increases ease factor`() {
        val card = baseCard().copy(repetitions = 2, interval = 6, easeFactor = 2.5f)
        val result = SpacedRepetition.review(card, quality = 5)
        assertTrue(
            "Ease factor should increase with quality 5, was ${result.easeFactor}",
            result.easeFactor > 2.5f
        )
    }

    @Test
    fun `first review sets interval to 1 day`() {
        val card = baseCard()
        val result = SpacedRepetition.review(card, quality = 4)
        assertEquals(1, result.repetitions)
        assertEquals(1, result.interval)
    }

    @Test
    fun `second review sets interval to 6 days`() {
        val card = baseCard().copy(repetitions = 1, interval = 1)
        val result = SpacedRepetition.review(card, quality = 4)
        assertEquals(2, result.repetitions)
        assertEquals(6, result.interval)
    }

    @Test
    fun `ease factor floors at 1_3`() {
        // Repeatedly review with quality 3 (hard) to drive ease factor down
        var card = baseCard().copy(repetitions = 5, interval = 30, easeFactor = 1.35f)
        card = SpacedRepetition.review(card, quality = 3)
        card = SpacedRepetition.review(card, quality = 3)
        card = SpacedRepetition.review(card, quality = 3)
        assertTrue(
            "Ease factor should not drop below 1.3, was ${card.easeFactor}",
            card.easeFactor >= 1.3f
        )
    }

    @Test
    fun `quality clamped to 0-5 range`() {
        // Quality -1 should be clamped to 0 (still < 3, so reset)
        val card = baseCard().copy(repetitions = 3, interval = 10)
        val resultNeg = SpacedRepetition.review(card, quality = -1)
        assertEquals(0, resultNeg.repetitions)
        assertEquals(1, resultNeg.interval)

        // Quality 10 should be clamped to 5 (successful review)
        val card2 = baseCard()
        val resultHigh = SpacedRepetition.review(card2, quality = 10)
        assertEquals(1, resultHigh.repetitions)
        assertTrue(resultHigh.easeFactor >= 2.5f)
    }
}
