package com.sylvester.rustsensei.llm

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LiteRtEngine(private val context: Context) : InferenceEngine {

    companion object {
        private const val TAG = "LiteRtEngine"
    }

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var isLoaded = false

    override suspend fun loadModel(modelPath: String, contextSize: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                engine?.close()
                conversation?.close()

                // Try GPU first, fall back to CPU
                val gpuConfig = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.GPU(),
                    cacheDir = context.cacheDir.absolutePath
                )
                try {
                    val newEngine = Engine(gpuConfig)
                    newEngine.initialize()
                    engine = newEngine
                    Log.i(TAG, "Model loaded (GPU): $modelPath")
                } catch (gpuError: Exception) {
                    Log.w(TAG, "GPU failed: ${gpuError.message}, trying CPU...")
                    val cpuConfig = EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        cacheDir = context.cacheDir.absolutePath
                    )
                    val newEngine = Engine(cpuConfig)
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

        return flow {
            val startTime = System.currentTimeMillis()
            var firstTokenTime: Long? = null
            var tokenCount = 0

            // Create conversation with default config (no custom sampler to avoid native crashes)
            val conv = try {
                currentEngine.createConversation(ConversationConfig())
            } catch (e: Exception) {
                Log.e(TAG, "createConversation failed: ${e.message}", e)
                throw RuntimeException("Failed to create conversation: ${e.message}")
            }
            conversation = conv

            try {
                // Use the synchronous sendMessage and emit the result
                // This avoids potential issues with the async Flow API
                val response = conv.sendMessage(prompt)
                val text = response.contents.toString()

                firstTokenTime = System.currentTimeMillis()
                tokenCount = 1

                // Emit the full response
                emit(text)

                val totalMs = System.currentTimeMillis() - startTime
                val prefillMs = (firstTokenTime - startTime).toFloat()
                Log.i(TAG, "Response: ${text.length} chars in ${totalMs}ms")
                onStats?.invoke(0f, 0f, prefillMs)
            } catch (e: Exception) {
                Log.e(TAG, "Generation failed: ${e.message}", e)
                throw RuntimeException("Generation failed: ${e.message}")
            } finally {
                try { conv.close() } catch (_: Exception) {}
                conversation = null
            }
        }
            .catch { e -> throw e }
            .flowOn(Dispatchers.IO)
            .buffer(capacity = 64, onBufferOverflow = BufferOverflow.SUSPEND)
    }

    override fun stopGeneration() {
        try {
            conversation?.cancelProcess()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling: ${e.message}")
        }
    }

    override fun clearCache() {
        try {
            conversation?.close()
            conversation = null
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing: ${e.message}")
        }
    }

    override suspend fun unloadModel() {
        withContext(Dispatchers.IO) {
            try {
                conversation?.close()
                engine?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error unloading: ${e.message}")
            }
            conversation = null
            engine = null
            isLoaded = false
        }
    }

    override fun isModelLoaded(): Boolean = isLoaded
}
