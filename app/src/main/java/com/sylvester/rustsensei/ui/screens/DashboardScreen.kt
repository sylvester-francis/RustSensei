package com.sylvester.rustsensei.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.ui.components.ActivityChart
import com.sylvester.rustsensei.ui.components.ProgressRing
import com.sylvester.rustsensei.ui.theme.NeonCyan
import com.sylvester.rustsensei.ui.theme.WarningAmber
import com.sylvester.rustsensei.viewmodel.Achievement
import com.sylvester.rustsensei.viewmodel.LearningPathViewModel
import com.sylvester.rustsensei.viewmodel.ProgressViewModel
import com.sylvester.rustsensei.viewmodel.ReviewViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: ProgressViewModel,
    reviewViewModel: ReviewViewModel,
    learningPathViewModel: LearningPathViewModel,
    onNavigateToReview: () -> Unit = {},
    onNavigateToLearningPaths: () -> Unit = {},
    onContinueReading: ((chapterId: String, sectionId: String) -> Unit)? = null,
    onContinueExercise: ((exerciseId: String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val reviewUiState by reviewViewModel.uiState.collectAsState()
    val pathUiState by learningPathViewModel.uiState.collectAsState()

    // Time-aware greeting
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning, Rustacean!"
            hour < 17 -> "Good afternoon, Rustacean!"
            else -> "Good evening, Rustacean!"
        }
    }

    // Daily motivational quote that changes by day of year
    val dailyQuote = remember {
        val quotes = listOf(
            "\"The only way to learn a new programming language is by writing programs in it.\" -- Dennis Ritchie",
            "\"First, solve the problem. Then, write the code.\" -- John Johnson",
            "\"Ownership is not a limitation -- it's a superpower.\" -- The Rust Community",
            "\"The compiler is your strictest mentor, and your most reliable friend.\" -- Anonymous Rustacean",
            "\"Every expert was once a beginner.\" -- Helen Hayes",
            "\"Code is like humor. When you have to explain it, it's bad.\" -- Cory House",
            "\"The borrow checker doesn't fight you -- it teaches you.\" -- Anonymous Rustacean"
        )
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        quotes[dayOfYear % quotes.size]
    }

    // Calculate daily goal progress (read 1 chapter, 1 exercise, 1 quiz)
    val todayStats = uiState.weeklyStats.lastOrNull()
    val sectionsToday = todayStats?.sectionsRead ?: 0
    val exercisesToday = todayStats?.exercisesCompleted ?: 0
    // Daily goals: Read 1 chapter, Complete 1 exercise, Complete 1 quiz
    val chapterGoalDone = sectionsToday >= 1
    val exerciseGoalDone = exercisesToday >= 1
    // Quiz is harder to track directly, so use a heuristic based on study time
    val quizGoalDone = (todayStats?.studyTimeSeconds ?: 0) > 300 && (sectionsToday + exercisesToday >= 2)
    val goalsCompleted = listOf(chapterGoalDone, exerciseGoalDone, quizGoalDone).count { it }
    val dailyGoalProgress = goalsCompleted / 3f
    val dailyGoalPercent = (dailyGoalProgress * 100).toInt()

    val cardShape = RoundedCornerShape(12.dp)
    val cardBackground = MaterialTheme.colorScheme.surfaceContainerHigh

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =====================================================
        // Section 1: Hero Greeting + Streak
        // =====================================================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Greeting
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Streak with flame icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val transition = rememberInfiniteTransition(label = "streak_flame")
                        val flameScale by transition.animateFloat(
                            initialValue = 0.95f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 1500),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "flame_scale"
                        )
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak flame",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(32.dp)
                                .scale(flameScale)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${uiState.studyStreak}-day streak",
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Weekly dots: 7 circles (Mo-Su)
                    if (uiState.weekActivity.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            uiState.weekActivity.forEachIndexed { index, day ->
                                val isToday = index == uiState.weekActivity.lastIndex
                                WeekDayDot(
                                    label = day.label,
                                    level = day.level,
                                    isToday = isToday,
                                    primaryColor = MaterialTheme.colorScheme.primary,
                                    surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================
        // Section 2: Daily Goal Card
        // =====================================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = cardBackground
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Goal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$goalsCompleted/3 complete -- $dailyGoalPercent%",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { dailyGoalProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Checklist items
                    DailyGoalCheckItem(
                        text = "Read 1 chapter",
                        isCompleted = chapterGoalDone
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DailyGoalCheckItem(
                        text = "Complete 1 exercise",
                        isCompleted = exerciseGoalDone
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DailyGoalCheckItem(
                        text = "Complete 1 quiz",
                        isCompleted = quizGoalDone
                    )
                }
            }
        }

        // =====================================================
        // Section 3: Activity Heatmap
        // =====================================================
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "This Week",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                if (uiState.weeklyStats.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(cardShape)
                            .background(cardBackground),
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

        // =====================================================
        // Section 4: Quick Action Cards (Horizontal scroll)
        // =====================================================
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                item {
                    QuickActionCard(
                        title = "Continue Reading",
                        subtitle = if (uiState.continueTarget?.type == "section")
                            "Resume last section"
                        else
                            "${uiState.completedSections} of ${uiState.totalSections} done",
                        icon = Icons.Default.MenuBook,
                        accentColor = MaterialTheme.colorScheme.primary,
                        cardBackground = cardBackground,
                        onClick = {
                            val target = uiState.continueTarget
                            if (target != null && target.type == "section" && target.parentId != null) {
                                onContinueReading?.invoke(target.parentId, target.id)
                            }
                        }
                    )
                }
                item {
                    QuickActionCard(
                        title = "Exercises",
                        subtitle = "${uiState.totalExercises - uiState.completedExercises} pending",
                        icon = Icons.Default.Code,
                        accentColor = NeonCyan,
                        cardBackground = cardBackground,
                        onClick = {
                            val target = uiState.continueTarget
                            if (target != null && target.type == "exercise") {
                                onContinueExercise?.invoke(target.id)
                            }
                        }
                    )
                }
                item {
                    QuickActionCard(
                        title = "Start Quiz",
                        subtitle = "Test your knowledge",
                        icon = Icons.Default.Quiz,
                        accentColor = WarningAmber,
                        cardBackground = cardBackground,
                        onClick = { /* Quiz navigation handled by parent */ }
                    )
                }
            }
        }

        // =====================================================
        // Section 5: Progress Overview (3 Radial Charts)
        // =====================================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val chapterProgress = if (uiState.totalSections > 0)
                    uiState.completedSections.toFloat() / uiState.totalSections else 0f
                val exerciseProgress = if (uiState.totalExercises > 0)
                    uiState.completedExercises.toFloat() / uiState.totalExercises else 0f
                // Quiz progress: use a fraction based on available data
                val quizProgress = dailyGoalProgress // Approximate with daily goal as proxy

                ProgressRingWithLabel(
                    progress = chapterProgress,
                    label = "Chapters",
                    fraction = "${uiState.completedSections}/${uiState.totalSections}",
                    color = MaterialTheme.colorScheme.primary
                )
                ProgressRingWithLabel(
                    progress = exerciseProgress,
                    label = "Exercises",
                    fraction = "${uiState.completedExercises}/${uiState.totalExercises}",
                    color = NeonCyan
                )
                ProgressRingWithLabel(
                    progress = quizProgress,
                    label = "Quizzes",
                    fraction = "$goalsCompleted/3",
                    color = WarningAmber
                )
            }
        }

        // =====================================================
        // Section 6: Achievements
        // =====================================================
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                if (uiState.achievements.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.achievements, key = { it.id }) { achievement ->
                            AchievementBadge(achievement = achievement)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val unlockedCount = uiState.achievements.count { it.isUnlocked }
                    Text(
                        text = "$unlockedCount of ${uiState.achievements.size} unlocked",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                } else {
                    Text(
                        text = "Keep learning to unlock badges!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // =====================================================
        // Section 9: Recent Activity / Notes
        // =====================================================
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (uiState.recentNotes.size > 3) {
                        Text(
                            text = "See all",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { /* future navigation */ }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.recentNotes.isEmpty()) {
                    Text(
                        text = "No notes yet. Add notes while reading sections!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                } else {
                    uiState.recentNotes.forEach { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = cardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = cardBackground
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Note",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = note.sectionId.replace("-", " "),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val dateStr = remember(note.updatedAt) {
                                    SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(note.updatedAt))
                                }
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================
        // Section 10: Motivational Quote
        // =====================================================
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = dailyQuote,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// =====================================================================
// Private composables
// =====================================================================

@Composable
private fun WeekDayDot(
    label: String,
    level: Int,
    isToday: Boolean,
    primaryColor: Color,
    surfaceVariantColor: Color
) {
    val dotSize = 12.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            // Today with no activity: pulsing outline
            isToday && level == 0 -> {
                val transition = rememberInfiniteTransition(label = "today_pulse_$label")
                val pulseAlpha by transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1200),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_alpha_$label"
                )
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = primaryColor.copy(alpha = pulseAlpha),
                            shape = CircleShape
                        )
                )
            }
            // Completed day (today or past): filled primary
            level > 0 -> {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
            }
            // Missed past day: surfaceVariant
            else -> {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(surfaceVariantColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label.take(2), // Mo, Tu, We, Th, Fr, Sa, Su
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = if (isToday) primaryColor
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun DailyGoalCheckItem(
    text: String,
    isCompleted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        Icon(
            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = if (isCompleted) "Completed: $text" else "Not completed: $text",
            modifier = Modifier.size(18.dp),
            tint = if (isCompleted)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isCompleted)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    cardBackground: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(150.dp)
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        )
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Colored left-edge accent
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxSize()
                    .background(accentColor)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = accentColor
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressRingWithLabel(
    progress: Float,
    label: String,
    fraction: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics {
            contentDescription = "$label: ${(progress * 100).toInt()}%, $fraction"
        }
    ) {
        ProgressRing(
            progress = progress,
            diameter = 80.dp,
            thickness = 8.dp,
            color = color,
            centerText = "${(progress * 100).toInt()}%"
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = fraction,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AchievementBadge(achievement: Achievement) {
    val bgColor = if (achievement.isUnlocked) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    }
    val borderColor = if (achievement.isUnlocked) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    }
    val contentAlpha = if (achievement.isUnlocked) 1f else 0.35f

    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (achievement.isUnlocked) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                } else Modifier
            )
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = achievement.icon,
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = achievement.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            lineHeight = 14.sp
        )
    }
}

private fun formatStudyTime(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
