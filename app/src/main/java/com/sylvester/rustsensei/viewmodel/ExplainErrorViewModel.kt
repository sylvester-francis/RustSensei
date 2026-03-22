package com.sylvester.rustsensei.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.data.PreferencesManager
import com.sylvester.rustsensei.domain.ErrorCodeParser
import com.sylvester.rustsensei.domain.ErrorExplanationEvent
import com.sylvester.rustsensei.domain.ExplainErrorUseCase
import com.sylvester.rustsensei.llm.ModelLifecycle
import com.sylvester.rustsensei.llm.ModelReadyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ExplainErrorUiState(
    val inputText: String = "",
    val detectedCode: String? = null,
    val explanationText: String = "",
    val isExplaining: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ExplainErrorViewModel @Inject constructor(
    private val explainError: ExplainErrorUseCase,
    private val preferencesManager: PreferencesManager,
    private val modelLifecycle: ModelLifecycle
) : ViewModel() {

    val modelState: StateFlow<ModelReadyState> = modelLifecycle.state

    private val _uiState = MutableStateFlow(ExplainErrorUiState())
    val uiState: StateFlow<ExplainErrorUiState> = _uiState.asStateFlow()

    private var explanationJob: Job? = null

    fun updateInput(text: String) {
        val detectedCode = ErrorCodeParser.extractFirstCode(text)
        _uiState.value = _uiState.value.copy(
            inputText = text,
            detectedCode = detectedCode
        )
    }

    fun explain() {
        val input = _uiState.value.inputText.trim()
        if (input.isBlank()) return

        _uiState.value = _uiState.value.copy(
            isExplaining = true,
            explanationText = "",
            errorMessage = null
        )

        val config = preferencesManager.loadInferenceConfig()

        explanationJob = viewModelScope.launch {
            explainError(input, config).collect { event ->
                when (event) {
                    is ErrorExplanationEvent.Token -> {
                        _uiState.value = _uiState.value.copy(
                            explanationText = event.displayText
                        )
                    }
                    is ErrorExplanationEvent.Completed -> {
                        _uiState.value = _uiState.value.copy(
                            isExplaining = false,
                            explanationText = event.fullText
                        )
                    }
                    is ErrorExplanationEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isExplaining = false,
                            errorMessage = event.message
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun reset() {
        explanationJob?.cancel()
        _uiState.value = ExplainErrorUiState()
    }
}
