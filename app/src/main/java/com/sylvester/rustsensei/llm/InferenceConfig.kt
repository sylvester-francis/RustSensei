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
                "litert-0.6b" -> InferenceConfig(
                    maxTokens = 256,
                    contextLength = 2048
                )
                "litert-1b-gemma" -> InferenceConfig(
                    maxTokens = 384,
                    contextLength = 2048
                )
                "litert-1.7b" -> InferenceConfig(
                    maxTokens = 384,
                    contextLength = 2048
                )
                else -> InferenceConfig()
            }
        }
    }
}
