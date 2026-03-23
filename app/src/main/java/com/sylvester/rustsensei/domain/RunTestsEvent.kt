package com.sylvester.rustsensei.domain

sealed interface RunTestsEvent {
    data object Running : RunTestsEvent
    data class Completed(
        val passed: Int,
        val total: Int,
        val testResults: List<TestCaseResult>,
        val rawOutput: String,
        val elapsedMs: Long
    ) : RunTestsEvent
    data class Error(val message: String) : RunTestsEvent
}

data class TestCaseResult(
    val name: String,
    val passed: Boolean
)
