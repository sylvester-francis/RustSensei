package com.sylvester.rustsensei.llm

import kotlinx.coroutines.flow.Flow

interface InferenceEngine {
    suspend fun loadModel(modelPath: String, contextSize: Int = 2048): Boolean
    fun generate(
        prompt: String,
        config: InferenceConfig = InferenceConfig(),
        onStats: ((Float, Float, Float) -> Unit)? = null
    ): Flow<String>
    fun stopGeneration()
    fun clearCache()
    suspend fun unloadModel()
    fun isModelLoaded(): Boolean
    fun getActiveBackend(): String = "CPU"
}
