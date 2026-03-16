package com.sylvester.rustsensei.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.sylvester.rustsensei.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.llm.ModelManager
import com.sylvester.rustsensei.ui.theme.CrispWhite
import com.sylvester.rustsensei.ui.theme.DarkSurfaceContainerHigh
import com.sylvester.rustsensei.ui.theme.ErrorNeon
import com.sylvester.rustsensei.ui.theme.NeonCyan
import com.sylvester.rustsensei.ui.theme.RustOrange
import com.sylvester.rustsensei.ui.theme.SecondaryText
import com.sylvester.rustsensei.ui.theme.SuccessGreen
import com.sylvester.rustsensei.viewmodel.ChatViewModel
import com.sylvester.rustsensei.viewmodel.ModelState
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
        ) {
            // --- Model Management Section ---
            SectionHeader("Model Management")
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Current active model
                    val activeModel = modelState.loadedModelId?.let { ModelManager.getModelById(it) }
                    if (activeModel != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Active Model",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = RustOrange
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = activeModel.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CrispWhite
                                )
                                Text(
                                    text = "${activeModel.parameterSize} parameters",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText
                                )
                            }
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = SuccessGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Size: ${modelViewModel.modelManager.getModelSizeMB(activeModel)} MB",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = SecondaryText
                        )
                        Text(
                            text = "Q8 | LiteRT (TFLite) | GPU-accelerated",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = SecondaryText.copy(alpha = 0.7f)
                        )
                    } else {
                        Text(
                            text = "No model loaded",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = SecondaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Download and load a model to start chatting",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Available models list
            ModelManager.AVAILABLE_MODELS.forEach { model ->
                val isLoaded = modelState.loadedModelId == model.id
                val isDownloaded = model.id in modelState.downloadedModelIds
                val isDownloading = modelState.selectedModelId == model.id &&
                        modelState.modelState == ModelState.DOWNLOADING
                val isLoading = modelState.selectedModelId == model.id &&
                        modelState.modelState == ModelState.LOADING

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(enabled = !isDownloading && !isLoading) {
                            if (isDownloaded && !isLoaded) {
                                modelViewModel.switchModel(model.id, chatViewModel.liteRtEngine)
                            } else if (!isDownloaded) {
                                modelViewModel.selectModel(model.id)
                                modelViewModel.startDownload()
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = model.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = CrispWhite
                                )
                                if (isLoaded) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Active",
                                        fontSize = 11.sp,
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            val sizeMb = model.expectedSizeBytes / (1024 * 1024)
                            Text(
                                text = "${model.ramRequired} \u00B7 ${sizeMb} MB",
                                fontSize = 12.sp,
                                color = SecondaryText
                            )

                            // Download progress bar
                            if (isDownloading) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { modelState.downloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = RustOrange,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${modelState.downloadedMB} / ${modelState.totalMB} MB \u00B7 ${"%.1f".format(modelState.downloadSpeedMBps)} MB/s",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = RustOrange.copy(alpha = 0.7f)
                                )
                            }

                            if (isLoading) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Initializing...",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = RustOrange.copy(alpha = 0.7f)
                                )
                            }
                        }

                        if (isDownloading) {
                            Text(
                                text = "${(modelState.downloadProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = RustOrange
                            )
                        } else if (isDownloaded) {
                            if (isLoaded) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = "Tap to load",
                                    fontSize = 11.sp,
                                    color = NeonCyan
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    modelViewModel.selectModel(model.id)
                                    modelViewModel.startDownload()
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, RustOrange.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = RustOrange
                                )
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- Inference Config Section ---
            SectionHeader("Inference Config")
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                        label = "Top P",
                        value = config.topP,
                        valueRange = 0.1f..1.0f,
                        valueLabel = String.format("%.2f", config.topP),
                        onValueChange = {
                            chatViewModel.updateConfig(config.copy(topP = it))
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
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SecondaryText
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            chatViewModel.updateConfig(
                                config.copy(
                                    temperature = 0.7f,
                                    topP = 0.9f,
                                    maxTokens = 384
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SecondaryText.copy(alpha = 0.3f))
                    ) {
                        Text(
                            "Reset to Defaults",
                            style = MaterialTheme.typography.labelLarge,
                            color = SecondaryText
                        )
                    }
                }
            }

            // --- Danger Zone Section ---
            SectionHeader("Danger Zone")
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurfaceContainerHigh
                ),
                border = BorderStroke(1.dp, ErrorNeon.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ErrorNeon.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Destructive actions",
                            style = MaterialTheme.typography.labelMedium,
                            color = ErrorNeon.copy(alpha = 0.7f)
                        )
                    }

                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ErrorNeon.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ErrorNeon
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Conversations")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showDeleteModelDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ErrorNeon.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ErrorNeon
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Active Model")
                    }
                }
            }

            // --- About Section ---
            SectionHeader("About")
            Spacer(modifier = Modifier.height(12.dp))

            val aboutContext = LocalContext.current

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RustSensei v1.0",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CrispWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your offline Rust programming tutor powered by on-device AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Content version: ${chatViewModel.getContentVersion()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Creator card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Created by",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = RustOrange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sylvester Ranjith Francis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CrispWhite
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    data class SocialLink(val label: String, val url: String, val iconRes: Int)

                    val links = listOf(
                        SocialLink("GitHub", "https://github.com/sylvester-francis", R.drawable.ic_github),
                        SocialLink("Hugging Face", "https://huggingface.co/sylvester-francis", R.drawable.ic_huggingface),
                        SocialLink("LinkedIn", "https://www.linkedin.com/in/sylvesterranjith/", R.drawable.ic_linkedin),
                        SocialLink("Substack", "https://techwithsyl.substack.com/", R.drawable.ic_substack),
                        SocialLink("Medium", "https://medium.com/@sylvesterranjithfrancis", R.drawable.ic_medium),
                        SocialLink("Instagram", "https://www.instagram.com/techwithsyl", R.drawable.ic_instagram)
                    )

                    links.forEach { link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                    aboutContext.startActivity(intent)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = link.iconRes),
                                contentDescription = link.label,
                                modifier = Modifier.size(20.dp),
                                tint = NeonCyan
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = link.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = NeonCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
    }

    // Confirm dialog: Clear All Conversations
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    "Clear All Conversations?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("This will permanently delete all your chat history. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    chatViewModel.clearAllConversations()
                    showClearDialog = false
                }) {
                    Text("Clear", color = ErrorNeon, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Confirm dialog: Delete Active Model
    if (showDeleteModelDialog) {
        val activeModel = modelState.loadedModelId?.let { ModelManager.getModelById(it) }
        AlertDialog(
            onDismissRequest = { showDeleteModelDialog = false },
            title = {
                Text(
                    "Delete ${activeModel?.displayName ?: "Model"}?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("This will delete the downloaded model. You will need to re-download it to use RustSensei.")
            },
            confirmButton = {
                TextButton(onClick = {
                    modelViewModel.deleteModel()
                    showDeleteModelDialog = false
                }) {
                    Text("Delete", color = ErrorNeon, fontWeight = FontWeight.Bold)
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
        style = MaterialTheme.typography.labelLarge,
        color = RustOrange,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 28.dp, bottom = 4.dp)
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
                fontSize = 14.sp,
                color = CrispWhite
            )
            Text(
                text = valueLabel,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = RustOrange
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = RustOrange,
                activeTrackColor = RustOrange,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
