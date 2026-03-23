package com.sylvester.rustsensei.domain

sealed interface CompileCodeEvent {
    data object Compiling : CompileCodeEvent
    data class Completed(
        val success: Boolean,
        val stdout: String,
        val stderr: String,
        val elapsedMs: Long
    ) : CompileCodeEvent
    data class Error(val message: String) : CompileCodeEvent
}
