package com.sylvester.rustsensei.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.sylvester.rustsensei.ui.components.ActivityChart
import com.sylvester.rustsensei.viewmodel.ProgressViewModel

@Composable
fun DashboardScreen(
    viewModel: ProgressViewModel,
    onContinueReading: ((chapterId: String, sectionId: String) -> Unit)? = null,
    onContinueExercise: ((exerciseId: String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // headlineMedium (monospace via theme)
            Text(
                text = "Progress",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            // Motivational message — keep subtle
            val streakMessage = when {
                uiState.studyStreak >= 30 -> "Incredible! A whole month of consistency!"
                uiState.studyStreak >= 14 -> "Two weeks strong! You're unstoppable."
                uiState.studyStreak >= 7 -> "One week streak! Keep the momentum going."
                uiState.studyStreak >= 3 -> "Great start! Keep showing up daily."
                uiState.studyStreak >= 1 -> "Nice! You're building a habit."
                else -> "Start learning today to build your streak."
            }
            Text(
                text = streakMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        // Continue Learning
        uiState.continueTarget?.let { target ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Continue Learning",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (target.type == "section") "Resume reading: ${target.id.replace("-", " ")}"
                                   else "Resume exercise: ${target.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Stat cards - 2x2 grid — monospace values
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AutoStories,
                    title = "Sections",
                    value = "${uiState.completedSections}",
                    subtitle = "of ${uiState.totalSections}",
                    progress = if (uiState.totalSections > 0)
                        uiState.completedSections.toFloat() / uiState.totalSections
                    else 0f,
                    accentColor = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Code,
                    title = "Exercises",
                    value = "${uiState.completedExercises}",
                    subtitle = "of ${uiState.totalExercises}",
                    progress = if (uiState.totalExercises > 0)
                        uiState.completedExercises.toFloat() / uiState.totalExercises
                    else 0f,
                    accentColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocalFireDepartment,
                    title = "Streak",
                    value = "${uiState.studyStreak}",
                    subtitle = "days",
                    progress = null,
                    accentColor = MaterialTheme.colorScheme.secondary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Timer,
                    title = "Study Time",
                    value = formatStudyTime(uiState.totalStudyTimeSeconds),
                    subtitle = "total",
                    progress = null,
                    accentColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // Weekly activity chart — thin neon border at top
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // Neon accent border at top of section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "This Week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                if (uiState.weeklyStats.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No activity yet this week",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start reading or solving exercises!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                } else {
                    ActivityChart(
                        stats = uiState.weeklyStats,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }
            }
        }

        // Recent activity — monospace dates
        if (uiState.weeklyStats.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            uiState.weeklyStats.sortedByDescending { it.date }.forEach { stat ->
                item(key = stat.date) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Monospace date on left
                        Text(
                            text = stat.date,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Data on right
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (stat.sectionsRead > 0) {
                                Text(
                                    text = "${stat.sectionsRead} read",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (stat.exercisesCompleted > 0) {
                                Text(
                                    text = "${stat.exercisesCompleted} solved",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            if (stat.studyTimeSeconds > 60) {
                                Text(
                                    text = formatStudyTime(stat.studyTimeSeconds),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    // Neon-tinted divider
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    progress: Float?,
    accentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = accentColor
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            // Monospace headlineSmall for stat values — precise/technical look
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (progress != null) {
            Spacer(modifier = Modifier.height(10.dp))
            // Sharp progress bar — 4dp height, 2dp corners
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Butt
            )
        }
    }
}

private fun formatStudyTime(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
