package com.sylvester.rustsensei.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.R
import com.sylvester.rustsensei.content.Project
import com.sylvester.rustsensei.ui.theme.Alpha
import com.sylvester.rustsensei.ui.theme.Dimens
import com.sylvester.rustsensei.ui.theme.NeonCyan
import com.sylvester.rustsensei.ui.theme.RustOrange
import com.sylvester.rustsensei.ui.theme.Spacing
import com.sylvester.rustsensei.ui.theme.SuccessGreen
import com.sylvester.rustsensei.viewmodel.ProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    viewModel: ProjectViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.currentProject?.title
                            ?: stringResource(R.string.projects_title),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.currentProject != null) viewModel.goBack()
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
        if (uiState.currentProject == null) {
            // Project list
            ProjectListView(
                projects = uiState.projects,
                onSelectProject = { viewModel.openProject(it) },
                modifier = Modifier.padding(padding)
            )
        } else {
            // Project detail with steps
            ProjectDetailView(
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ProjectListView(
    projects: List<Project>,
    onSelectProject: (Project) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.SM)
    ) {
        item {
            Text(
                text = stringResource(R.string.projects_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.LG)
            )
        }
        items(projects) { project ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectProject(project) },
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
                        Icons.Default.Build,
                        contentDescription = null,
                        tint = RustOrange,
                        modifier = Modifier.size(Dimens.IconLG)
                    )
                    Spacer(modifier = Modifier.width(Spacing.MD))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = project.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.XXS))
                        Text(
                            text = project.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(Spacing.XS))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.SM)) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = RustOrange.copy(alpha = Alpha.BORDER)
                                ),
                                shape = RoundedCornerShape(Spacing.XS)
                            ) {
                                Text(
                                    text = project.difficulty.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = RustOrange,
                                    modifier = Modifier.padding(horizontal = Spacing.SM, vertical = Spacing.XXS)
                                )
                            }
                            Text(
                                text = "${project.steps.size} steps \u00B7 ~${project.estimatedHours}h",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectDetailView(
    viewModel: ProjectViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val project = uiState.currentProject ?: return
    val step = uiState.currentStep

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPadding)
            .verticalScroll(rememberScrollState())
    ) {
        // Step list
        Text(
            text = stringResource(R.string.projects_steps),
            style = MaterialTheme.typography.labelLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = RustOrange,
            modifier = Modifier.padding(bottom = Spacing.SM)
        )

        project.steps.forEachIndexed { index, projectStep ->
            val isCompleted = projectStep.id in uiState.completedStepIds
            val isActive = index == uiState.currentStepIndex

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.XXS)
                    .clickable { viewModel.selectStep(index) },
                shape = RoundedCornerShape(Spacing.SM),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive)
                        RustOrange.copy(alpha = Alpha.BORDER)
                    else MaterialTheme.colorScheme.surfaceContainer
                ),
                border = if (isActive) BorderStroke(Dimens.Divider, RustOrange.copy(alpha = Alpha.MUTED)) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.MD, vertical = Spacing.SM),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isCompleted) Icons.Default.CheckCircle
                        else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isCompleted) SuccessGreen
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.HINT),
                        modifier = Modifier.size(Dimens.IconSM)
                    )
                    Spacer(modifier = Modifier.width(Spacing.SM))
                    Text(
                        text = "${index + 1}. ${projectStep.title}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) RustOrange
                               else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.XL))

        // Current step detail
        if (step != null) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.SM))

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = NeonCyan.copy(alpha = Alpha.BORDER)
                ),
                border = BorderStroke(Dimens.Divider, NeonCyan.copy(alpha = Alpha.MUTED))
            ) {
                Text(
                    text = step.instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(Dimens.CardPadding)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.LG))

            // Starter code
            Text(
                text = stringResource(R.string.projects_starter_code),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = RustOrange,
                modifier = Modifier.padding(bottom = Spacing.XS)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Text(
                    text = step.starterCode,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(Dimens.CardPadding)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.XL))

            // Complete step button
            if (step.id !in uiState.completedStepIds) {
                Button(
                    onClick = { viewModel.completeStep() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.ButtonHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = RustOrange),
                    shape = RoundedCornerShape(Dimens.CardRadius)
                ) {
                    Text(
                        text = stringResource(R.string.projects_mark_complete),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.XXXL))
    }
}
