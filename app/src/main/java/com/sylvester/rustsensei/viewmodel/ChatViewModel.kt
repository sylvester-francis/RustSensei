package com.sylvester.rustsensei.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import com.sylvester.rustsensei.data.ChatMessage
import com.sylvester.rustsensei.data.ChatRepository
import com.sylvester.rustsensei.data.PreferencesManager
import com.sylvester.rustsensei.domain.ChatStreamEvent
import com.sylvester.rustsensei.domain.SendChatMessageUseCase
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.llm.InferenceEngine
import com.sylvester.rustsensei.llm.ModelLifecycle
import com.sylvester.rustsensei.llm.ModelReadyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

sealed class ChatContext {
    data object General : ChatContext()
    data class BookSection(val sectionId: String, val content: String) : ChatContext()
    data class Exercise(val exerciseId: String, val description: String, val userCode: String) : ChatContext()
}

@Immutable
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

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val engine: InferenceEngine,
    private val prefsManager: PreferencesManager,
    private val modelLifecycle: ModelLifecycle,
    private val sendChatMessage: SendChatMessageUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
        private const val MAX_MESSAGE_LENGTH = 4000
    }

    val modelState: StateFlow<ModelReadyState> = modelLifecycle.state

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _config = MutableStateFlow(prefsManager.loadInferenceConfig())
    val config: StateFlow<InferenceConfig> = _config.asStateFlow()

    private val _chatContext = MutableStateFlow<ChatContext>(ChatContext.General)
    val chatContext: StateFlow<ChatContext> = _chatContext.asStateFlow()

    private var generationJob: Job? = null
    private var messagesJob: Job? = null
    private val sendingGate = AtomicBoolean(false)

    // ── Config ──────────────────────────────────────────────────────

    fun updateConfig(config: InferenceConfig) {
        _config.value = config
        prefsManager.saveInferenceConfig(config)
    }

    fun applyModelDefaults(modelId: String) {
        val defaults = InferenceConfig.forModel(modelId)
        val current = _config.value
        val updated = current.copy(
            contextLength = defaults.contextLength,
            maxTokens = defaults.maxTokens
        )
        _config.value = updated
        prefsManager.saveInferenceConfig(updated)
    }

    fun setChatContext(context: ChatContext) { _chatContext.value = context }
    fun clearChatContext() { _chatContext.value = ChatContext.General }
    fun getContentVersion(): Int = prefsManager.getContentVersion()

    fun reloadModel() {
        viewModelScope.launch { modelLifecycle.ensureLoaded() }
    }

    // ── Conversation lifecycle ──────────────────────────────────────

    fun startNewConversation() {
        viewModelScope.launch {
            try {
                messagesJob?.cancel()
                engine.clearCache()
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
                engine.clearCache()
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

    // ── Message sending (delegates to UseCase) ──────────────────────

    fun sendMessage(text: String) {
        val convId = _uiState.value.currentConversationId ?: return
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        if (!sendingGate.compareAndSet(false, true)) return

        val message = trimmed.take(MAX_MESSAGE_LENGTH)
        val context = _chatContext.value
        if (context !is ChatContext.General) _chatContext.value = ChatContext.General

        modelLifecycle.cancelIdleTimer()

        _uiState.value = _uiState.value.copy(
            isGenerating = true,
            streamingText = "",
            inferenceTimeMs = 0,
            lastDecodeTokPerSec = 0f,
            lastTimeToFirstTokenMs = 0
        )

        generationJob = viewModelScope.launch {
            sendChatMessage(convId, message, context, _config.value)
                .onCompletion {
                    sendingGate.set(false)
                    modelLifecycle.scheduleIdleUnload()
                }
                .collect { event ->
                    when (event) {
                        is ChatStreamEvent.Token -> {
                            _uiState.value = _uiState.value.copy(streamingText = event.displayText)
                        }
                        is ChatStreamEvent.Completed -> {
                            val followUps = if (event.fullText.isNotEmpty())
                                generateFollowUps(event.fullText) else emptyList()
                            _uiState.value = _uiState.value.copy(
                                isGenerating = false,
                                streamingText = "",
                                inferenceTimeMs = event.elapsedMs,
                                lastDecodeTokPerSec = event.decodeTokPerSec,
                                lastTimeToFirstTokenMs = event.prefillMs,
                                followUpSuggestions = followUps
                            )
                        }
                        is ChatStreamEvent.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isGenerating = false,
                                streamingText = "",
                                errorMessage = event.message
                            )
                        }
                    }
                }
        }
    }

    fun stopGeneration() {
        engine.stopGeneration()
        generationJob?.cancel()
        sendingGate.set(false)
        _uiState.value = _uiState.value.copy(isGenerating = false, streamingText = "")
        modelLifecycle.scheduleIdleUnload()
    }

    // ── Helpers ─────────────────────────────────────────────────────

    fun exportConversation(): String = buildString {
        appendLine("# RustSensei Chat")
        appendLine()
        for (msg in _uiState.value.messages) {
            if (msg.role == "user") appendLine("**You:** ${msg.content}")
            else appendLine("**RustSensei:** ${msg.content}")
            appendLine()
        }
    }

    private fun generateFollowUps(lastResponse: String): List<String> {
        val followUps = mutableListOf<String>()
        val lower = lastResponse.lowercase()

        if ("ownership" in lower && "borrow" !in lower) followUps.add("How does borrowing work?")
        if ("borrow" in lower && "lifetime" !in lower) followUps.add("Explain lifetimes")
        if ("struct" in lower) followUps.add("Show me struct methods")
        if ("enum" in lower) followUps.add("How does match work with enums?")
        if ("result" in lower || "error" in lower) followUps.add("What's the ? operator?")
        if ("vec" in lower || "vector" in lower) followUps.add("How do I iterate over a Vec?")
        if ("trait" in lower) followUps.add("Show me trait bounds")
        if ("async" in lower) followUps.add("How does async/await work?")
        if ("closure" in lower) followUps.add("What are Fn, FnMut, FnOnce?")

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
        engine.stopGeneration()
    }
}
