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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val streamingText: String = "",
    val currentConversationId: Long? = null,
    val inferenceTimeMs: Long = 0
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository =
        (application as RustSenseiApplication).repository

    val llamaEngine = LlamaEngine()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _config = MutableStateFlow(InferenceConfig())
    val config: StateFlow<InferenceConfig> = _config.asStateFlow()

    private var generationJob: Job? = null
    private var messagesJob: Job? = null

    fun updateConfig(config: InferenceConfig) {
        _config.value = config
    }

    fun startNewConversation() {
        viewModelScope.launch {
            messagesJob?.cancel()
            val convId = repository.createConversation()
            _uiState.value = ChatUiState(currentConversationId = convId)
            observeMessages(convId)
        }
    }

    fun loadConversation(conversationId: Long) {
        viewModelScope.launch {
            messagesJob?.cancel()
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
            // Save user message
            repository.addMessage(convId, "user", text)

            // Build the prompt from conversation history
            val allMessages = repository.getMessagesOnce(convId)
            val prompt = ChatTemplateFormatter.formatMessages(allMessages, _config.value.contextLength)

            // Start generation
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
                        // Save error as assistant message
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
