package com.sylvester.rustsensei.viewmodel

import androidx.compose.runtime.Immutable
import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.data.PreferencesManager
import com.sylvester.rustsensei.llm.DownloadState
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.llm.InferenceEngine
import com.sylvester.rustsensei.llm.ModelForegroundService
import com.sylvester.rustsensei.llm.ModelInfo
import com.sylvester.rustsensei.llm.ModelLifecycle
import com.sylvester.rustsensei.llm.ModelManager
import com.sylvester.rustsensei.llm.ModelReadyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ModelState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    DOWNLOAD_INCOMPLETE,
    LOADING,
    READY,
    ERROR
}

@Immutable
data class ModelUiState(
    val modelState: ModelState = ModelState.NOT_DOWNLOADED,
    val downloadProgress: Float = 0f,
    val downloadedMB: Long = 0,
    val totalMB: Long = 0,
    val downloadSpeedMBps: Float = 0f,
    val estimatedSecondsLeft: Long = 0,
    val errorMessage: String? = null,
    val modelSizeMB: Long = 0,
    val availableModels: List<ModelInfo> = ModelManager.AVAILABLE_MODELS,
    val selectedModelId: String = "litert-1b-gemma",
    val loadedModelId: String? = null,
    val downloadedModelIds: Set<String> = emptySet()
)

@HiltViewModel
class ModelViewModel @Inject constructor(
    private val application: Application,
    private val modelManager: ModelManager,
    private val prefsManager: PreferencesManager,
    private val engine: InferenceEngine,
    private val modelLifecycle: ModelLifecycle
) : ViewModel() {

    companion object {
        private const val TAG = "ModelViewModel"
    }

    private val _uiState = MutableStateFlow(ModelUiState())
    val uiState: StateFlow<ModelUiState> = _uiState.asStateFlow()

    private val _navigateToChat = MutableSharedFlow<Unit>()
    val navigateToChat: SharedFlow<Unit> = _navigateToChat.asSharedFlow()

    init {
        val savedModelId = prefsManager.getSelectedModelId()
        _uiState.value = _uiState.value.copy(selectedModelId = savedModelId)
        checkModelStatus()

        if (_uiState.value.modelState == ModelState.DOWNLOADED && !engine.isModelLoaded()) {
            loadModel()
        }
    }

    fun checkModelStatus() {
        val selectedId = _uiState.value.selectedModelId
        val selectedModel = ModelManager.getModelById(selectedId) ?: ModelManager.AVAILABLE_MODELS[0]
        val downloadedIds = ModelManager.AVAILABLE_MODELS
            .filter { modelManager.isModelDownloaded(it) }
            .map { it.id }
            .toSet()

        val state = if (modelManager.isModelDownloaded(selectedModel)) {
            ModelState.DOWNLOADED
        } else if (modelManager.hasTempFile(selectedModel)) {
            ModelState.DOWNLOAD_INCOMPLETE
        } else {
            ModelState.NOT_DOWNLOADED
        }

        _uiState.value = _uiState.value.copy(
            modelState = state,
            modelSizeMB = modelManager.getModelSizeMB(selectedModel),
            downloadedModelIds = downloadedIds
        )
    }

    fun selectModel(modelId: String) {
        prefsManager.saveSelectedModelId(modelId)
        _uiState.value = _uiState.value.copy(selectedModelId = modelId)
        checkModelStatus()
    }

    fun getSelectedModelInfo(): ModelInfo {
        val selectedId = _uiState.value.selectedModelId
        val model = ModelManager.getModelById(selectedId)
        if (model == null) {
            Log.w(TAG, "Persisted model ID '$selectedId' does not match any available model. " +
                "Falling back to '${ModelManager.AVAILABLE_MODELS[0].id}'.")
        }
        return model ?: ModelManager.AVAILABLE_MODELS[0]
    }

    fun startDownload() {
        val modelInfo = getSelectedModelInfo()
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    modelState = ModelState.DOWNLOADING,
                    errorMessage = null
                )

                modelManager.downloadModel(modelInfo).collect { state ->
                    when (state) {
                        is DownloadState.Idle -> {}
                        is DownloadState.Downloading -> {
                            _uiState.value = _uiState.value.copy(
                                modelState = ModelState.DOWNLOADING,
                                downloadProgress = state.progress,
                                downloadedMB = state.downloadedMB,
                                totalMB = state.totalMB,
                                downloadSpeedMBps = state.speedMBps,
                                estimatedSecondsLeft = state.estimatedSecondsLeft
                            )
                        }
                        is DownloadState.Completed -> {
                            val downloadedIds = _uiState.value.downloadedModelIds + modelInfo.id
                            _uiState.value = _uiState.value.copy(
                                modelState = ModelState.DOWNLOADED,
                                downloadProgress = 1f,
                                modelSizeMB = modelManager.getModelSizeMB(modelInfo),
                                downloadedModelIds = downloadedIds
                            )
                            modelLifecycle.refreshState()
                        }
                        is DownloadState.Error -> {
                            _uiState.value = _uiState.value.copy(
                                modelState = ModelState.ERROR,
                                errorMessage = state.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in startDownload: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    modelState = ModelState.ERROR,
                    errorMessage = "Download failed: ${e.message}"
                )
            }
        }
    }

    fun loadModel() {
        val currentState = _uiState.value.modelState
        if (currentState == ModelState.LOADING || currentState == ModelState.READY) return

        val modelInfo = getSelectedModelInfo()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(modelState = ModelState.LOADING)
            try {
                val currentLoaded = _uiState.value.loadedModelId
                if (currentLoaded != null && currentLoaded != modelInfo.id) {
                    engine.unloadModel()
                }

                val modelPath = modelManager.getModelFile(modelInfo).absolutePath
                val contextSize = InferenceConfig.forModel(modelInfo.id).contextLength
                val success = engine.loadModel(modelPath, contextSize)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        modelState = ModelState.READY,
                        loadedModelId = modelInfo.id
                    )
                    modelLifecycle.refreshState()
                    startModelService()
                    _navigateToChat.emit(Unit)
                } else {
                    _uiState.value = _uiState.value.copy(
                        modelState = ModelState.ERROR,
                        errorMessage = "Failed to initialize ${modelInfo.displayName}. " +
                                "The model file may be corrupted — try deleting and re-downloading."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    modelState = ModelState.ERROR,
                    errorMessage = "Load error: ${e.message ?: "Unknown error"}. " +
                            "Try deleting and re-downloading the model."
                )
            }
        }
    }

    fun switchModel(modelId: String) {
        selectModel(modelId)
        val modelInfo = getSelectedModelInfo()
        if (modelManager.isModelDownloaded(modelInfo)) {
            loadModel()
        }
    }

    fun deleteModel() {
        val modelInfo = getSelectedModelInfo()
        stopModelService()
        viewModelScope.launch {
            modelLifecycle.unload()
        }
        modelManager.deleteModel(modelInfo)
        val downloadedIds = _uiState.value.downloadedModelIds - modelInfo.id
        _uiState.value = _uiState.value.copy(
            modelState = ModelState.NOT_DOWNLOADED,
            downloadedModelIds = downloadedIds,
            loadedModelId = if (_uiState.value.loadedModelId == modelInfo.id) null else _uiState.value.loadedModelId
        )
        modelLifecycle.refreshState()
    }

    fun isModelDownloaded(): Boolean = modelManager.isModelDownloaded(getSelectedModelInfo())

    fun getActiveBackend(): String = modelLifecycle.getActiveBackend()

    fun getModelSizeMB(modelInfo: ModelInfo): Long = modelManager.getModelSizeMB(modelInfo)

    private fun startModelService() {
        try {
            val intent = Intent(application, ModelForegroundService::class.java)
            application.startForegroundService(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not start foreground service: ${e.message}")
        }
    }

    private fun stopModelService() {
        try {
            val intent = Intent(application, ModelForegroundService::class.java)
            application.stopService(intent)
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        stopModelService()
    }
}
