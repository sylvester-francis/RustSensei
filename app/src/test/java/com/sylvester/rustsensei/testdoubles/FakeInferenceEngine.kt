package com.sylvester.rustsensei.testdoubles

import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.llm.InferenceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Test double for [InferenceEngine] that emits configurable tokens
 * without requiring native libraries or GPU hardware.
 */
class FakeInferenceEngine : InferenceEngine {

    var loaded = false
    var shouldFailLoad = false
    var shouldFailGenerate = false
    var tokensToEmit: List<String> = listOf("Hello", " ", "World")
    var generateCallCount = 0
        private set

    override suspend fun loadModel(modelPath: String, contextSize: Int): Boolean {
        if (shouldFailLoad) return false
        loaded = true
        return true
    }

    override fun generate(
        prompt: String,
        config: InferenceConfig,
        onStats: ((Float, Float, Float) -> Unit)?
    ): Flow<String> = flow {
        generateCallCount++
        if (shouldFailGenerate) throw RuntimeException("Fake generation error")
        tokensToEmit.forEach { emit(it) }
        onStats?.invoke(0f, 10f, 50f)
    }

    override fun stopGeneration() {}

    override fun clearCache() {}

    override suspend fun unloadModel() {
        loaded = false
    }

    override fun isModelLoaded(): Boolean = loaded

    override fun getActiveBackend(): String = "FAKE"
}
