package com.sylvester.rustsensei.llm

import android.util.Log
import com.sylvester.rustsensei.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the on-device model's in-memory lifecycle.
 *
 * Responsibilities (SRP):
 *  - Track whether the model is loaded, unloaded, or being loaded.
 *  - Transparently reload after idle-unload so callers never hit a dead engine.
 *  - Schedule idle-unload to reclaim ~1 GB of RAM after inactivity.
 *
 * Every ViewModel that needs inference injects this instead of touching
 * [LiteRtEngine] lifecycle methods directly (DIP).
 */
@Singleton
class ModelLifecycleManager @Inject constructor(
    private val engine: InferenceEngine,
    private val modelManager: ModelManager,
    private val prefs: PreferencesManager
) : ModelLifecycle {

    companion object {
        private const val TAG = "ModelLifecycle"
        private const val IDLE_UNLOAD_DELAY_MS = 5 * 60 * 1000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val loadMutex = Mutex()
    private var idleJob: Job? = null

    private val _state = MutableStateFlow(resolveCurrentState())
    override val state: StateFlow<ModelReadyState> = _state.asStateFlow()

    // ── Public API ──────────────────────────────────────────────────

    /**
     * Guarantees the model is loaded before returning.
     * Safe to call from any ViewModel — concurrent callers are serialised
     * by [loadMutex] and the second caller gets a fast-path return.
     */
    override suspend fun ensureLoaded(): Boolean {
        cancelIdleTimer()
        if (engine.isModelLoaded()) {
            _state.value = ModelReadyState.READY
            return true
        }
        return loadMutex.withLock {
            // Double-check after acquiring lock
            if (engine.isModelLoaded()) {
                _state.value = ModelReadyState.READY
                return@withLock true
            }

            val modelId = prefs.getSelectedModelId()
            val info = ModelManager.getModelById(modelId)
            if (info == null || !modelManager.isModelDownloaded(info)) {
                _state.value = ModelReadyState.NOT_DOWNLOADED
                return@withLock false
            }

            _state.value = ModelReadyState.LOADING
            val file = modelManager.getModelFile(info)
            val contextSize = InferenceConfig.forModel(modelId).contextLength
            val success = engine.loadModel(file.absolutePath, contextSize)
            _state.value = if (success) ModelReadyState.READY else ModelReadyState.ERROR
            if (success) Log.i(TAG, "Model loaded: ${info.displayName}")
            else Log.e(TAG, "Failed to load: ${info.displayName}")
            success
        }
    }

    override fun scheduleIdleUnload() {
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(IDLE_UNLOAD_DELAY_MS)
            Log.i(TAG, "Idle timeout — unloading model to save battery")
            engine.unloadModel()
            refreshState()
        }
    }

    override fun cancelIdleTimer() {
        idleJob?.cancel()
        idleJob = null
    }

    override suspend fun unload() {
        cancelIdleTimer()
        engine.unloadModel()
        refreshState()
    }

    override fun refreshState() {
        _state.value = resolveCurrentState()
    }

    override fun getActiveBackend(): String = engine.getActiveBackend()

    override fun isModelLoaded(): Boolean = engine.isModelLoaded()

    // ── Internal ────────────────────────────────────────────────────

    private fun resolveCurrentState(): ModelReadyState {
        val modelId = prefs.getSelectedModelId()
        val info = ModelManager.getModelById(modelId)
        return when {
            info != null && engine.isModelLoaded() -> ModelReadyState.READY
            info != null && modelManager.isModelDownloaded(info) -> ModelReadyState.DOWNLOADED
            else -> ModelReadyState.NOT_DOWNLOADED
        }
    }
}

/**
 * Abstraction for model lifecycle operations.
 * ViewModels and UseCases depend on this interface (DIP),
 * enabling unit testing with [FakeModelLifecycle] test doubles.
 */
interface ModelLifecycle {
    val state: StateFlow<ModelReadyState>
    suspend fun ensureLoaded(): Boolean
    suspend fun unload()
    fun scheduleIdleUnload()
    fun cancelIdleTimer()
    fun refreshState()
    fun getActiveBackend(): String
    fun isModelLoaded(): Boolean
}

/** Represents the model's readiness from the UI's perspective. */
enum class ModelReadyState {
    /** No model file on disk for the selected model ID. */
    NOT_DOWNLOADED,
    /** Model file exists on disk but is not loaded into memory (e.g. after idle-unload). */
    DOWNLOADED,
    /** Model is being loaded into GPU/CPU memory. */
    LOADING,
    /** Model is in memory and ready for inference. */
    READY,
    /** Last load attempt failed. */
    ERROR
}
