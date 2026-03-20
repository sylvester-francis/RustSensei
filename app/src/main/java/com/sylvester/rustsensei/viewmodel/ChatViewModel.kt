package com.sylvester.rustsensei.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.content.RagRetriever
import com.sylvester.rustsensei.data.ChatMessage
import com.sylvester.rustsensei.data.ChatRepository
import com.sylvester.rustsensei.data.PreferencesManager
import com.sylvester.rustsensei.llm.ChatTemplateFormatter
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.llm.InferenceEngine
import com.sylvester.rustsensei.llm.LiteRtEngine
import com.sylvester.rustsensei.llm.ModelManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

sealed class ChatContext {
    data object General : ChatContext()
    data class BookSection(val sectionId: String, val content: String) : ChatContext()
    data class Exercise(val exerciseId: String, val description: String, val userCode: String) : ChatContext()
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val streamingText: String = "",
    val currentConversationId: Long? = null,
    val inferenceTimeMs: Long = 0,
    val lastPrefillTokPerSec: Float = 0f,
    val lastDecodeTokPerSec: Float = 0f,
    val lastTimeToFirstTokenMs: Long = 0,
    val followUpSuggestions: List<String> = emptyList(),
    val errorMessage: String? = null
)

class ChatViewModel(
    private val repository: ChatRepository,
    val liteRtEngine: LiteRtEngine,
    private val ragRetriever: RagRetriever,
    private val prefsManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
        // Bug 10: maximum allowed input length
        private const val MAX_MESSAGE_LENGTH = 4000
        // Optimization #1: Unload LLM after 5 minutes of inactivity
        private const val IDLE_UNLOAD_DELAY_MS = 5 * 60 * 1000L
    }

    private fun getActiveEngine(): InferenceEngine {
        val modelId = prefsManager.getSelectedModelId()
        val model = ModelManager.getModelById(modelId)
        return liteRtEngine
    }

    // Reactive model-loaded state — ChatScreen observes this via collectAsState()
    val modelLoaded: StateFlow<Boolean> = liteRtEngine.modelLoaded
    val isModelLoading: StateFlow<Boolean> = liteRtEngine.isModelLoading

    fun isAnyModelLoaded(): Boolean {
        return liteRtEngine.isModelLoaded()
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // P1 Fix #14: load persisted config on init, save on change
    private val _config = MutableStateFlow(prefsManager.loadInferenceConfig())
    val config: StateFlow<InferenceConfig> = _config.asStateFlow()

    private val _chatContext = MutableStateFlow<ChatContext>(ChatContext.General)
    val chatContext: StateFlow<ChatContext> = _chatContext.asStateFlow()

    private var generationJob: Job? = null
    private var messagesJob: Job? = null

    // Optimization #1: Idle timer to unload the model after inactivity
    private var idleUnloadJob: Job? = null

    // Bug 6: AtomicBoolean gate to prevent double-tap race condition.
    // compareAndSet is atomic — only the first caller wins.
    private val sendingGate = AtomicBoolean(false)

    fun updateConfig(config: InferenceConfig) {
        _config.value = config
        prefsManager.saveInferenceConfig(config)
    }

    fun applyModelDefaults(modelId: String) {
        val defaults = InferenceConfig.forModel(modelId)
        // Preserve user's temperature/topP but apply model-specific context and max tokens
        val current = _config.value
        val updated = current.copy(
            contextLength = defaults.contextLength,
            maxTokens = defaults.maxTokens
        )
        _config.value = updated
        prefsManager.saveInferenceConfig(updated)
    }

    fun setChatContext(context: ChatContext) {
        _chatContext.value = context
    }

    fun clearChatContext() {
        _chatContext.value = ChatContext.General
    }

    fun getContentVersion(): Int = prefsManager.getContentVersion()

    fun startNewConversation() {
        viewModelScope.launch {
            try {
                messagesJob?.cancel()
                getActiveEngine().clearCache()
                val convId = repository.createConversation()
                _uiState.value = ChatUiState(currentConversationId = convId)
                _chatContext.value = ChatContext.General
                observeMessages(convId)
            } catch (e: Exception) {
                Log.e(TAG, "Error in startNewConversation: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to create conversation. Please try again."
                )
            }
        }
    }

    fun loadConversation(conversationId: Long) {
        viewModelScope.launch {
            try {
                messagesJob?.cancel()
                getActiveEngine().clearCache()
                _uiState.value = _uiState.value.copy(currentConversationId = conversationId)
                observeMessages(conversationId)
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadConversation: ${e.message}", e)
            }
        }
    }

    private fun observeMessages(conversationId: Long) {
        messagesJob = viewModelScope.launch {
            try {
                repository.getMessages(conversationId).collect { messages ->
                    _uiState.value = _uiState.value.copy(messages = messages)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in observeMessages: ${e.message}", e)
            }
        }
    }

    // Optimization #1: Schedule model unload after idle period
    private fun scheduleIdleUnload() {
        idleUnloadJob?.cancel()
        idleUnloadJob = viewModelScope.launch {
            delay(IDLE_UNLOAD_DELAY_MS)
            if (!_uiState.value.isGenerating) {
                Log.i(TAG, "Idle timeout reached — unloading model to save battery")
                liteRtEngine.unloadModel()
            }
        }
    }

    // Optimization #1: Cancel idle timer when user is actively chatting
    private fun cancelIdleUnload() {
        idleUnloadJob?.cancel()
        idleUnloadJob = null
    }

    fun sendMessage(text: String) {
        val convId = _uiState.value.currentConversationId ?: return

        // Bug 10: trim whitespace and validate input
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        // Bug 6: atomic gate — only the first concurrent caller proceeds.
        // This prevents the race where two quick taps both pass the isGenerating check.
        if (!sendingGate.compareAndSet(false, true)) return

        // Bug 10: enforce max character limit
        val message = if (trimmed.length > MAX_MESSAGE_LENGTH) {
            Log.w(TAG, "Message truncated from ${trimmed.length} to $MAX_MESSAGE_LENGTH characters")
            trimmed.take(MAX_MESSAGE_LENGTH)
        } else {
            trimmed
        }

        // Optimization #1: Cancel idle unload while actively chatting
        cancelIdleUnload()

        viewModelScope.launch {
            try {
                repository.addMessage(convId, "user", message)

                // Optimization #6: RAG retrieval already runs on Dispatchers.IO
                // (see RagRetriever.retrieveContext), so this is safe.
                val ragContext = when (val ctx = _chatContext.value) {
                    is ChatContext.General -> {
                        ragRetriever.retrieveContext(message)
                    }
                    is ChatContext.BookSection -> {
                        ctx.content
                    }
                    is ChatContext.Exercise -> {
                        buildString {
                            appendLine("Exercise: ${ctx.description}")
                            appendLine("\nStudent's code:")
                            appendLine("```rust")
                            appendLine(ctx.userCode)
                            appendLine("```")
                        }
                    }
                }

                // Fix #7: clear context after using it for the first message
                // so subsequent messages in the same conversation don't re-inject stale context
                if (_chatContext.value !is ChatContext.General) {
                    _chatContext.value = ChatContext.General
                }

                val allMessages = repository.getMessagesOnce(convId)
                val prompt = ChatTemplateFormatter.formatMessages(
                    allMessages,
                    _config.value.contextLength,
                    ragContext = ragContext
                )

                // Reset the LiteRT conversation before each message — we manage
                // the full chat history in our prompt, so the Conversation object
                // must not accumulate its own internal context (which would double
                // the token count and overflow after ~3 exchanges).
                getActiveEngine().clearCache()

                _uiState.value = _uiState.value.copy(
                    isGenerating = true,
                    streamingText = "",
                    inferenceTimeMs = 0,
                    lastPrefillTokPerSec = 0f,
                    lastDecodeTokPerSec = 0f,
                    lastTimeToFirstTokenMs = 0
                )

                val startTime = System.currentTimeMillis()
                val tokenBuffer = StringBuilder()

                generationJob = viewModelScope.launch {
                    getActiveEngine().generate(
                        prompt,
                        _config.value,
                        onStats = { prefillTokPerSec, decodeTokPerSec, prefillMs ->
                            _uiState.value = _uiState.value.copy(
                                lastPrefillTokPerSec = prefillTokPerSec,
                                lastDecodeTokPerSec = decodeTokPerSec,
                                lastTimeToFirstTokenMs = prefillMs.toLong()
                            )
                        }
                    )
                        .catch { e ->
                            _uiState.value = _uiState.value.copy(
                                isGenerating = false,
                                streamingText = ""
                            )
                            repository.addMessage(convId, "assistant", "Error: ${e.message}")
                        }
                        .onCompletion {
                            val elapsed = System.currentTimeMillis() - startTime
                            val finalText = ChatTemplateFormatter.stripThinkTags(
                                tokenBuffer.toString()
                            ).trim()
                            if (finalText.isNotEmpty()) {
                                repository.addMessage(convId, "assistant", finalText)
                            }
                            val followUps = if (finalText.isNotEmpty()) generateFollowUps(finalText) else emptyList()
                            _uiState.value = _uiState.value.copy(
                                isGenerating = false,
                                streamingText = "",
                                inferenceTimeMs = elapsed,
                                followUpSuggestions = followUps
                            )
                            // Bug 6: release the gate so the next message can be sent
                            sendingGate.set(false)
                            // Optimization #1: Start idle timer after generation completes
                            scheduleIdleUnload()
                        }
                        .collect { token ->
                            tokenBuffer.append(token)
                            val displayText = ChatTemplateFormatter.stripThinkTags(
                                tokenBuffer.toString()
                            )
                            _uiState.value = _uiState.value.copy(
                                streamingText = displayText
                            )
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in sendMessage: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    streamingText = "",
                    errorMessage = "Failed to generate response. Please try again."
                )
                // Bug 6: release the gate on error so user is not permanently locked out
                sendingGate.set(false)
                // Optimization #1: Start idle timer even on error
                scheduleIdleUnload()
            }
        }
    }

    fun stopGeneration() {
        getActiveEngine().stopGeneration()
        generationJob?.cancel()
        // Bug 6: release the gate when generation is manually stopped
        sendingGate.set(false)
        _uiState.value = _uiState.value.copy(isGenerating = false, streamingText = "")
        // Optimization #1: Start idle timer after manual stop
        scheduleIdleUnload()
    }

    fun exportConversation(): String {
        return buildString {
            appendLine("# RustSensei Chat")
            appendLine()
            for (msg in _uiState.value.messages) {
                if (msg.role == "user") appendLine("**You:** ${msg.content}")
                else appendLine("**RustSensei:** ${msg.content}")
                appendLine()
            }
        }
    }

    private fun generateFollowUps(lastResponse: String): List<String> {
        val followUps = mutableListOf<String>()
        val lowerResponse = lastResponse.lowercase()

        if ("ownership" in lowerResponse && "borrow" !in lowerResponse) followUps.add("How does borrowing work?")
        if ("borrow" in lowerResponse && "lifetime" !in lowerResponse) followUps.add("Explain lifetimes")
        if ("struct" in lowerResponse) followUps.add("Show me struct methods")
        if ("enum" in lowerResponse) followUps.add("How does match work with enums?")
        if ("result" in lowerResponse || "error" in lowerResponse) followUps.add("What's the ? operator?")
        if ("vec" in lowerResponse || "vector" in lowerResponse) followUps.add("How do I iterate over a Vec?")
        if ("trait" in lowerResponse) followUps.add("Show me trait bounds")
        if ("async" in lowerResponse) followUps.add("How does async/await work?")
        if ("closure" in lowerResponse) followUps.add("What are Fn, FnMut, FnOnce?")

        // Always have at least one follow-up
        if (followUps.isEmpty()) {
            followUps.add("Show me a code example")
            followUps.add("How is this different from Python?")
        }

        return followUps.take(3)
    }

    fun getConversations() = repository.getConversations()

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteConversation(conversationId)
                if (_uiState.value.currentConversationId == conversationId) {
                    startNewConversation()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in deleteConversation: ${e.message}", e)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            try {
                repository.clearAllData()
                startNewConversation()
            } catch (e: Exception) {
                Log.e(TAG, "Error in clearAllConversations: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        idleUnloadJob?.cancel()
        getActiveEngine().stopGeneration()
    }
}
