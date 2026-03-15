package com.sylvester.rustsensei.llm

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LiteRtEngine(private val context: Context) : InferenceEngine {

    companion object {
        private const val TAG = "LiteRtEngine"
    }

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var isLoaded = false

    @OptIn(ExperimentalApi::class)
    override suspend fun loadModel(modelPath: String, contextSize: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                engine?.close()
                conversation?.close()

                // Use GPU backend — same as Google's AI Edge Gallery.
                // On Tensor G3 / Mali-G715, this uses WebGPU/Vulkan (not OpenCL).
                // The "Cannot find OpenCL" warning is non-fatal.
                val config = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.GPU(),
                    cacheDir = context.cacheDir.absolutePath
                )
                val newEngine = Engine(config)
                newEngine.initialize()
                engine = newEngine

                // Match Google Gallery's pattern: disable constrained decoding
                ExperimentalFlags.enableConversationConstrainedDecoding = false

                isLoaded = true
                Log.i(TAG, "Model loaded (GPU): $modelPath")
                true
            } catch (e: Exception) {
                Log.e(TAG, "GPU load failed, trying CPU: ${e.message}")
                try {
                    val cpuConfig = EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        cacheDir = context.cacheDir.absolutePath
                    )
                    val newEngine = Engine(cpuConfig)
                    newEngine.initialize()
                    engine = newEngine
                    ExperimentalFlags.enableConversationConstrainedDecoding = false
                    isLoaded = true
                    Log.i(TAG, "Model loaded (CPU): $modelPath")
                    true
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to load model: ${e2.message}", e2)
                    isLoaded = false
                    false
                }
            }
        }
    }

    @OptIn(ExperimentalApi::class)
    override fun generate(
        prompt: String,
        config: InferenceConfig,
        onStats: ((Float, Float, Float) -> Unit)?
    ): Flow<String> {
        val currentEngine = engine ?: return flow {
            throw RuntimeException("Model not loaded")
        }

        val startTime = System.currentTimeMillis()
        var firstTokenTime: Long? = null
        var tokenCount = 0

        // Create conversation with SamplerConfig — matches Google Gallery's pattern exactly
        val conv = try {
            ExperimentalFlags.enableConversationConstrainedDecoding = false
            currentEngine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(
                        topK = 40,
                        topP = config.topP.toDouble(),
                        temperature = config.temperature.toDouble()
                    )
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "createConversation failed: ${e.message}", e)
            return flow { throw RuntimeException("Failed to create conversation: ${e.message}") }
        }
        conversation = conv

        // sendMessageAsync(String) returns Flow<Message>
        return conv.sendMessageAsync(prompt)
            .map { message: Message ->
                if (firstTokenTime == null) {
                    firstTokenTime = System.currentTimeMillis()
                    Log.i(TAG, "TTFT: ${firstTokenTime!! - startTime} ms")
                }
                tokenCount++
                message.contents.toString()
            }
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
