package com.sylvester.rustsensei.llm

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException

class LiteRtEngine(private val context: Context) : InferenceEngine {

    companion object {
        private const val TAG = "LiteRtEngine"

        // Bug 5: single source of truth for sampler parameters, used in both
        // loadModel() and clearCache() to prevent configuration drift.
        val DEFAULT_SAMPLER = SamplerConfig(
            topK = 40,
            topP = 0.95,
            temperature = 0.7
        )
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

                // Use GPU backend — WebGPU/Vulkan on Tensor G3
                val config = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.GPU(),
                    cacheDir = context.cacheDir.absolutePath
                )

                val newEngine = Engine(config)
                newEngine.initialize()
                engine = newEngine

                // Create initial conversation — exact Google pattern
                ExperimentalFlags.enableConversationConstrainedDecoding = false
                val conv = newEngine.createConversation(
                    ConversationConfig(
                        samplerConfig = DEFAULT_SAMPLER
                    )
                )
                ExperimentalFlags.enableConversationConstrainedDecoding = false
                conversation = conv

                isLoaded = true
                Log.i(TAG, "Model loaded: $modelPath")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load: ${e.message}", e)
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
        val conv = conversation
        if (conv == null || engine == null) {
            return flow {
                emit("Error: Model not loaded. Please load a model first.")
            }
        }

        // Exact Google Gallery pattern:
        // conversation.sendMessageAsync(Contents.of(contents), MessageCallback)
        return callbackFlow {
            try {
                val startTime = System.currentTimeMillis()
                var firstTokenTime: Long? = null
                var tokenCount = 0

                val contents = listOf(Content.Text(prompt))

                conv.sendMessageAsync(
                    Contents.of(contents),
                    object : MessageCallback {
                        override fun onMessage(message: Message) {
                            try {
                                val text = message.toString()

                                // Skip control tokens (Google does this)
                                if (text.startsWith("<ctrl")) return

                                if (firstTokenTime == null && text.isNotEmpty()) {
                                    val now = System.currentTimeMillis()
                                    firstTokenTime = now
                                    Log.i(TAG, "TTFT: ${now - startTime} ms")
                                }
                                tokenCount++
                                trySend(text)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in onMessage: ${e.message}", e)
                            }
                        }

                        override fun onDone() {
                            try {
                                val ftTime = firstTokenTime ?: startTime
                                val genMs = System.currentTimeMillis() - ftTime
                                val tokPerSec = if (genMs > 0) tokenCount * 1000f / genMs else 0f
                                Log.i(TAG, "Done: $tokenCount tokens, ${"%.1f".format(tokPerSec)} tok/s")
                                onStats?.invoke(0f, tokPerSec, (ftTime - startTime).toFloat())
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in onDone stats: ${e.message}", e)
                            }
                            close()
                        }

                        override fun onError(throwable: Throwable) {
                            if (throwable is CancellationException) {
                                Log.i(TAG, "Inference cancelled")
                                close()
                            } else {
                                Log.e(TAG, "Error: ${throwable.message}")
                                close(RuntimeException(throwable.message))
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error starting generation: ${e.message}", e)
                trySend("Error: ${e.message}")
                close()
            }

            awaitClose {
                try { conv.cancelProcess() } catch (_: Exception) {}
            }
        }.buffer(capacity = 64, onBufferOverflow = BufferOverflow.SUSPEND)
    }

    override fun stopGeneration() {
        try {
            conversation?.cancelProcess()
        } catch (e: Exception) {
            Log.w(TAG, "Cancel error: ${e.message}")
        }
    }

    @OptIn(ExperimentalApi::class)
    override fun clearCache() {
        // Reset conversation — reuse engine (Google's pattern)
        try {
            val eng = engine ?: return
            conversation?.close()
            ExperimentalFlags.enableConversationConstrainedDecoding = false
            // Bug 5: use the same DEFAULT_SAMPLER as loadModel() to avoid config drift
            conversation = eng.createConversation(
                ConversationConfig(
                    samplerConfig = DEFAULT_SAMPLER
                )
            )
            ExperimentalFlags.enableConversationConstrainedDecoding = false
        } catch (e: Exception) {
            Log.w(TAG, "Reset conversation error: ${e.message}")
        }
    }

    override suspend fun unloadModel() {
        withContext(Dispatchers.IO) {
            try {
                conversation?.close()
                engine?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Unload error: ${e.message}")
            }
            conversation = null
            engine = null
            isLoaded = false
        }
    }

    override fun isModelLoaded(): Boolean = isLoaded
}
