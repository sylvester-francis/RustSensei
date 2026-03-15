package com.sylvester.rustsensei.llm

data class InferenceConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 512,
    val contextLength: Int = 2048
)
