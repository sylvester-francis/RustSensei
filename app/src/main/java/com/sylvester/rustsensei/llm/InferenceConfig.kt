package com.sylvester.rustsensei.llm

data class InferenceConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 384,
    val contextLength: Int = 2048
) {
    companion object {
        fun forModel(modelId: String): InferenceConfig {
            return when (modelId) {
                "qwen3-0.6b" -> InferenceConfig(
                    maxTokens = 256,
                    contextLength = 2048   // tiny model: keep context small
                )
                "qwen3-1.7b" -> InferenceConfig(
                    maxTokens = 384,
                    contextLength = 2048
                )
                "qwen3-4b" -> InferenceConfig(
                    maxTokens = 384,
                    contextLength = 4096
                )
                "qwen3-8b" -> InferenceConfig(
                    maxTokens = 256,
                    contextLength = 2048
                )
                else -> InferenceConfig()
            }
        }
    }
}
