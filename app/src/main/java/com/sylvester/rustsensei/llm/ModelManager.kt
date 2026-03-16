package com.sylvester.rustsensei.llm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(
        val progress: Float,
        val downloadedMB: Long,
        val totalMB: Long,
        val speedMBps: Float = 0f,
        val estimatedSecondsLeft: Long = 0
    ) : DownloadState()
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
    val ramRequired: String,
    val minDeviceMemoryGb: Float = 0f,
    val sha256: String = ""
)

class ModelManager(private val context: Context) {

    companion object {
        private const val MODEL_DIR = "models"

        val AVAILABLE_MODELS = listOf(
            ModelInfo(
                id = "litert-0.6b",
                displayName = "Rust Mentor 0.6B",
                parameterSize = "0.6B",
                filename = "rust_mentor_0_6b_q8_ekv2048.litertlm",
                downloadUrl = "https://huggingface.co/sylvester-francis/rust-mentor-0.6b-LiteRT/resolve/main/rust_mentor_0_6b_q8_ekv2048.litertlm",
                expectedSizeBytes = 665_000_000L,
                description = "Fastest responses. Good for quick questions.",
                ramRequired = "~2 GB RAM",
                minDeviceMemoryGb = 2f
            ),
            ModelInfo(
                id = "litert-1b-gemma",
                displayName = "Rust Mentor 1B",
                parameterSize = "1B",
                filename = "rust-mentor-1b-mobile_q8_ekv2053.litertlm",
                downloadUrl = "https://huggingface.co/sylvester-francis/rust-mentor-1b-mobile-LiteRT/resolve/main/rust-mentor-1b-mobile_q8_ekv2053.litertlm",
                expectedSizeBytes = 1_200_000_000L,
                description = "Best balance of speed and quality. Recommended.",
                ramRequired = "~3 GB RAM",
                minDeviceMemoryGb = 3f
            ),
            ModelInfo(
                id = "litert-1.7b",
                displayName = "Rust Mentor 1.7B",
                parameterSize = "1.7B",
                filename = "rust_mentor_1_7b_q8_ekv2048.litertlm",
                downloadUrl = "https://huggingface.co/sylvester-francis/rust-mentor-1.7b-LiteRT/resolve/main/rust_mentor_1_7b_q8_ekv2048.litertlm",
                expectedSizeBytes = 1_800_000_000L,
                description = "Most capable. Best for detailed explanations.",
                ramRequired = "~4 GB RAM",
                minDeviceMemoryGb = 4f
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

    /** Bug 12: Check if a partial .tmp file exists from an interrupted download. */
    fun hasTempFile(modelInfo: ModelInfo): Boolean {
        val tempFile = getTempFile(modelInfo)
        return tempFile.exists() && tempFile.length() > 0
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

            val downloadStartTime = System.currentTimeMillis()

            body.byteStream().use { input ->
                FileOutputStream(tempFile, isResuming).use { output ->
                    val buffer = ByteArray(32768)
                    var bytesRead = existingBytes
                    val bytesAtStart = existingBytes
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

                            // Calculate speed and ETA
                            val elapsedMs = now - downloadStartTime
                            val bytesDownloadedThisSession = bytesRead - bytesAtStart
                            val speedBps = if (elapsedMs > 0) (bytesDownloadedThisSession * 1000.0 / elapsedMs) else 0.0
                            val speedMBps = (speedBps / (1024 * 1024)).toFloat()
                            val remainingBytes = contentLength - bytesRead
                            val estimatedSecondsLeft = if (speedBps > 0) (remainingBytes / speedBps).toLong() else 0

                            emit(DownloadState.Downloading(progress, downloadedMB, totalMB, speedMBps, estimatedSecondsLeft))
                            lastEmitTime = now
                        }
                    }
                }
            }

            if (tempFile.length() < 1_000_000) {
                emit(DownloadState.Error("Download appears incomplete (${tempFile.length()} bytes)"))
                return@flow
            }

            // Verify SHA256 integrity if checksum is provided
            if (modelInfo.sha256.isNotEmpty()) {
                val actualHash = sha256(tempFile)
                if (!actualHash.equals(modelInfo.sha256, ignoreCase = true)) {
                    tempFile.delete()
                    emit(DownloadState.Error(
                        "Integrity check failed. Expected SHA256: ${modelInfo.sha256.take(12)}... " +
                        "Got: ${actualHash.take(12)}... The file may be corrupted."
                    ))
                    return@flow
                }
                Log.i("ModelManager", "SHA256 verified: ${actualHash.take(16)}...")
            }

            tempFile.renameTo(finalFile)
            emit(DownloadState.Completed)

        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Unknown download error"))
        }
    }.flowOn(Dispatchers.IO)

    // Legacy: download default model
    fun downloadModel(): Flow<DownloadState> = downloadModel(AVAILABLE_MODELS[0])

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
