package com.sylvester.rustsensei.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.R
import com.sylvester.rustsensei.content.AllocationStatus
import com.sylvester.rustsensei.content.HeapAllocation
import com.sylvester.rustsensei.content.OwnershipScenario
import com.sylvester.rustsensei.content.StackVariable
import com.sylvester.rustsensei.content.VariableStatus
import com.sylvester.rustsensei.content.VisualizationStep
import com.sylvester.rustsensei.ui.theme.Alpha
import com.sylvester.rustsensei.ui.theme.Dimens
import com.sylvester.rustsensei.ui.theme.ErrorNeon
import com.sylvester.rustsensei.ui.theme.NeonCyan
import com.sylvester.rustsensei.ui.theme.RustOrange
import com.sylvester.rustsensei.ui.theme.Spacing
import com.sylvester.rustsensei.ui.theme.SuccessGreen
import com.sylvester.rustsensei.ui.theme.WarningAmber
import com.sylvester.rustsensei.viewmodel.VisualizerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizerScreen(
    viewModel: VisualizerViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.currentScenario != null)
                            uiState.currentScenario!!.title
                        else stringResource(R.string.visualizer_title),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.currentScenario != null) viewModel.goBack()
                        else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (uiState.currentScenario == null) {
            // Scenario list
            ScenarioListView(
                scenarios = uiState.scenarios,
                onSelectScenario = { viewModel.openScenario(it) },
                modifier = Modifier.padding(padding)
            )
        } else {
            // Step-by-step visualization
            StepVisualizationView(
                step = uiState.currentStep,
                stepIndex = uiState.currentStepIndex,
                totalSteps = uiState.totalSteps,
                hasNext = uiState.hasNext,
                hasPrevious = uiState.hasPrevious,
                onNext = { viewModel.nextStep() },
                onPrevious = { viewModel.previousStep() },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ScenarioListView(
    scenarios: List<OwnershipScenario>,
    onSelectScenario: (OwnershipScenario) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.SM)
    ) {
        item {
            Text(
                text = stringResource(R.string.visualizer_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.LG)
            )
        }
        items(scenarios) { scenario ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectScenario(scenario) },
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier.padding(Dimens.CardPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Memory,
                        contentDescription = null,
                        tint = RustOrange,
                        modifier = Modifier.size(Dimens.IconLG)
                    )
                    Spacer(modifier = Modifier.width(Spacing.MD))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = scenario.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.XXS))
                        Text(
                            text = scenario.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(Spacing.XS))
                        Text(
                            text = "${scenario.steps.size} steps",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.HINT),
                        modifier = Modifier.size(Dimens.IconSM)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepVisualizationView(
    step: VisualizationStep?,
    stepIndex: Int,
    totalSteps: Int,
    hasNext: Boolean,
    hasPrevious: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (step == null) return

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPadding)
            .verticalScroll(rememberScrollState())
    ) {
        // Step indicator
        Text(
            text = stringResource(R.string.visualizer_step, stepIndex + 1, totalSteps),
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = RustOrange,
            modifier = Modifier.padding(bottom = Spacing.SM)
        )

        // Code for this step
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CardRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Text(
                text = step.code,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = NeonCyan,
                lineHeight = 18.sp,
                modifier = Modifier.padding(Dimens.CardPadding)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.LG))

        // Memory visualization
        Text(
            text = stringResource(R.string.visualizer_memory),
            style = MaterialTheme.typography.labelLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = RustOrange,
            modifier = Modifier.padding(bottom = Spacing.SM)
        )

        // Stack section
        if (step.stackVariables.isNotEmpty()) {
            Text(
                text = stringResource(R.string.visualizer_stack),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.XS)
            )
            step.stackVariables.forEach { variable ->
                StackVariableCard(variable)
                Spacer(modifier = Modifier.height(Spacing.XS))
            }
        }

        Spacer(modifier = Modifier.height(Spacing.MD))

        // Heap section
        if (step.heapAllocations.isNotEmpty()) {
            Text(
                text = stringResource(R.string.visualizer_heap),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.XS)
            )
            step.heapAllocations.forEach { alloc ->
                HeapAllocationCard(alloc)
                Spacer(modifier = Modifier.height(Spacing.XS))
            }
        }

        Spacer(modifier = Modifier.height(Spacing.LG))

        // Annotation
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CardRadius),
            colors = CardDefaults.cardColors(
                containerColor = NeonCyan.copy(alpha = Alpha.BORDER)
            ),
            border = BorderStroke(Dimens.Divider, NeonCyan.copy(alpha = Alpha.MUTED))
        ) {
            Text(
                text = step.annotation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp,
                modifier = Modifier.padding(Dimens.CardPadding)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.XL))

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.SM)
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = hasPrevious,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(Dimens.CardRadius),
                border = BorderStroke(Dimens.Divider, if (hasPrevious) RustOrange.copy(alpha = Alpha.MUTED) else MaterialTheme.colorScheme.outline.copy(alpha = Alpha.BORDER))
            ) {
                Text(stringResource(R.string.visualizer_previous))
            }
            Button(
                onClick = onNext,
                enabled = hasNext,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = RustOrange),
                shape = RoundedCornerShape(Dimens.CardRadius)
            ) {
                Text(stringResource(R.string.visualizer_next))
            }
        }

        Spacer(modifier = Modifier.height(Spacing.XXXL))
    }
}

@Composable
private fun StackVariableCard(variable: StackVariable) {
    val statusColor by animateColorAsState(
        targetValue = when (variable.status) {
            VariableStatus.ACTIVE -> SuccessGreen
            VariableStatus.MOVED -> WarningAmber
            VariableStatus.DROPPED -> ErrorNeon
            VariableStatus.BORROWED -> NeonCyan
        },
        label = "statusColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.SM),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = Alpha.BORDER)
        ),
        border = BorderStroke(Dimens.Divider, statusColor.copy(alpha = Alpha.MUTED))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.MD, vertical = Spacing.SM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = variable.name,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier.width(60.dp)
            )
            Text(
                text = variable.type,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = variable.status.name.lowercase(),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}

@Composable
private fun HeapAllocationCard(alloc: HeapAllocation) {
    val statusColor = if (alloc.status == AllocationStatus.ALIVE) SuccessGreen else ErrorNeon

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.SM),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = Alpha.BORDER)
        ),
        border = BorderStroke(Dimens.Divider, statusColor.copy(alpha = Alpha.MUTED))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.MD, vertical = Spacing.SM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = alloc.value,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (alloc.status == AllocationStatus.ALIVE) "alive" else "freed",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}
