package com.sylvester.rustsensei.content

data class Project(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val estimatedHours: Int,
    val concepts: List<String>,
    val steps: List<ProjectStep>
)

data class ProjectStep(
    val id: String,
    val title: String,
    val instructions: String,
    val starterCode: String,
    val relatedChapter: String? = null
)
