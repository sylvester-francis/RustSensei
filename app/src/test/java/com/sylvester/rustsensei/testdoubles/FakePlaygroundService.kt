package com.sylvester.rustsensei.testdoubles

import com.sylvester.rustsensei.network.PlaygroundRequest
import com.sylvester.rustsensei.network.PlaygroundResponse
import com.sylvester.rustsensei.network.RustPlaygroundService

/**
 * Test double for [RustPlaygroundService] that returns configurable responses
 * without making real HTTP requests.
 */
class FakePlaygroundService : RustPlaygroundService {

    var responseToReturn = PlaygroundResponse(
        success = true,
        stdout = "Hello, world!\n",
        stderr = ""
    )
    var shouldThrow: Exception? = null
    var lastRequest: PlaygroundRequest? = null
        private set
    var executeCallCount = 0
        private set

    override suspend fun execute(request: PlaygroundRequest): PlaygroundResponse {
        executeCallCount++
        lastRequest = request
        shouldThrow?.let { throw it }
        return responseToReturn
    }
}
