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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.llm.LlamaEngine
import com.sylvester.rustsensei.llm.ModelInfo
import com.sylvester.rustsensei.llm.ModelManager
import com.sylvester.rustsensei.viewmodel.ModelState
import com.sylvester.rustsensei.viewmodel.ModelViewModel

@Composable
fun ModelSetupScreen(
    modelViewModel: ModelViewModel,
    llamaEngine: LlamaEngine,
    onNavigateToChat: () -> Unit
) {
    val uiState by modelViewModel.uiState.collectAsState()

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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "\uD83E\uDD80", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "RustSensei",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Your Offline Rust Tutor",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (uiState.modelState) {
            ModelState.NOT_DOWNLOADED -> {
                Text(
                    text = "Choose a model to download. After downloading, no internet connection is required.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Model selection cards
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

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { modelViewModel.startDownload() },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download ${modelViewModel.getSelectedModelInfo().displayName}")
                }
            }

            ModelState.DOWNLOADING -> {
                val info = modelViewModel.getSelectedModelInfo()
                Text(
                    text = "Downloading ${info.displayName}...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { uiState.downloadProgress },
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${uiState.downloadedMB} MB / ${uiState.totalMB} MB",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "${(uiState.downloadProgress * 100).toInt()}%",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            ModelState.DOWNLOADED, ModelState.LOADING -> {
                Text(
                    text = "Loading ${modelViewModel.getSelectedModelInfo().displayName} into memory...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This may take a few seconds",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            ModelState.READY -> {
                Text(
                    text = "${modelViewModel.getSelectedModelInfo().displayName} is ready!",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateToChat) {
                    Text("Start Learning")
                }
            }

            ModelState.ERROR -> {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage ?: "An unknown error occurred",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
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
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text(
                        if (modelViewModel.isModelDownloaded()) "Retry Load"
                        else "Retry Download"
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
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant

    OutlinedCard(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (model.parameterSize == "4B") Icons.Default.Speed
                else Icons.Default.Psychology,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
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
                Text(
                    text = model.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${model.expectedSizeBytes / (1024 * 1024)} MB download  \u2022  ${model.ramRequired}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
