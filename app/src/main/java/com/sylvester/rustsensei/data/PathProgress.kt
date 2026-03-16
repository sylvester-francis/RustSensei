package com.sylvester.rustsensei.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "path_progress")
data class PathProgress(
    @PrimaryKey
    val stepId: String,  // "rust-fundamentals:step-1"
    val pathId: String,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)
