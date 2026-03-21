package com.sylvester.rustsensei.testdoubles

import com.sylvester.rustsensei.llm.ModelLifecycle
import com.sylvester.rustsensei.llm.ModelReadyState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Test double for [ModelLifecycle] that avoids native libraries and file system.
 *
 * Configure [ensureLoadedResult] per test to simulate model availability.
 */
class FakeModelLifecycle : ModelLifecycle {

    private val _state = MutableStateFlow(ModelReadyState.READY)
    override val state: StateFlow<ModelReadyState> = _state.asStateFlow()

    var ensureLoadedResult = true
    var ensureLoadedCallCount = 0
        private set

    override suspend fun ensureLoaded(): Boolean {
        ensureLoadedCallCount++
        _state.value = if (ensureLoadedResult) ModelReadyState.READY else ModelReadyState.NOT_DOWNLOADED
        return ensureLoadedResult
    }

    override suspend fun unload() { _state.value = ModelReadyState.DOWNLOADED }
    override fun scheduleIdleUnload() {}
    override fun cancelIdleTimer() {}
    override fun refreshState() {}
    override fun getActiveBackend(): String = "FAKE"
    override fun isModelLoaded(): Boolean = _state.value == ModelReadyState.READY

    fun setState(state: ModelReadyState) { _state.value = state }
}
