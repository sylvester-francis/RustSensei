package com.sylvester.rustsensei.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.llm.DownloadState
import com.sylvester.rustsensei.llm.LlamaEngine
import com.sylvester.rustsensei.llm.ModelManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ModelState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    LOADING,
    READY,
    ERROR
}

data class ModelUiState(
    val modelState: ModelState = ModelState.NOT_DOWNLOADED,
    val downloadProgress: Float = 0f,
    val downloadedMB: Long = 0,
    val totalMB: Long = 0,
    val errorMessage: String? = null,
    val modelSizeMB: Long = 0
)

class ModelViewModel(application: Application) : AndroidViewModel(application) {

    val modelManager = ModelManager(application)

    private val _uiState = MutableStateFlow(ModelUiState())
    val uiState: StateFlow<ModelUiState> = _uiState.asStateFlow()

    // One-shot event for navigation (survives composition changes)
    private val _navigateToChat = MutableSharedFlow<Unit>()
    val navigateToChat: SharedFlow<Unit> = _navigateToChat.asSharedFlow()

    init {
        checkModelStatus()
    }

    fun checkModelStatus() {
        if (modelManager.isModelDownloaded()) {
            _uiState.value = ModelUiState(
                modelState = ModelState.DOWNLOADED,
                modelSizeMB = modelManager.getModelSizeMB()
            )
        } else {
            _uiState.value = ModelUiState(modelState = ModelState.NOT_DOWNLOADED)
        }
    }

    fun startDownload() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                modelState = ModelState.DOWNLOADING,
                errorMessage = null
            )

            modelManager.downloadModel().collect { state ->
                when (state) {
                    is DownloadState.Idle -> {}
                    is DownloadState.Downloading -> {
                        _uiState.value = _uiState.value.copy(
                            modelState = ModelState.DOWNLOADING,
                            downloadProgress = state.progress,
                            downloadedMB = state.downloadedMB,
                            totalMB = state.totalMB
                        )
                    }
                    is DownloadState.Completed -> {
                        _uiState.value = _uiState.value.copy(
                            modelState = ModelState.DOWNLOADED,
                            downloadProgress = 1f,
                            modelSizeMB = modelManager.getModelSizeMB()
                        )
                    }
                    is DownloadState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            modelState = ModelState.ERROR,
                            errorMessage = state.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Loads the model using viewModelScope — safe from composition lifecycle.
     * Emits a navigation event on success.
     */
    fun loadModel(llamaEngine: LlamaEngine) {
        if (_uiState.value.modelState == ModelState.LOADING) return // already loading

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(modelState = ModelState.LOADING)
            try {
                val modelPath = modelManager.modelFile.absolutePath
                val success = llamaEngine.loadModel(modelPath)
                if (success) {
                    _uiState.value = _uiState.value.copy(modelState = ModelState.READY)
                    _navigateToChat.emit(Unit)
                } else {
                    _uiState.value = _uiState.value.copy(
                        modelState = ModelState.ERROR,
                        errorMessage = "Failed to load model. The file may be corrupted — try deleting and re-downloading."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    modelState = ModelState.ERROR,
                    errorMessage = "Load error: ${e.message}"
                )
            }
        }
    }

    fun deleteModel() {
        modelManager.deleteModel()
        _uiState.value = ModelUiState(modelState = ModelState.NOT_DOWNLOADED)
    }
}
