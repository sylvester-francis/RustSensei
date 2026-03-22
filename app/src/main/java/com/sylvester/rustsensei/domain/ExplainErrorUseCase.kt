package com.sylvester.rustsensei.domain

import com.sylvester.rustsensei.content.ContentProvider
import com.sylvester.rustsensei.llm.ChatTemplateFormatter
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.llm.InferenceEngine
import com.sylvester.rustsensei.llm.ModelLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import javax.inject.Inject

sealed interface ErrorExplanationEvent {
    data class Token(val displayText: String) : ErrorExplanationEvent
    data class Completed(val fullText: String, val elapsedMs: Long) : ErrorExplanationEvent
    data class Error(val message: String) : ErrorExplanationEvent
}

open class ExplainErrorUseCase @Inject constructor(
    private val contentProvider: ContentProvider,
    private val engine: InferenceEngine,
    private val modelLifecycle: ModelLifecycle
) {

    open operator fun invoke(
        rawError: String,
        config: InferenceConfig
    ): Flow<ErrorExplanationEvent> = flow {
        if (!modelLifecycle.ensureLoaded()) {
            emit(ErrorExplanationEvent.Error("Model not available. Download from Settings."))
            return@flow
        }

        // Try to enrich with bundled reference data
        val errorCode = ErrorCodeParser.extractFirstCode(rawError)
        val referenceContext = errorCode?.let { code ->
            try {
                val json = contentProvider.getReferenceItem("compiler-errors", code)
                json?.let { buildReferenceContext(it) }
            } catch (_: Exception) {
                null
            }
        }

        val prompt = ChatTemplateFormatter.formatErrorExplanation(rawError, referenceContext)

        engine.clearCache()

        val startTime = System.currentTimeMillis()
        val buffer = StringBuilder()

        try {
            engine.generate(prompt, config).collect { token ->
                buffer.append(token)
                emit(ErrorExplanationEvent.Token(
                    ChatTemplateFormatter.stripThinkTags(buffer.toString())
                ))
            }
        } catch (e: Exception) {
            emit(ErrorExplanationEvent.Error(e.message ?: "Explanation failed"))
            return@flow
        }

        val finalText = ChatTemplateFormatter.stripThinkTags(buffer.toString()).trim()
        emit(ErrorExplanationEvent.Completed(
            fullText = finalText,
            elapsedMs = System.currentTimeMillis() - startTime
        ))
    }

    private fun buildReferenceContext(json: JSONObject): String = buildString {
        appendLine("Error: ${json.optString("code")} — ${json.optString("title")}")
        appendLine("Explanation: ${json.optString("explanation")}")
        val fixes = json.optJSONArray("fixes")
        if (fixes != null && fixes.length() > 0) {
            appendLine("Fixes:")
            for (i in 0 until fixes.length()) {
                val fix = fixes.getJSONObject(i)
                appendLine("- ${fix.optString("description")}: ${fix.optString("code")}")
            }
        }
        json.optString("tip").takeIf { it.isNotBlank() }?.let {
            appendLine("Tip: $it")
        }
    }
}
