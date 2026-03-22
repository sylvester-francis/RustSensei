package com.sylvester.rustsensei.domain

import com.sylvester.rustsensei.llm.ChatTemplateFormatter
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.llm.InferenceEngine
import com.sylvester.rustsensei.llm.ModelLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

open class SimulateExecutionUseCase @Inject constructor(
    private val engine: InferenceEngine,
    private val modelLifecycle: ModelLifecycle
) {
    open operator fun invoke(code: String, config: InferenceConfig): Flow<ExecutionEvent> = flow {
        if (!modelLifecycle.ensureLoaded()) {
            emit(ExecutionEvent.Error("Model not available. Download from Settings."))
            return@flow
        }

        val prompt = buildExecutionPrompt(code)
        engine.clearCache()

        val startTime = System.currentTimeMillis()
        val buffer = StringBuilder()

        try {
            engine.generate(prompt, config.copy(temperature = 0.1f)).collect { token ->
                buffer.append(token)
                emit(ExecutionEvent.Output(ChatTemplateFormatter.stripThinkTags(buffer.toString())))
            }
        } catch (e: Exception) {
            emit(ExecutionEvent.Error(e.message ?: "Execution simulation failed"))
            return@flow
        }

        val finalOutput = ChatTemplateFormatter.stripThinkTags(buffer.toString()).trim()
        emit(ExecutionEvent.Completed(
            fullOutput = finalOutput,
            elapsedMs = System.currentTimeMillis() - startTime
        ))
    }

    private fun buildExecutionPrompt(code: String): String = buildString {
        append("<|im_start|>system\n")
        append("You are a Rust compiler and runtime simulator. Given Rust code, analyze it step by step:\n")
        append("1. If the code has compilation errors, output the exact compiler error message.\n")
        append("2. If the code compiles, trace through execution and output ONLY what stdout would print.\n")
        append("3. Format output exactly as it would appear in a terminal.\n")
        append("4. Do not explain the code. Do not add commentary. Just show the output.\n")
        append("5. If there is no output, say: (no output)\n")
        append("Do not use <think> tags.")
        append("<|im_end|>\n")
        append("<|im_start|>user\n")
        append("Run this Rust code and show the output:\n\n```rust\n")
        append(ChatTemplateFormatter.sanitize(code))
        append("\n```\n")
        append("<|im_end|>\n")
        append("<|im_start|>assistant\n")
    }
}
