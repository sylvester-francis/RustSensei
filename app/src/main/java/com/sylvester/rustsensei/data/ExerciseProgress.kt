package com.sylvester.rustsensei.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_progress")
data class ExerciseProgress(
    @PrimaryKey
    val exerciseId: String,
    val category: String,
    val status: String = "not_started", // not_started, in_progress, completed
    val userCode: String = "",
    val hintsViewed: Int = 0,
    val attempts: Int = 0,
    val completedAt: Long? = null,
    val lastAttemptAt: Long = System.currentTimeMillis()
)
