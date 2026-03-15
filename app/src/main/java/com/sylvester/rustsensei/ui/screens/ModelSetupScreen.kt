package com.sylvester.rustsensei.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.llm.LlamaEngine
import com.sylvester.rustsensei.llm.ModelInfo
import com.sylvester.rustsensei.llm.ModelManager
import com.sylvester.rustsensei.ui.components.MemoryWarningDialog
import com.sylvester.rustsensei.ui.components.isMemoryLow
import com.sylvester.rustsensei.viewmodel.ModelState
import com.sylvester.rustsensei.viewmodel.ModelViewModel

@Composable
fun ModelSetupScreen(
    modelViewModel: ModelViewModel,
    llamaEngine: LlamaEngine,
    onNavigateToChat: () -> Unit
) {
    val uiState by modelViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showMemoryWarning by remember { mutableStateOf(false) }

    if (showMemoryWarning) {
        val selectedModel = modelViewModel.getSelectedModelInfo()
        MemoryWarningDialog(
            model = selectedModel,
            onProceed = {
                showMemoryWarning = false
                modelViewModel.startDownload()
            },
            onDismiss = {
                showMemoryWarning = false
            }
        )
    }

    LaunchedEffect(Unit) {
        modelViewModel.navigateToChat.collect {
            onNavigateToChat()
        }
    }

    LaunchedEffect(uiState.modelState) {
        if (uiState.modelState == ModelState.DOWNLOADED) {
            modelViewModel.loadModel(llamaEngine)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        // Logo area
        Text(text = "\uD83E\uDD80", fontSize = 72.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "RustSensei",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Your Offline Rust Tutor",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        when (uiState.modelState) {
            ModelState.NOT_DOWNLOADED -> {
                Text(
                    text = "Choose a model to download.\nAfter downloading, no internet connection is required.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Model selection rows
                ModelManager.AVAILABLE_MODELS.forEach { model ->
                    val isSelected = uiState.selectedModelId == model.id
                    val isDownloaded = model.id in uiState.downloadedModelIds

                    ModelCard(
                        model = model,
                        isSelected = isSelected,
                        isDownloaded = isDownloaded,
                        onSelect = { modelViewModel.selectModel(model.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Download button
                Button(
                    onClick = {
                        val selectedModel = modelViewModel.getSelectedModelInfo()
                        if (isMemoryLow(context, selectedModel)) {
                            showMemoryWarning = true
                        } else {
                            modelViewModel.startDownload()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Download ${modelViewModel.getSelectedModelInfo().displayName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            ModelState.DOWNLOADING -> {
                val info = modelViewModel.getSelectedModelInfo()

                Text(
                    text = "Downloading ${info.displayName}...",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { uiState.downloadProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        strokeCap = StrokeCap.Round,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${(uiState.downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${uiState.downloadedMB} MB / ${uiState.totalMB} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.downloadSpeedMBps > 0f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "%.1f MB/s".format(uiState.downloadSpeedMBps),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        if (uiState.estimatedSecondsLeft > 0) {
                            val minutes = uiState.estimatedSecondsLeft / 60
                            val seconds = uiState.estimatedSecondsLeft % 60
                            val etaText = if (minutes > 0) "${minutes}m ${seconds}s left"
                                else "${seconds}s left"
                            Text(
                                text = etaText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            ModelState.DOWNLOADED, ModelState.LOADING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading model...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ModelState.READY -> {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${modelViewModel.getSelectedModelInfo().displayName} is ready!",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onNavigateToChat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        "Start Learning",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            ModelState.ERROR -> {
                Text(
                    text = uiState.errorMessage ?: "An unknown error occurred",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (modelViewModel.isModelDownloaded()) {
                            modelViewModel.loadModel(llamaEngine)
                        } else {
                            modelViewModel.startDownload()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        if (modelViewModel.isModelDownloaded()) "Retry Load"
                        else "Retry Download",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ModelCard(
    model: ModelInfo,
    isSelected: Boolean,
    isDownloaded: Boolean,
    onSelect: () -> Unit
) {
    OutlinedCard(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 0.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isDownloaded) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Downloaded",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${model.expectedSizeBytes / (1024 * 1024)} MB  \u2022  ${model.ramRequired}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
