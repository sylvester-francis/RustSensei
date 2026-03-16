package com.sylvester.rustsensei.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flash_cards")
data class FlashCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val front: String,        // Question or term
    val back: String,         // Answer or definition
    val category: String,     // e.g., "ownership", "traits", "lifetimes"
    val sourceId: String = "",// e.g., "ch04-01" or "variables1"
    val nextReviewAt: Long = System.currentTimeMillis(), // When to review next
    val interval: Int = 0,    // Days until next review
    val easeFactor: Float = 2.5f, // SM-2 ease factor
    val repetitions: Int = 0, // Number of successful reviews
    val lastReviewedAt: Long = 0
)
