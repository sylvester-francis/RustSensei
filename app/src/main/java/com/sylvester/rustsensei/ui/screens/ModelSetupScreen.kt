package com.sylvester.rustsensei.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.llm.LiteRtEngine
import com.sylvester.rustsensei.llm.ModelInfo
import com.sylvester.rustsensei.llm.ModelManager
import com.sylvester.rustsensei.ui.components.MemoryWarningDialog
import com.sylvester.rustsensei.ui.components.isMemoryLow
import com.sylvester.rustsensei.viewmodel.ModelState
import com.sylvester.rustsensei.viewmodel.ModelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSetupScreen(
    modelViewModel: ModelViewModel,
    liteRtEngine: LiteRtEngine,
    onNavigateToChat: () -> Unit,
    onSkip: () -> Unit = {}
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
            modelViewModel.loadModel(liteRtEngine)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download AI Model") },
                navigationIcon = {
                    IconButton(onClick = onSkip) {
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
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Logo area — dramatic 80sp crab
        Text(text = "\uD83E\uDD80", fontSize = 80.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Subtitle — enable chat with your tutor
        Text(
            text = "Enable chat with your Rust tutor",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Thin neon horizontal line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.30f))
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (uiState.modelState) {
            ModelState.NOT_DOWNLOADED, ModelState.DOWNLOAD_INCOMPLETE -> {
                Text(
                    text = "Choose a model to download.\nAfter downloading, no internet connection is required.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Model selection cards — sharp 8dp corners, neon borders
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

                // Download button — sharp 8dp corners, primary fill, bold monospace text
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
                    shape = RoundedCornerShape(8.dp),
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
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
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
                    // Sharp progress bar — 6dp height, 3dp corners, primary color
                    LinearProgressIndicator(
                        progress = { uiState.downloadProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        strokeCap = StrokeCap.Butt,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${(uiState.downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )
                        if (uiState.estimatedSecondsLeft > 0) {
                            val minutes = uiState.estimatedSecondsLeft / 60
                            val seconds = uiState.estimatedSecondsLeft % 60
                            val etaText = if (minutes > 0) "${minutes}m ${seconds}s left"
                                else "${seconds}s left"
                            Text(
                                text = etaText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            ModelState.DOWNLOADED, ModelState.LOADING -> {
                // Pulsing horizontal neon line instead of CircularProgressIndicator
                NeonPulsingLine()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Initializing model...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
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
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Start Learning",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
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
                            modelViewModel.loadModel(liteRtEngine)
                        } else {
                            modelViewModel.startDownload()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (modelViewModel.isModelDownloaded()) "Retry Load"
                        else "Retry Download",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Skip for now — at the very bottom, very subtle
        Spacer(modifier = Modifier.height(48.dp))
        TextButton(onClick = onSkip) {
            Text(
                "Skip for now",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
    } // end Scaffold
}

/**
 * Horizontal pulsing neon line — replaces CircularProgressIndicator for loading state.
 */
@Composable
private fun NeonPulsingLine() {
    val transition = rememberInfiniteTransition(label = "neon-pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse-alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .height(3.dp)
            .clip(RoundedCornerShape(1.5.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
    )
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
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
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
                val sizeMb = model.expectedSizeBytes / (1024 * 1024)
                val estimatedMinutes = (sizeMb / 10).coerceAtLeast(1) // ~10 MB/s estimate
                Text(
                    text = "${sizeMb} MB  \u2022  ${model.ramRequired}  \u2022  ~${estimatedMinutes}min on Wi-Fi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
