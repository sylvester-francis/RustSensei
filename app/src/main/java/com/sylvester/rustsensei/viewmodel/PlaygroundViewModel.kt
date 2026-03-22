package com.sylvester.rustsensei.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.data.InferenceConfigProvider
import com.sylvester.rustsensei.domain.ExecutionEvent
import com.sylvester.rustsensei.domain.SimulateExecutionUseCase
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
data class PlaygroundUiState(
    val code: String = DEFAULT_CODE,
    val output: String = "",
    val isRunning: Boolean = false,
    val elapsedMs: Long = 0,
    val errorMessage: String? = null
) {
    companion object {
        const val DEFAULT_CODE = "fn main() {\n    println!(\"Hello, Rust!\");\n}"
    }
}

@HiltViewModel
class PlaygroundViewModel @Inject constructor(
    private val simulateExecution: SimulateExecutionUseCase,
    private val configProvider: InferenceConfigProvider,
    private val modelLifecycle: ModelLifecycle
) : ViewModel() {

    val modelState: StateFlow<ModelReadyState> = modelLifecycle.state

    private val _uiState = MutableStateFlow(PlaygroundUiState())
    val uiState: StateFlow<PlaygroundUiState> = _uiState.asStateFlow()

    private var runJob: Job? = null

    fun updateCode(code: String) {
        _uiState.value = _uiState.value.copy(code = code)
    }

    fun run() {
        val code = _uiState.value.code.trim()
        if (code.isBlank()) return

        _uiState.value = _uiState.value.copy(
            isRunning = true,
            output = "",
            errorMessage = null,
            elapsedMs = 0
        )

        val config = configProvider.loadInferenceConfig()
        runJob = viewModelScope.launch {
            simulateExecution(code, config).collect { event ->
                when (event) {
                    is ExecutionEvent.Output -> {
                        _uiState.value = _uiState.value.copy(output = event.text)
                    }
                    is ExecutionEvent.Completed -> {
                        _uiState.value = _uiState.value.copy(
                            isRunning = false,
                            output = event.fullOutput,
                            elapsedMs = event.elapsedMs
                        )
                    }
                    is ExecutionEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isRunning = false,
                            errorMessage = event.message
                        )
                    }
                }
            }
        }
    }

    fun stop() {
        runJob?.cancel()
        _uiState.value = _uiState.value.copy(isRunning = false)
    }

    fun clearOutput() {
        _uiState.value = _uiState.value.copy(output = "", errorMessage = null, elapsedMs = 0)
    }
}
