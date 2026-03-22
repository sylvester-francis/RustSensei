package com.sylvester.rustsensei.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "project_progress",
    indices = [Index(value = ["projectId"])]
)
data class ProjectProgress(
    @PrimaryKey val id: String,
    val projectId: String,
    val stepId: String,
    val isCompleted: Boolean = false,
    val userCode: String = "",
    val completedAt: Long? = null
)
