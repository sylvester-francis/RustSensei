package com.sylvester.rustsensei.domain

import com.sylvester.rustsensei.content.ExerciseData
import com.sylvester.rustsensei.llm.ChatTemplateFormatter
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.llm.InferenceEngine
import com.sylvester.rustsensei.llm.ModelLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Validates a student's Rust code against an exercise using the on-device LLM.
 *
 * The model is prompted to evaluate correctness and produce a "CORRECT" or
 * "INCORRECT" verdict followed by an explanation. The UseCase streams tokens
 * for real-time UI feedback and emits a final [ValidationEvent.Completed] with
 * the parsed verdict.
 */
class ValidateExerciseUseCase @Inject constructor(
    private val engine: InferenceEngine,
    private val modelLifecycle: ModelLifecycle
) {

    operator fun invoke(
        exercise: ExerciseData,
        userCode: String
    ): Flow<ValidationEvent> = flow {
        if (!modelLifecycle.ensureLoaded()) {
            emit(ValidationEvent.Error("Model not available. Load a model from Settings."))
            return@flow
        }

        val prompt = ChatTemplateFormatter.formatExerciseValidation(
            exerciseDescription = exercise.description,
            exerciseInstructions = exercise.instructions,
            expectedSolution = exercise.solution,
            studentCode = userCode
        )

        val config = InferenceConfig(
            temperature = 0.3f,
            topP = 0.9f,
            maxTokens = 256,
            contextLength = 2048
        )

        val buffer = StringBuilder()

        try {
            engine.generate(prompt, config).collect { token ->
                buffer.append(token)
                emit(ValidationEvent.Token(
                    ChatTemplateFormatter.stripThinkTags(buffer.toString())
                ))
            }
        } catch (e: Exception) {
            emit(ValidationEvent.Error(e.message ?: "Validation failed"))
            return@flow
        }

        val finalText = ChatTemplateFormatter.stripThinkTags(buffer.toString()).trim()
        val isCorrect = finalText.uppercase().startsWith("CORRECT")
        emit(ValidationEvent.Completed(finalText, isCorrect))
    }
}

sealed interface ValidationEvent {
    /** Progressive display text (accumulated and think-tag-stripped). */
    data class Token(val displayText: String) : ValidationEvent

    /** Validation finished — [isCorrect] is derived from the model's verdict prefix. */
    data class Completed(val fullText: String, val isCorrect: Boolean) : ValidationEvent

    /** Unrecoverable error during validation. */
    data class Error(val message: String) : ValidationEvent
}
