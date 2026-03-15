package com.sylvester.rustsensei.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.llm.ModelManager
import com.sylvester.rustsensei.viewmodel.ChatViewModel
import com.sylvester.rustsensei.viewmodel.ModelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    chatViewModel: ChatViewModel,
    modelViewModel: ModelViewModel,
    onNavigateBack: () -> Unit
) {
    val config by chatViewModel.config.collectAsState()
    val modelState by modelViewModel.uiState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteModelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Model Selection
            SectionHeader("Model")
            Spacer(modifier = Modifier.height(8.dp))

            ModelManager.AVAILABLE_MODELS.forEach { model ->
                val isLoaded = modelState.loadedModelId == model.id
                val isDownloaded = model.id in modelState.downloadedModelIds
                val isSelected = modelState.selectedModelId == model.id

                OutlinedCard(
                    onClick = {
                        if (isDownloaded && !isLoaded) {
                            modelViewModel.switchModel(model.id, chatViewModel.llamaEngine)
                        } else if (!isDownloaded) {
                            modelViewModel.selectModel(model.id)
                            modelViewModel.startDownload()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    border = BorderStroke(
                        width = if (isLoaded) 2.dp else 1.dp,
                        color = if (isLoaded) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (model.parameterSize == "4B") Icons.Default.Speed
                            else Icons.Default.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = if (isLoaded) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = model.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                if (isLoaded) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Active",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = model.ramRequired,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isDownloaded) {
                            if (isLoaded) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = "Tap to switch",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Download",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Inference Settings
            SectionHeader("Inference Settings")

            SettingSlider(
                label = "Temperature",
                value = config.temperature,
                valueRange = 0.1f..1.5f,
                valueLabel = String.format("%.1f", config.temperature),
                onValueChange = {
                    chatViewModel.updateConfig(config.copy(temperature = it))
                }
            )

            SettingSlider(
                label = "Max Tokens",
                value = config.maxTokens.toFloat(),
                valueRange = 128f..2048f,
                valueLabel = config.maxTokens.toString(),
                onValueChange = {
                    chatViewModel.updateConfig(config.copy(maxTokens = it.toInt()))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Context Length: ${config.contextLength} tokens",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Model Info
            SectionHeader("Active Model Info")
            Spacer(modifier = Modifier.height(8.dp))

            val activeModel = modelState.loadedModelId?.let { ModelManager.getModelById(it) }
            if (activeModel != null) {
                Text(
                    text = "${activeModel.displayName} (${activeModel.parameterSize})",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Size on disk: ${modelViewModel.modelManager.getModelSizeMB(activeModel)} MB",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "No model loaded",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Quantization: Q4_K_M  |  Format: GGUF",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Data Management
            SectionHeader("Data Management")
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear All Conversations")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showDeleteModelDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete Active Model")
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Conversations?") },
            text = { Text("This will permanently delete all your chat history.") },
            confirmButton = {
                TextButton(onClick = {
                    chatViewModel.clearAllConversations()
                    showClearDialog = false
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteModelDialog) {
        val activeModel = modelState.loadedModelId?.let { ModelManager.getModelById(it) }
        AlertDialog(
            onDismissRequest = { showDeleteModelDialog = false },
            title = { Text("Delete ${activeModel?.displayName ?: "Model"}?") },
            text = {
                Text("This will delete the downloaded model. You will need to re-download it to use RustSensei.")
            },
            confirmButton = {
                TextButton(onClick = {
                    modelViewModel.deleteModel()
                    showDeleteModelDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteModelDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueLabel,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
