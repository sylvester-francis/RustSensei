package com.sylvester.rustsensei.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "refactoring_results")
data class RefactoringResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val challengeId: String,
    val userCode: String = "",
    val score: Int = 0,
    val feedback: String = "",
    val completedAt: Long = 0
)
