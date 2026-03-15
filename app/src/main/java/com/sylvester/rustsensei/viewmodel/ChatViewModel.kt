package com.sylvester.rustsensei.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.RustSenseiApplication
import com.sylvester.rustsensei.data.ChatMessage
import com.sylvester.rustsensei.data.ChatRepository
import com.sylvester.rustsensei.llm.ChatTemplateFormatter
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.llm.LlamaEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val inferenceTimeMs: Long = 0
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RustSenseiApplication
    private val repository: ChatRepository = app.repository

    val llamaEngine = LlamaEngine()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // P1 Fix #14: load persisted config on init, save on change
    private val prefsManager = app.preferencesManager
    private val _config = MutableStateFlow(prefsManager.loadInferenceConfig())
    val config: StateFlow<InferenceConfig> = _config.asStateFlow()

    private val _chatContext = MutableStateFlow<ChatContext>(ChatContext.General)
    val chatContext: StateFlow<ChatContext> = _chatContext.asStateFlow()

    private var generationJob: Job? = null
    private var messagesJob: Job? = null

    fun updateConfig(config: InferenceConfig) {
        _config.value = config
        prefsManager.saveInferenceConfig(config)
    }

    fun setChatContext(context: ChatContext) {
        _chatContext.value = context
    }

    fun clearChatContext() {
        _chatContext.value = ChatContext.General
    }

    fun startNewConversation() {
        viewModelScope.launch {
            messagesJob?.cancel()
            llamaEngine.clearCache()
            val convId = repository.createConversation()
            _uiState.value = ChatUiState(currentConversationId = convId)
            _chatContext.value = ChatContext.General
            observeMessages(convId)
        }
    }

    fun loadConversation(conversationId: Long) {
        viewModelScope.launch {
            messagesJob?.cancel()
            llamaEngine.clearCache()
            _uiState.value = _uiState.value.copy(currentConversationId = conversationId)
            observeMessages(conversationId)
        }
    }

    private fun observeMessages(conversationId: Long) {
        messagesJob = viewModelScope.launch {
            repository.getMessages(conversationId).collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
    }

    fun sendMessage(text: String) {
        val convId = _uiState.value.currentConversationId ?: return
        if (text.isBlank() || _uiState.value.isGenerating) return

        viewModelScope.launch {
            repository.addMessage(convId, "user", text)

            // Build RAG context based on current chat context
            val ragContext = when (val ctx = _chatContext.value) {
                is ChatContext.General -> {
                    withContext(Dispatchers.IO) {
                        app.ragRetriever.retrieveContext(text)
                    }
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
                inferenceTimeMs = 0
            )

            val startTime = System.currentTimeMillis()
            val tokenBuffer = StringBuilder()

            generationJob = viewModelScope.launch {
                llamaEngine.generate(prompt, _config.value)
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            streamingText = ""
                        )
                        repository.addMessage(convId, "assistant", "Error: ${e.message}")
                    }
                    .onCompletion {
                        val elapsed = System.currentTimeMillis() - startTime
                        val finalText = tokenBuffer.toString().trim()
                        if (finalText.isNotEmpty()) {
                            repository.addMessage(convId, "assistant", finalText)
                        }
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            streamingText = "",
                            inferenceTimeMs = elapsed
                        )
                    }
                    .collect { token ->
                        tokenBuffer.append(token)
                        _uiState.value = _uiState.value.copy(
                            streamingText = tokenBuffer.toString()
                        )
                    }
            }
        }
    }

    fun stopGeneration() {
        llamaEngine.stopGeneration()
        generationJob?.cancel()
    }

    fun getConversations() = repository.getConversations()

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
            if (_uiState.value.currentConversationId == conversationId) {
                startNewConversation()
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            repository.clearAllData()
            startNewConversation()
        }
    }

    override fun onCleared() {
        super.onCleared()
        llamaEngine.stopGeneration()
    }
}
