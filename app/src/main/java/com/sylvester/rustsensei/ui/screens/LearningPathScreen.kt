package com.sylvester.rustsensei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.data.LearningPath
import com.sylvester.rustsensei.data.PathStep
import com.sylvester.rustsensei.ui.theme.Alpha
import com.sylvester.rustsensei.ui.theme.Dimens

import com.sylvester.rustsensei.ui.theme.AppColors
import com.sylvester.rustsensei.ui.theme.Spacing
import com.sylvester.rustsensei.viewmodel.LearningPathViewModel
import com.sylvester.rustsensei.viewmodel.PathMode

private val pathIcons = listOf(
    Icons.Default.Rocket,
    Icons.Default.Speed,
    Icons.Default.Star,
    Icons.Default.School,
    Icons.Default.Code,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningPathScreen(
    viewModel: LearningPathViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChapter: (chapterId: String) -> Unit = {},
    onNavigateToExercise: (exerciseId: String) -> Unit = {},
    onNavigateToReview: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler {
        if (uiState.mode == PathMode.DETAIL) viewModel.navigateBack()
        else onNavigateBack()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (uiState.mode == PathMode.DETAIL)
                                uiState.selectedPath?.title ?: "Learning Path"
                            else "Learning Paths",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (uiState.mode == PathMode.DETAIL) viewModel.navigateBack()
                            else onNavigateBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.Divider)
                        .background(AppColors.current.accent.copy(alpha = Alpha.BORDER))
                )
            }
        }
    ) { innerPadding ->
        when (uiState.mode) {
            PathMode.LIST -> PathListContent(
                paths = uiState.paths,
                viewModel = viewModel,
                onSelectPath = { viewModel.selectPath(it.id) },
                modifier = Modifier.padding(innerPadding)
            )
            PathMode.DETAIL -> {
                val path = uiState.selectedPath
                if (path != null) {
                    PathDetailContent(
                        path = path,
                        stepProgress = uiState.stepProgress,
                        onStepTap = { step ->
                            viewModel.setPendingStep(path.id, step)
                            when (step.type) {
                                "read" -> onNavigateToChapter(step.targetId)
                                "exercise" -> onNavigateToExercise(step.targetId)
                                "review" -> onNavigateToReview()
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// --- PATH LIST ---

@Composable
private fun PathListContent(
    paths: List<LearningPath>,
    viewModel: LearningPathViewModel,
    onSelectPath: (LearningPath) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.XL),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Pick a structured path to guide your Rust journey",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        items(paths.size) { index ->
            val path = paths[index]
            val percent = viewModel.getPathCompletionPercent(path)
            val accents = listOf(
                AppColors.current.accent,
                AppColors.current.cyan,
                AppColors.current.pathAccentOrange,
                AppColors.current.success,
                AppColors.current.pathAccentPurple
            )
            val accent = accents[index % accents.size]
            val icon = pathIcons[index % pathIcons.size]

            PathCard(
                path = path,
                completionPercent = percent,
                accent = accent,
                icon = icon,
                onClick = { onSelectPath(path) }
            )
        }

        item { Spacer(modifier = Modifier.height(Spacing.SM)) }
    }
}

@Composable
private fun PathCard(
    path: LearningPath,
    completionPercent: Float,
    accent: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val percentInt = (completionPercent * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
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
            // Progress ring with icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp)
            ) {
                // Track
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(56.dp),
                    strokeWidth = Spacing.XS,
                    color = accent.copy(alpha = 0.12f),
                    strokeCap = StrokeCap.Round
                )
                // Progress
                CircularProgressIndicator(
                    progress = { completionPercent },
                    modifier = Modifier.size(56.dp),
                    strokeWidth = Spacing.XS,
                    color = accent,
                    strokeCap = StrokeCap.Round
                )
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = accent
                )
            }

            Spacer(modifier = Modifier.width(Spacing.LG))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = path.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.XS))
                Text(
                    text = path.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(Spacing.SM))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.MD)
                ) {
                    Text(
                        text = "${path.steps.size} steps",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(Spacing.MD),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${path.estimatedDays}d",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$percentInt%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (percentInt == 100) AppColors.current.success
                        else if (percentInt > 0) accent
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// --- PATH DETAIL (Vertical Node Path - Duolingo style) ---

@Composable
private fun PathDetailContent(
    path: LearningPath,
    stepProgress: Map<String, Boolean>,
    onStepTap: (PathStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val completedCount = path.steps.count { stepProgress["${path.id}:${it.id}"] == true }
    val progress = if (path.steps.isEmpty()) 0f else completedCount.toFloat() / path.steps.size

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with description and progress
        item {
            Text(
                text = path.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Progress summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.CardRadius))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$completedCount of ${path.steps.size} steps",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.SM))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AppColors.current.accent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.LG))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AppColors.current.accent
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // Timeline steps (vertical node path)
        itemsIndexed(path.steps) { index, step ->
            val isCompleted = stepProgress["${path.id}:${step.id}"] == true
            val isCurrentStep = !isCompleted && path.steps.take(index).all {
                stepProgress["${path.id}:${it.id}"] == true
            }
            val isLocked = !isCompleted && !isCurrentStep
            val isLastStep = index == path.steps.size - 1

            NodeStepItem(
                step = step,
                stepNumber = index + 1,
                isCompleted = isCompleted,
                isCurrentStep = isCurrentStep,
                isLocked = isLocked,
                isLastStep = isLastStep,
                onTap = { onStepTap(step) }
            )
        }

        item { Spacer(modifier = Modifier.height(Spacing.LG)) }
    }
}

@Composable
private fun NodeStepItem(
    step: PathStep,
    stepNumber: Int,
    isCompleted: Boolean,
    isCurrentStep: Boolean,
    isLocked: Boolean,
    isLastStep: Boolean,
    onTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked, onClick = onTap),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline column (node + connection line)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(56.dp)
        ) {
            // Node circle
            when {
                isCompleted -> {
                    // Completed: 48dp circle, primary bg, white checkmark
                    Box(
                        modifier = Modifier
                            .size(Spacing.Section)
                            .background(AppColors.current.accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Completed",
                            modifier = Modifier.size(Dimens.IconMD),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                isCurrentStep -> {
                    // Current: primaryContainer bg, pulsing primary border, play icon
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(Spacing.Section)
                            .background(AppColors.current.accent.copy(alpha = Alpha.BORDER), CircleShape)
                            .border(
                                width = 3.dp,
                                color = AppColors.current.accent.copy(alpha = pulseAlpha),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Current",
                            modifier = Modifier.size(Dimens.IconMD),
                            tint = AppColors.current.accent
                        )
                    }
                }

                else -> {
                    // Locked: surfaceVariant bg, lock icon, 60% opacity
                    Box(
                        modifier = Modifier
                            .size(Spacing.Section)
                            .alpha(Alpha.SECONDARY)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.size(Dimens.IconSM),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.HINT)
                        )
                    }
                }
            }

            // Connection line
            if (!isLastStep) {
                val nextStep = step // We determine line style based on current node state
                val lineColor = when {
                    isCompleted -> AppColors.current.accent
                    isCurrentStep -> AppColors.current.accent.copy(alpha = Alpha.MUTED)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.BORDER)
                }
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(Spacing.Section)
                        .background(lineColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.MD))

        // Step content
        val contentAlpha = if (isLocked) Alpha.SECONDARY else 1f

        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(contentAlpha)
                .padding(bottom = if (isLastStep) 0.dp else Spacing.LG, top = Spacing.XS)
        ) {
            // Type label
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    stepTypeIcon(step.type),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = when {
                        isCompleted -> AppColors.current.accent
                        isCurrentStep -> AppColors.current.accent
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.HINT)
                    }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = step.type.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    color = when {
                        isCompleted -> AppColors.current.accent.copy(alpha = Alpha.SOFT)
                        isCurrentStep -> AppColors.current.accent
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.HINT)
                    }
                )
                if (isCurrentStep) {
                    Spacer(modifier = Modifier.width(Spacing.SM))
                    Text(
                        text = "UP NEXT",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = AppColors.current.cyan,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.XS))

            Text(
                text = step.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isCurrentStep) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = Alpha.HINT)
                    isCurrentStep -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = Alpha.SECONDARY)
                }
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = step.description,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    isCurrentStep -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                }
            )
        }
    }
}

private fun stepTypeIcon(type: String): ImageVector = when (type) {
    "read" -> Icons.AutoMirrored.Filled.MenuBook
    "exercise" -> Icons.Default.Code
    "review" -> Icons.Default.School
    else -> Icons.Default.PlayArrow
}
