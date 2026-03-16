package com.sylvester.rustsensei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.data.LearningPath
import com.sylvester.rustsensei.data.PathStep
import com.sylvester.rustsensei.viewmodel.LearningPathViewModel
import com.sylvester.rustsensei.viewmodel.PathMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningPathScreen(
    viewModel: LearningPathViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler {
        if (uiState.mode == PathMode.DETAIL) {
            viewModel.navigateBack()
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (uiState.mode == PathMode.DETAIL)
                                uiState.selectedPath?.title ?: "Learning Path"
                            else
                                "Learning Paths",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (uiState.mode == PathMode.DETAIL) {
                                viewModel.navigateBack()
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                // Neon bottom border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                )
            }
        }
    ) { innerPadding ->
        when (uiState.mode) {
            PathMode.LIST -> PathListContent(
                paths = uiState.paths,
                stepProgress = uiState.stepProgress,
                onSelectPath = { viewModel.selectPath(it.id) },
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel
            )
            PathMode.DETAIL -> {
                val path = uiState.selectedPath
                if (path != null) {
                    PathDetailContent(
                        path = path,
                        stepProgress = uiState.stepProgress,
                        onStepTap = { step ->
                            viewModel.markStepComplete(path.id, step.id)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun PathListContent(
    paths: List<LearningPath>,
    stepProgress: Map<String, Boolean>,
    onSelectPath: (LearningPath) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LearningPathViewModel
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Choose a guided path to structure your learning journey.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(paths.size) { index ->
            val path = paths[index]
            val completionPercent = viewModel.getPathCompletionPercent(path)
            PathCard(
                path = path,
                completionPercent = completionPercent,
                onClick = { onSelectPath(path) }
            )
        }
    }
}

@Composable
private fun PathCard(
    path: LearningPath,
    completionPercent: Float,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = path.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = path.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Meta info row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${path.estimatedDays} days",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${path.steps.size} steps",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${(completionPercent * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { completionPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Butt
            )
        }
    }
}

@Composable
private fun PathDetailContent(
    path: LearningPath,
    stepProgress: Map<String, Boolean>,
    onStepTap: (PathStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val completedCount = path.steps.count { step ->
        stepProgress["${path.id}:${step.id}"] == true
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Path description header
        item {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    text = path.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$completedCount of ${path.steps.size} steps completed",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = {
                        if (path.steps.isEmpty()) 0f
                        else completedCount.toFloat() / path.steps.size
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Butt
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Neon separator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                )
            }
        }

        // Timeline steps
        itemsIndexed(path.steps) { index, step ->
            val isCompleted = stepProgress["${path.id}:${step.id}"] == true
            // Find the first uncompleted step as the "current" step
            val isCurrentStep = !isCompleted && path.steps.take(index).all { prev ->
                stepProgress["${path.id}:${prev.id}"] == true
            }
            val isLastStep = index == path.steps.size - 1

            TimelineStepItem(
                step = step,
                isCompleted = isCompleted,
                isCurrentStep = isCurrentStep,
                isLastStep = isLastStep,
                onTap = { onStepTap(step) }
            )
        }
    }
}

@Composable
private fun TimelineStepItem(
    step: PathStep,
    isCompleted: Boolean,
    isCurrentStep: Boolean,
    isLastStep: Boolean,
    onTap: () -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val completedColor = MaterialTheme.colorScheme.tertiary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

    val lineColor = when {
        isCompleted -> completedColor.copy(alpha = 0.5f)
        else -> inactiveColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(vertical = 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline column: icon + line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // Step indicator icon
            when {
                isCompleted -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        modifier = Modifier.size(24.dp),
                        tint = completedColor
                    )
                }
                isCurrentStep -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                accentColor.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(accentColor, CircleShape)
                        )
                    }
                }
                else -> {
                    Icon(
                        Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Not started",
                        modifier = Modifier.size(24.dp),
                        tint = inactiveColor
                    )
                }
            }

            // Connecting line (unless last step)
            if (!isLastStep) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(52.dp)
                        .offset(y = (-2).dp)
                        .background(lineColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Step content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLastStep) 0.dp else 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step type icon
                val typeIcon = stepTypeIcon(step.type)
                Icon(
                    typeIcon,
                    contentDescription = step.type,
                    modifier = Modifier.size(14.dp),
                    tint = when {
                        isCompleted -> completedColor
                        isCurrentStep -> accentColor
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = step.type.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = when {
                        isCompleted -> completedColor
                        isCurrentStep -> accentColor
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = step.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isCurrentStep) FontWeight.Bold else FontWeight.SemiBold,
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    isCurrentStep -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = step.description,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    isCurrentStep -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                }
            )
        }
    }
}

private fun stepTypeIcon(type: String): ImageVector {
    return when (type) {
        "read" -> Icons.AutoMirrored.Filled.MenuBook
        "exercise" -> Icons.Default.Code
        "review" -> Icons.Default.School
        else -> Icons.Default.PlayCircleFilled
    }
}
