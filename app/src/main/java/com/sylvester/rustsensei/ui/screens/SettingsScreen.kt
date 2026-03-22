package com.sylvester.rustsensei.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.sylvester.rustsensei.MainActivity
import com.sylvester.rustsensei.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.data.ThemePreference
import com.sylvester.rustsensei.llm.ModelManager
import com.sylvester.rustsensei.ui.theme.Alpha
import com.sylvester.rustsensei.ui.theme.CrispWhite
import com.sylvester.rustsensei.ui.theme.Dimens
import com.sylvester.rustsensei.ui.theme.DarkSurfaceContainerHigh
import com.sylvester.rustsensei.ui.theme.AppColors
import com.sylvester.rustsensei.ui.theme.SecondaryText
import com.sylvester.rustsensei.ui.theme.Spacing
import com.sylvester.rustsensei.viewmodel.ChatViewModel
import com.sylvester.rustsensei.viewmodel.ModelState
import com.sylvester.rustsensei.viewmodel.ModelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    chatViewModel: ChatViewModel,
    modelViewModel: ModelViewModel,
    onNavigateBack: () -> Unit,
    remindersEnabled: Boolean = false,
    onRemindersToggled: (Boolean) -> Unit = {}
) {
    val config by chatViewModel.config.collectAsState()
    val modelState by modelViewModel.uiState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteModelDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding)
        ) {
            // ── Screen Title ──
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = Spacing.XS)
            )
            Text(
                text = "Model, inference & account",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                modifier = Modifier.padding(bottom = Spacing.SM)
            )

            // --- Appearance Section ---
            SectionHeader(stringResource(R.string.appearance))
            Spacer(modifier = Modifier.height(Spacing.MD))

            val activity = LocalContext.current as? MainActivity
            val currentTheme by (activity?.themePreference
                ?: kotlinx.coroutines.flow.MutableStateFlow(ThemePreference.SYSTEM))
                .collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                    Text(
                        text = stringResource(R.string.theme),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = AppColors.current.accent
                    )
                    Spacer(modifier = Modifier.height(Spacing.MD))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.SM)
                    ) {
                        ThemePreference.entries.forEach { pref ->
                            val isSelected = pref == currentTheme
                            val label = when (pref) {
                                ThemePreference.SYSTEM -> stringResource(R.string.theme_system)
                                ThemePreference.DARK -> stringResource(R.string.theme_dark)
                                ThemePreference.LIGHT -> stringResource(R.string.theme_light)
                            }
                            Surface(
                                onClick = { activity?.updateThemePreference(pref) },
                                shape = RoundedCornerShape(Spacing.SM),
                                color = if (isSelected) AppColors.current.accent.copy(alpha = Alpha.BORDER)
                                        else Color.Transparent,
                                border = BorderStroke(
                                    Dimens.Divider,
                                    if (isSelected) AppColors.current.accent.copy(alpha = Alpha.MUTED)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = Alpha.BORDER)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) AppColors.current.accent
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = Spacing.MD, horizontal = Spacing.SM),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // --- Study Reminders Section ---
            SectionHeader(stringResource(R.string.study_reminders))
            Spacer(modifier = Modifier.height(Spacing.MD))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.CardPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.study_reminders),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.XS))
                        Text(
                            text = stringResource(R.string.study_reminders_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = onRemindersToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AppColors.current.accent,
                            checkedTrackColor = AppColors.current.accent.copy(alpha = Alpha.MUTED)
                        )
                    )
                }
            }

            // --- Model Management Section ---
            SectionHeader("Model Management")
            Spacer(modifier = Modifier.height(Spacing.MD))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(Dimens.CardPadding)) {
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
                                    color = AppColors.current.accent
                                )
                                Spacer(modifier = Modifier.height(Spacing.XS))
                                Text(
                                    text = activeModel.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
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
                                tint = AppColors.current.success,
                                modifier = Modifier.size(Dimens.IconMD)
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.SM))
                        Text(
                            text = "Size: ${modelViewModel.getModelSizeMB(activeModel)} MB",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = SecondaryText
                        )
                        val backend = modelViewModel.getActiveBackend()
                        val backendLabel = if (backend == "GPU") "GPU-accelerated" else "CPU mode (slower)"
                        Text(
                            text = "Q8 | LiteRT | $backendLabel",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (backend == "GPU") SecondaryText.copy(alpha = Alpha.SOFT)
                                    else AppColors.current.amber.copy(alpha = Alpha.SOFT)
                        )
                    } else {
                        Text(
                            text = "No model loaded",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = SecondaryText
                        )
                        Spacer(modifier = Modifier.height(Spacing.XS))
                        Text(
                            text = "Download and load a model to start chatting",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText.copy(alpha = Alpha.SOFT)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.MD))

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
                        .padding(vertical = Spacing.XS)
                        .clickable(enabled = !isDownloading && !isLoading) {
                            if (isDownloaded && !isLoaded) {
                                modelViewModel.switchModel(model.id)
                            } else if (!isDownloaded) {
                                modelViewModel.selectModel(model.id)
                                modelViewModel.startDownload()
                            }
                        },
                    shape = RoundedCornerShape(Dimens.CardRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isLoaded) {
                                    Spacer(modifier = Modifier.width(Spacing.SM))
                                    Text(
                                        text = "Active",
                                        fontSize = 11.sp,
                                        color = AppColors.current.success,
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
                                Spacer(modifier = Modifier.height(Spacing.SM))
                                LinearProgressIndicator(
                                    progress = { modelState.downloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(Spacing.XS)
                                        .clip(RoundedCornerShape(Spacing.XXS)),
                                    color = AppColors.current.accent,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.height(Spacing.XS))
                                Text(
                                    text = "${modelState.downloadedMB} / ${modelState.totalMB} MB \u00B7 ${"%.1f".format(modelState.downloadSpeedMBps)} MB/s",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = AppColors.current.accent.copy(alpha = Alpha.SOFT)
                                )
                            }

                            if (isLoading) {
                                Spacer(modifier = Modifier.height(Spacing.XS))
                                Text(
                                    text = "Initializing...",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = AppColors.current.accent.copy(alpha = Alpha.SOFT)
                                )
                            }
                        }

                        if (isDownloading) {
                            Text(
                                text = "${(modelState.downloadProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.current.accent
                            )
                        } else if (isDownloaded) {
                            if (isLoaded) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = AppColors.current.success,
                                    modifier = Modifier.size(Dimens.IconSM)
                                )
                            } else {
                                Text(
                                    text = "Tap to load",
                                    fontSize = 11.sp,
                                    color = AppColors.current.cyan
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    modelViewModel.selectModel(model.id)
                                    modelViewModel.startDownload()
                                },
                                shape = RoundedCornerShape(Spacing.SM),
                                border = BorderStroke(Dimens.Divider, AppColors.current.accent.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AppColors.current.accent
                                )
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(Spacing.LG)
                                )
                                Spacer(modifier = Modifier.width(Spacing.XS))
                                Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- Inference Config Section ---
            SectionHeader("Inference Config")
            Spacer(modifier = Modifier.height(Spacing.MD))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(Dimens.CardPadding)) {
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

                    Spacer(modifier = Modifier.height(Spacing.SM))

                    Text(
                        text = "Context Length: ${config.contextLength} tokens",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SecondaryText
                    )

                    Spacer(modifier = Modifier.height(Spacing.LG))

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
                        shape = RoundedCornerShape(Spacing.SM),
                        border = BorderStroke(Dimens.Divider, SecondaryText.copy(alpha = Alpha.MUTED))
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
            Spacer(modifier = Modifier.height(Spacing.MD))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = BorderStroke(Dimens.Divider, AppColors.current.error.copy(alpha = Alpha.BORDER))
            ) {
                Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = Spacing.MD)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(Spacing.LG),
                            tint = AppColors.current.error.copy(alpha = Alpha.SOFT)
                        )
                        Spacer(modifier = Modifier.width(Spacing.SM))
                        Text(
                            text = "Destructive actions",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.current.error.copy(alpha = Alpha.SOFT)
                        )
                    }

                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Spacing.SM),
                        border = BorderStroke(Dimens.Divider, AppColors.current.error.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppColors.current.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(Spacing.LG)
                        )
                        Spacer(modifier = Modifier.width(Spacing.SM))
                        Text("Clear All Conversations")
                    }

                    Spacer(modifier = Modifier.height(Spacing.SM))

                    OutlinedButton(
                        onClick = { showDeleteModelDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Spacing.SM),
                        border = BorderStroke(Dimens.Divider, AppColors.current.error.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppColors.current.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(Spacing.LG)
                        )
                        Spacer(modifier = Modifier.width(Spacing.SM))
                        Text("Delete Active Model")
                    }
                }
            }

            // --- About Section ---
            SectionHeader("About")
            Spacer(modifier = Modifier.height(Spacing.MD))

            val aboutContext = LocalContext.current

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                    Text(
                        text = "RustSensei v${com.sylvester.rustsensei.BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CrispWhite
                    )
                    Spacer(modifier = Modifier.height(Spacing.XS))
                    Text(
                        text = "Your offline Rust programming tutor powered by on-device AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(Spacing.MD))
                    Text(
                        text = "Content version: ${chatViewModel.getContentVersion()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.MD))

            // Creator card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                    Text(
                        text = "Created by",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = AppColors.current.accent
                    )
                    Spacer(modifier = Modifier.height(Spacing.SM))
                    Text(
                        text = "Sylvester Ranjith Francis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CrispWhite
                    )
                    Spacer(modifier = Modifier.height(Spacing.MD))

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
                                .padding(vertical = Spacing.SM),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = link.iconRes),
                                contentDescription = link.label,
                                modifier = Modifier.size(Dimens.IconSM),
                                tint = AppColors.current.cyan
                            )
                            Spacer(modifier = Modifier.width(Spacing.MD))
                            Text(
                                text = link.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.current.cyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.MD))

            // Acknowledgments card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(Dimens.CardPadding)) {
                    Text(
                        text = "Acknowledgments",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = AppColors.current.accent
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Book content adapted from The Rust Programming Language by Steve Klabnik and Carol Nichols, licensed under MIT/Apache 2.0.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(Spacing.XS))
                    Text(
                        text = "github.com/rust-lang/book",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = AppColors.current.cyan,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rust-lang/book"))
                            aboutContext.startActivity(intent)
                        }
                    )
                    Spacer(modifier = Modifier.height(Spacing.MD))
                    Text(
                        text = "Exercises inspired by Rustlings by the Rust Community, licensed under MIT.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(Spacing.XS))
                    Text(
                        text = "github.com/rust-lang/rustlings",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = AppColors.current.cyan,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rust-lang/rustlings"))
                            aboutContext.startActivity(intent)
                        }
                    )
                    Spacer(modifier = Modifier.height(Spacing.MD))
                    Text(
                        text = "AI model powered by Google LiteRT, running entirely on-device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.XXXL))
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
                    Text("Clear", color = AppColors.current.error, fontWeight = FontWeight.Bold)
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
                    Text("Delete", color = AppColors.current.error, fontWeight = FontWeight.Bold)
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
        color = AppColors.current.accent,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 28.dp, bottom = Spacing.XS)
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
    Column(modifier = Modifier.padding(vertical = Spacing.SM)) {
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
                color = AppColors.current.accent
            )
        }
        Spacer(modifier = Modifier.height(Spacing.XS))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = AppColors.current.accent,
                activeTrackColor = AppColors.current.accent,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
