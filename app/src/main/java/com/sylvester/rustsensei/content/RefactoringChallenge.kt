package com.sylvester.rustsensei.content

data class RefactoringChallenge(
    val id: String,
    val title: String,
    val difficulty: String,
    val description: String,
    val uglyCode: String,
    val hints: List<String>,
    val idiomaticSolution: String,
    val scoringCriteria: String
)
