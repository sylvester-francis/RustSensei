package com.sylvester.rustsensei.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.RustSenseiApplication
import com.sylvester.rustsensei.llm.DownloadState
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.llm.InferenceEngine
import com.sylvester.rustsensei.llm.LiteRtEngine

import com.sylvester.rustsensei.llm.ModelForegroundService
import com.sylvester.rustsensei.llm.ModelInfo
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
    val downloadSpeedMBps: Float = 0f,
    val estimatedSecondsLeft: Long = 0,
    val errorMessage: String? = null,
    val modelSizeMB: Long = 0,
    val availableModels: List<ModelInfo> = ModelManager.AVAILABLE_MODELS,
    val selectedModelId: String = "litert-0.6b",
    val loadedModelId: String? = null,
    val downloadedModelIds: Set<String> = emptySet()
)

class ModelViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RustSenseiApplication
    val modelManager = ModelManager(application)
    private val prefsManager = app.preferencesManager

    private val _uiState = MutableStateFlow(ModelUiState())
    val uiState: StateFlow<ModelUiState> = _uiState.asStateFlow()

    private val _navigateToChat = MutableSharedFlow<Unit>()
    val navigateToChat: SharedFlow<Unit> = _navigateToChat.asSharedFlow()

    init {
        val savedModelId = prefsManager.getSelectedModelId()
        _uiState.value = _uiState.value.copy(selectedModelId = savedModelId)
        checkModelStatus()
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
        return ModelManager.getModelById(_uiState.value.selectedModelId)
            ?: ModelManager.AVAILABLE_MODELS[0]
    }

    fun startDownload() {
        val modelInfo = getSelectedModelInfo()
        viewModelScope.launch {
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

    fun loadModel(liteRtEngine: LiteRtEngine) {
        if (_uiState.value.modelState == ModelState.LOADING) return

        val modelInfo = getSelectedModelInfo()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(modelState = ModelState.LOADING)
            try {
                // Unload current model first if a different one is loaded
                val currentLoaded = _uiState.value.loadedModelId
                if (currentLoaded != null && currentLoaded != modelInfo.id) {
                    // Unload the previously active engine
                    val previousModel = ModelManager.getModelById(currentLoaded)
                    if (true) { // LiteRT only
                        liteRtEngine.unloadModel()
                    } else {
                        // no-op: only LiteRT now
                    }
                }

                val modelPath = modelManager.getModelFile(modelInfo).absolutePath
                val contextSize = InferenceConfig.forModel(modelInfo.id).contextLength
                val activeEngine: InferenceEngine = liteRtEngine
                val success = activeEngine.loadModel(modelPath, contextSize)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        modelState = ModelState.READY,
                        loadedModelId = modelInfo.id
                    )
                    startModelService()
                    _navigateToChat.emit(Unit)
                } else {
                    _uiState.value = _uiState.value.copy(
                        modelState = ModelState.ERROR,
                        errorMessage = "Failed to load ${modelInfo.displayName}. The file may be corrupted \u2014 try deleting and re-downloading."
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

    fun switchModel(modelId: String, liteRtEngine: LiteRtEngine) {
        selectModel(modelId)
        val modelInfo = getSelectedModelInfo()
        if (modelManager.isModelDownloaded(modelInfo)) {
            loadModel(liteRtEngine)
        }
        // If not downloaded, the UI will show the download button
    }

    fun deleteModel() {
        val modelInfo = getSelectedModelInfo()
        stopModelService()
        modelManager.deleteModel(modelInfo)
        val downloadedIds = _uiState.value.downloadedModelIds - modelInfo.id
        _uiState.value = _uiState.value.copy(
            modelState = ModelState.NOT_DOWNLOADED,
            downloadedModelIds = downloadedIds,
            loadedModelId = if (_uiState.value.loadedModelId == modelInfo.id) null else _uiState.value.loadedModelId
        )
    }

    fun isModelDownloaded(): Boolean = modelManager.isModelDownloaded(getSelectedModelInfo())

    private fun startModelService() {
        try {
            val context = getApplication<Application>()
            val intent = Intent(context, ModelForegroundService::class.java)
            context.startForegroundService(intent)
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException on Android 12+ when app is in background
            android.util.Log.w("ModelViewModel", "Could not start foreground service: ${e.message}")
        }
    }

    private fun stopModelService() {
        try {
            val context = getApplication<Application>()
            val intent = Intent(context, ModelForegroundService::class.java)
            context.stopService(intent)
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        stopModelService()
    }
}
