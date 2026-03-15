package com.sylvester.rustsensei.llm

data class InferenceConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 384,
    val contextLength: Int = 2048
) {
    companion object {
        // Model-specific defaults
        fun forModel(modelId: String): InferenceConfig {
            return when (modelId) {
                "qwen3-4b" -> InferenceConfig(
                    maxTokens = 384,
                    contextLength = 4096  // 4B model: plenty of room
                )
                "qwen3-8b" -> InferenceConfig(
                    maxTokens = 256,       // 8B: shorter responses, faster
                    contextLength = 2048   // 8B: smaller context, less KV cache memory
                )
                else -> InferenceConfig()
            }
        }
    }
}
