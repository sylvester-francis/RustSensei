package com.sylvester.rustsensei.llm

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.ResponseCallback
import com.google.ai.edge.litertlm.SessionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class LiteRtEngine(private val context: Context) : InferenceEngine {

    companion object {
        private const val TAG = "LiteRtEngine"
    }

    private var engine: Engine? = null
    private var isLoaded = false

    override suspend fun loadModel(modelPath: String, contextSize: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                engine?.close()

                // Try GPU first, fall back to CPU
                try {
                    val config = EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.GPU(),
                        cacheDir = context.cacheDir.absolutePath
                    )
                    val newEngine = Engine(config)
                    newEngine.initialize()
                    engine = newEngine
                    Log.i(TAG, "Model loaded (GPU): $modelPath")
                } catch (gpuError: Exception) {
                    Log.w(TAG, "GPU failed: ${gpuError.message}, trying CPU...")
                    val config = EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        cacheDir = context.cacheDir.absolutePath
                    )
                    val newEngine = Engine(config)
                    newEngine.initialize()
                    engine = newEngine
                    Log.i(TAG, "Model loaded (CPU): $modelPath")
                }

                isLoaded = true
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load model: ${e.message}", e)
                isLoaded = false
                false
            }
        }
    }

    override fun generate(
        prompt: String,
        config: InferenceConfig,
        onStats: ((Float, Float, Float) -> Unit)?
    ): Flow<String> {
        val currentEngine = engine ?: return flow {
            throw RuntimeException("Model not loaded")
        }

        // Use low-level Session API — doesn't require stop tokens
        return callbackFlow {
            val startTime = System.currentTimeMillis()
            var firstTokenTime: Long? = null
            var tokenCount = 0

            val session = try {
                currentEngine.createSession(SessionConfig())
            } catch (e: Exception) {
                Log.e(TAG, "createSession failed: ${e.message}", e)
                close(RuntimeException("Failed to create session: ${e.message}"))
                return@callbackFlow
            }

            try {
                val input = listOf(InputData.Text(prompt))

                session.generateContentStream(input, object : ResponseCallback {
                    override fun onNext(partialResult: String) {
                        if (firstTokenTime == null && partialResult.isNotEmpty()) {
                            firstTokenTime = System.currentTimeMillis()
                            Log.i(TAG, "TTFT: ${firstTokenTime!! - startTime} ms")
                        }
                        tokenCount++
                        trySend(partialResult)
                    }

                    override fun onDone() {
                        val ftTime = firstTokenTime ?: startTime
                        val genMs = System.currentTimeMillis() - ftTime
                        val tokPerSec = if (genMs > 0) tokenCount * 1000f / genMs else 0f
                        val prefillMs = (ftTime - startTime).toFloat()
                        Log.i(TAG, "Done: $tokenCount chunks in ${genMs}ms (${"%.1f".format(tokPerSec)} tok/s)")
                        onStats?.invoke(0f, tokPerSec, prefillMs)
                        try { session.close() } catch (_: Exception) {}
                        close()
                    }

                    override fun onError(error: Throwable) {
                        Log.e(TAG, "Stream error: ${error.message}")
                        try { session.close() } catch (_: Exception) {}
                        close(RuntimeException(error.message))
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "generateContentStream failed: ${e.message}", e)
                try { session.close() } catch (_: Exception) {}
                close(RuntimeException(e.message))
            }

            awaitClose {
                try { session.cancelProcess() } catch (_: Exception) {}
                try { session.close() } catch (_: Exception) {}
            }
        }
            .flowOn(Dispatchers.IO)
            .buffer(capacity = 64, onBufferOverflow = BufferOverflow.SUSPEND)
    }

    override fun stopGeneration() {
        // Session cancel is handled in awaitClose
    }

    override fun clearCache() {
        // Sessions are per-request, no persistent cache to clear
    }

    override suspend fun unloadModel() {
        withContext(Dispatchers.IO) {
            try {
                engine?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error unloading: ${e.message}")
            }
            engine = null
            isLoaded = false
        }
    }

    override fun isModelLoaded(): Boolean = isLoaded
}
