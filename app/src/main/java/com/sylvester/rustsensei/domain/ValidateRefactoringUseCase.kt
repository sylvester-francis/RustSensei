package com.sylvester.rustsensei.domain

import com.sylvester.rustsensei.llm.ChatTemplateFormatter
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.llm.InferenceEngine
import com.sylvester.rustsensei.llm.ModelLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

sealed interface RefactoringEvent {
    data class Token(val displayText: String) : RefactoringEvent
    data class Completed(val fullText: String, val score: Int) : RefactoringEvent
    data class Error(val message: String) : RefactoringEvent
}

class ValidateRefactoringUseCase @Inject constructor(
    private val engine: InferenceEngine,
    private val modelLifecycle: ModelLifecycle
) {
    operator fun invoke(
        originalCode: String,
        userCode: String,
        idiomaticSolution: String,
        scoringCriteria: String,
        config: InferenceConfig
    ): Flow<RefactoringEvent> = flow {
        if (!modelLifecycle.ensureLoaded()) {
            emit(RefactoringEvent.Error("Model not available. Download from Settings."))
            return@flow
        }

        val prompt = ChatTemplateFormatter.formatRefactoringValidation(
            originalCode, userCode, idiomaticSolution, scoringCriteria
        )

        engine.clearCache()
        val buffer = StringBuilder()

        try {
            engine.generate(prompt, config.copy(temperature = 0.3f)).collect { token ->
                buffer.append(token)
                emit(RefactoringEvent.Token(
                    ChatTemplateFormatter.stripThinkTags(buffer.toString())
                ))
            }
        } catch (e: Exception) {
            emit(RefactoringEvent.Error(e.message ?: "Validation failed"))
            return@flow
        }

        val finalText = ChatTemplateFormatter.stripThinkTags(buffer.toString()).trim()
        val score = parseScore(finalText)
        emit(RefactoringEvent.Completed(fullText = finalText, score = score))
    }

    private fun parseScore(text: String): Int {
        val regex = Regex("""(\d{1,3})/100|Score:\s*(\d{1,3})""")
        val match = regex.find(text)
        return (match?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.toIntOrNull() ?: 50)
            .coerceIn(0, 100)
    }
}
