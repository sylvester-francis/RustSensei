package com.sylvester.rustsensei.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_results")
data class QuizResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quizId: String,
    val score: Int,
    val totalQuestions: Int,
    val completedAt: Long = System.currentTimeMillis()
)
