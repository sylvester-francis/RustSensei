package com.sylvester.rustsensei.llm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float, val downloadedMB: Long, val totalMB: Long) : DownloadState()
    data object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}

data class ModelInfo(
    val id: String,
    val displayName: String,
    val parameterSize: String,
    val filename: String,
    val downloadUrl: String,
    val expectedSizeBytes: Long,
    val description: String,
    val ramRequired: String
)

class ModelManager(private val context: Context) {

    companion object {
        private const val MODEL_DIR = "models"

        val AVAILABLE_MODELS = listOf(
            ModelInfo(
                id = "qwen3-4b",
                displayName = "Qwen3 4B",
                parameterSize = "4B",
                filename = "qwen3-4b.Q4_K_M.gguf",
                downloadUrl = "https://huggingface.co/sylvester-francis/rust-mentor-4b-GGUF/resolve/main/qwen3-4b.Q4_K_M.gguf",
                expectedSizeBytes = 2_497_280_832L,
                description = "Faster responses, lower memory. Good for most devices.",
                ramRequired = "~4 GB RAM"
            ),
            ModelInfo(
                id = "qwen3-8b",
                displayName = "Qwen3 8B",
                parameterSize = "8B",
                filename = "qwen3-8b.Q4_K_M.gguf",
                downloadUrl = "https://huggingface.co/sylvester-francis/rust-mentor-8b-GGUF/resolve/main/qwen3-8b.Q4_K_M.gguf",
                expectedSizeBytes = 5_030_000_000L,
                description = "More capable, deeper explanations. Needs 8+ GB RAM.",
                ramRequired = "~8 GB RAM"
            )
        )

        fun getModelById(id: String): ModelInfo? = AVAILABLE_MODELS.find { it.id == id }
    }

    private val modelsDir: File
        get() = File(context.filesDir, MODEL_DIR).also { it.mkdirs() }

    fun getModelFile(modelInfo: ModelInfo): File = File(modelsDir, modelInfo.filename)

    private fun getTempFile(modelInfo: ModelInfo): File = File(modelsDir, "${modelInfo.filename}.tmp")

    // Legacy accessors for backward compatibility
    val modelFile: File
        get() = File(modelsDir, AVAILABLE_MODELS[0].filename)

    fun isModelDownloaded(modelInfo: ModelInfo): Boolean {
        val file = getModelFile(modelInfo)
        return file.exists() && file.length() > 1_000_000
    }

    fun isModelDownloaded(): Boolean = isModelDownloaded(AVAILABLE_MODELS[0])

    fun getModelSizeMB(modelInfo: ModelInfo): Long {
        val file = getModelFile(modelInfo)
        return if (file.exists()) file.length() / (1024 * 1024) else 0
    }

    fun getModelSizeMB(): Long = getModelSizeMB(AVAILABLE_MODELS[0])

    fun getDownloadedModels(): List<ModelInfo> {
        return AVAILABLE_MODELS.filter { isModelDownloaded(it) }
    }

    fun deleteModel(modelInfo: ModelInfo): Boolean {
        getTempFile(modelInfo).delete()
        return getModelFile(modelInfo).delete()
    }

    fun deleteModel(): Boolean = deleteModel(AVAILABLE_MODELS[0])

    fun downloadModel(modelInfo: ModelInfo): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0f, 0, 0))

        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()

            val tempFile = getTempFile(modelInfo)
            val finalFile = getModelFile(modelInfo)

            var existingBytes = 0L
            if (tempFile.exists()) {
                existingBytes = tempFile.length()
            }

            val requestBuilder = Request.Builder().url(modelInfo.downloadUrl)
            if (existingBytes > 0) {
                requestBuilder.addHeader("Range", "bytes=$existingBytes-")
            }

            val response = client.newCall(requestBuilder.build()).execute()

            if (!response.isSuccessful && response.code != 206) {
                emit(DownloadState.Error("Download failed: HTTP ${response.code}"))
                return@flow
            }

            val body = response.body ?: run {
                emit(DownloadState.Error("Empty response body"))
                return@flow
            }

            val isResuming = response.code == 206 && existingBytes > 0
            if (!isResuming) {
                existingBytes = 0
                tempFile.delete()
            }

            val contentLength = body.contentLength().let {
                if (it > 0) it + existingBytes else modelInfo.expectedSizeBytes
            }
            val totalMB = contentLength / (1024 * 1024)

            body.byteStream().use { input ->
                FileOutputStream(tempFile, isResuming).use { output ->
                    val buffer = ByteArray(32768)
                    var bytesRead = existingBytes
                    var lastEmitTime = 0L

                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        bytesRead += read

                        val now = System.currentTimeMillis()
                        if (now - lastEmitTime > 500) {
                            val progress = bytesRead.toFloat() / contentLength
                            val downloadedMB = bytesRead / (1024 * 1024)
                            emit(DownloadState.Downloading(progress, downloadedMB, totalMB))
                            lastEmitTime = now
                        }
                    }
                }
            }

            if (tempFile.length() < 1_000_000) {
                emit(DownloadState.Error("Download appears incomplete (${tempFile.length()} bytes)"))
                return@flow
            }

            tempFile.renameTo(finalFile)
            emit(DownloadState.Completed)

        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Unknown download error"))
        }
    }.flowOn(Dispatchers.IO)

    // Legacy: download default model
    fun downloadModel(): Flow<DownloadState> = downloadModel(AVAILABLE_MODELS[0])
}
