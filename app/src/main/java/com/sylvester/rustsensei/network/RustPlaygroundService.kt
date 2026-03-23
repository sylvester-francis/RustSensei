package com.sylvester.rustsensei.network

import java.io.IOException

interface RustPlaygroundService {
    suspend fun execute(request: PlaygroundRequest): PlaygroundResponse
}

data class PlaygroundRequest(
    val channel: String = "stable",
    val mode: String = "debug",
    val edition: String = "2021",
    val crateType: String = "bin",
    val tests: Boolean = false,
    val code: String,
    val backtrace: Boolean = false
)

data class PlaygroundResponse(
    val success: Boolean,
    val stdout: String,
    val stderr: String
)

class PlaygroundApiException(message: String, cause: Throwable? = null) :
    IOException(message, cause)
