package com.sylvester.rustsensei.domain

import com.sylvester.rustsensei.content.RagRetriever
import com.sylvester.rustsensei.data.ChatRepository
import com.sylvester.rustsensei.llm.ChatTemplateFormatter
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.llm.InferenceEngine
import com.sylvester.rustsensei.llm.ModelLifecycle
import com.sylvester.rustsensei.viewmodel.ChatContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Orchestrates the full lifecycle of sending a chat message:
 *  1. Ensures the model is loaded (transparently handles idle-unload).
 *  2. Persists the user message.
 *  3. Resolves contextual RAG / exercise / book content.
 *  4. Formats the prompt and drives streaming inference.
 *  5. Persists the assistant response on completion.
 *
 * The ViewModel remains thin — it maps [ChatStreamEvent]s to UI state.
 */
class SendChatMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val ragRetriever: RagRetriever,
    private val engine: InferenceEngine,
    private val modelLifecycle: ModelLifecycle
) {

    operator fun invoke(
        conversationId: Long,
        message: String,
        chatContext: ChatContext,
        config: InferenceConfig
    ): Flow<ChatStreamEvent> = flow {
        if (!modelLifecycle.ensureLoaded()) {
            emit(ChatStreamEvent.Error("Model not available. Download from Settings."))
            return@flow
        }

        chatRepository.addMessage(conversationId, "user", message)

        val ragContext = resolveContext(message, chatContext)
        val allMessages = chatRepository.getMessagesOnce(conversationId)
        val prompt = ChatTemplateFormatter.formatMessages(
            allMessages, config.contextLength, ragContext = ragContext
        )

        engine.clearCache()

        val startTime = System.currentTimeMillis()
        val buffer = StringBuilder()
        var decodeTokPerSec = 0f
        var prefillMs = 0L

        try {
            engine.generate(prompt, config, onStats = { _, decode, prefill ->
                decodeTokPerSec = decode
                prefillMs = prefill.toLong()
            }).collect { token ->
                buffer.append(token)
                emit(ChatStreamEvent.Token(
                    ChatTemplateFormatter.stripThinkTags(buffer.toString())
                ))
            }
        } catch (e: Exception) {
            chatRepository.addMessage(conversationId, "assistant", "Error: ${e.message}")
            emit(ChatStreamEvent.Error(e.message ?: "Generation failed"))
            return@flow
        }

        val finalText = ChatTemplateFormatter.stripThinkTags(buffer.toString()).trim()
        if (finalText.isNotEmpty()) {
            chatRepository.addMessage(conversationId, "assistant", finalText)
        }

        emit(ChatStreamEvent.Completed(
            fullText = finalText,
            elapsedMs = System.currentTimeMillis() - startTime,
            decodeTokPerSec = decodeTokPerSec,
            prefillMs = prefillMs
        ))
    }

    private suspend fun resolveContext(message: String, context: ChatContext): String? =
        when (context) {
            is ChatContext.General -> ragRetriever.retrieveContext(message)
            is ChatContext.BookSection -> context.content
            is ChatContext.Exercise -> buildString {
                appendLine("Exercise: ${context.description}")
                appendLine("\nStudent's code:")
                appendLine("```rust")
                appendLine(context.userCode)
                appendLine("```")
            }
        }
}

sealed interface ChatStreamEvent {
    /** Progressive display text (accumulated and think-tag-stripped). */
    data class Token(val displayText: String) : ChatStreamEvent

    /** Generation finished successfully. */
    data class Completed(
        val fullText: String,
        val elapsedMs: Long,
        val decodeTokPerSec: Float,
        val prefillMs: Long
    ) : ChatStreamEvent

    /** Unrecoverable error during generation. */
    data class Error(val message: String) : ChatStreamEvent
}
