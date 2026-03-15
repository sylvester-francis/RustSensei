package com.sylvester.rustsensei.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class LlamaEngine {

    companion object {
        init {
            System.loadLibrary("rustsensei-llm")
        }
    }

    // Callbacks from native code
    private var tokenCallback: ((String) -> Unit)? = null
    private var completeCallback: (() -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null

    // Called from JNI
    @Suppress("unused")
    fun onNativeToken(token: String) {
        tokenCallback?.invoke(token)
    }

    @Suppress("unused")
    fun onNativeComplete() {
        completeCallback?.invoke()
    }

    @Suppress("unused")
    fun onNativeError(error: String) {
        errorCallback?.invoke(error)
    }

    suspend fun loadModel(modelPath: String, contextSize: Int = 2048): Boolean {
        return withContext(Dispatchers.IO) {
            loadModelNative(modelPath, contextSize)
        }
    }

    fun generate(
        prompt: String,
        config: InferenceConfig = InferenceConfig()
    ): Flow<String> = callbackFlow {
        tokenCallback = { token ->
            trySend(token)
        }
        completeCallback = {
            close()
        }
        errorCallback = { error ->
            close(RuntimeException(error))
        }

        // Run inference on a background thread
        val thread = Thread {
            generateNative(prompt, config.maxTokens, config.temperature, config.topP)
        }
        thread.start()

        awaitClose {
            stopGenerationNative()
            tokenCallback = null
            completeCallback = null
            errorCallback = null
        }
    }

    fun stopGeneration() {
        stopGenerationNative()
    }

    suspend fun unloadModel() {
        withContext(Dispatchers.IO) {
            unloadModelNative()
        }
    }

    fun isModelLoaded(): Boolean = isModelLoadedNative()

    // Native methods
    private external fun loadModelNative(modelPath: String, contextSize: Int): Boolean
    private external fun generateNative(prompt: String, maxTokens: Int, temperature: Float, topP: Float)
    private external fun stopGenerationNative()
    private external fun unloadModelNative()
    private external fun isModelLoadedNative(): Boolean
}
