package com.sylvester.rustsensei.domain

sealed interface ExecutionEvent {
    data class Output(val text: String) : ExecutionEvent
    data class Completed(val fullOutput: String, val elapsedMs: Long) : ExecutionEvent
    data class Error(val message: String) : ExecutionEvent
}
