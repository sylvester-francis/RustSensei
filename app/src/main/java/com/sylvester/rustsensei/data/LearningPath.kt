package com.sylvester.rustsensei.data

data class LearningPath(
    val id: String,
    val title: String,
    val description: String,
    val estimatedDays: Int,
    val steps: List<PathStep>
)

data class PathStep(
    val id: String,
    val type: String,     // "read", "exercise", "review"
    val targetId: String, // section ID, exercise ID, or "review"
    val title: String,
    val description: String
)
