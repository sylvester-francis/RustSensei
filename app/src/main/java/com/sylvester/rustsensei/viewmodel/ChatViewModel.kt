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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

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
    val lastTimeToFirstTokenMs: Long = 0
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    val liteRtEngine: LiteRtEngine,
    private val ragRetriever: RagRetriever,
    private val prefsManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
        // Bug 10: maximum allowed input length
        private const val MAX_MESSAGE_LENGTH = 4000
    }

    private fun getActiveEngine(): InferenceEngine {
        val modelId = prefsManager.getSelectedModelId()
        val model = ModelManager.getModelById(modelId)
        return liteRtEngine
    }

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

        viewModelScope.launch {
            try {
                repository.addMessage(convId, "user", message)

                // Build RAG context based on current chat context
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
                            _uiState.value = _uiState.value.copy(
                                isGenerating = false,
                                streamingText = "",
                                inferenceTimeMs = elapsed
                            )
                            // Bug 6: release the gate so the next message can be sent
                            sendingGate.set(false)
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
                    streamingText = ""
                )
                // Bug 6: release the gate on error so user is not permanently locked out
                sendingGate.set(false)
            }
        }
    }

    fun stopGeneration() {
        getActiveEngine().stopGeneration()
        generationJob?.cancel()
        // Bug 6: release the gate when generation is manually stopped
        sendingGate.set(false)
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
        getActiveEngine().stopGeneration()
    }
}
