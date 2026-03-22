package com.sylvester.rustsensei.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_challenge_results",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyChallengeResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val exerciseId: String,
    val completedAt: Long = 0,
    val timeTakenSeconds: Long = 0,
    val score: Int = 0
)
