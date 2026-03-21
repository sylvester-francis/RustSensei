package com.sylvester.rustsensei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.ui.theme.Alpha
import com.sylvester.rustsensei.ui.theme.DarkSurfaceContainerHigh
import com.sylvester.rustsensei.ui.theme.DifficultyAdvanced
import com.sylvester.rustsensei.ui.theme.DifficultyBeginner
import com.sylvester.rustsensei.ui.theme.DifficultyIntermediate
import com.sylvester.rustsensei.ui.theme.Dimens
import com.sylvester.rustsensei.ui.theme.NeonCyan
import com.sylvester.rustsensei.ui.theme.RustOrange
import com.sylvester.rustsensei.ui.theme.SecondaryText
import com.sylvester.rustsensei.ui.theme.Spacing
import com.sylvester.rustsensei.ui.theme.SuccessGreen
import com.sylvester.rustsensei.viewmodel.ExerciseScreenMode
import com.sylvester.rustsensei.viewmodel.ExerciseViewModel

// ── Python parallel descriptions (data map) ─────────────────────────

private val pythonParallels = mapOf(
    "variable" to "Variables (like Python's variables, but immutable by default)",
    "function" to "Functions (like Python's def, but with type annotations)",
    "if" to "Control Flow (similar to Python's if/else)",
    "control" to "Control Flow (similar to Python's if/else)",
    "primitive" to "Primitive Types (like Python's int/float/str, but fixed-size)",
    "type" to "Primitive Types (like Python's int/float/str, but fixed-size)",
    "string" to "Strings (like Python's str, but with ownership)",
    "vector" to "Vectors (like Python's list)",
    "vec" to "Vectors (like Python's list)",
    "struct" to "Structs (like Python's dataclass)",
    "enum" to "Enums (like Python's Enum, but much more powerful)",
    "module" to "Modules (like Python's import system)",
    "hash" to "HashMaps (like Python's dict)",
    "map" to "HashMaps (like Python's dict)",
    "option" to "Options (like Python's Optional type hint)",
    "error" to "Error Handling (like Python's try/except, but compile-time)",
    "generic" to "Generics (like Python's TypeVar)",
    "trait" to "Traits (like Python's abstract base classes)",
    "lifetime" to "Lifetimes (no Python equivalent -- this is new!)",
    "thread" to "Threads (like Python's threading, but safe)",
    "iterator" to "Iterators (like Python's iterators and generators)",
    "smart_pointer" to "Smart Pointers (no direct Python equivalent)",
    "pointer" to "Smart Pointers (no direct Python equivalent)",
    "macro" to "Macros (like Python's decorators, but at compile-time)",
    "closure" to "Closures (like Python's lambda, but more powerful)",
    "move" to "Ownership (no Python equivalent -- core Rust concept)",
    "ownership" to "Ownership (no Python equivalent -- core Rust concept)",
    "borrow" to "References (like Python's object references, but checked)",
    "reference" to "References (like Python's object references, but checked)"
)

internal fun pythonParallel(categoryTitle: String): String {
    val title = categoryTitle.lowercase()
    return pythonParallels.entries.firstOrNull { title.contains(it.key) }?.value
        ?: categoryTitle
}

// ── Difficulty helpers for categories list (exercise-ID based) ───────
// In the categories list we only have exercise IDs (strings), not the
// full ExerciseData. These heuristics map naming patterns to difficulty.

private fun getDifficultyColor(exerciseId: String): Color {
    val id = exerciseId.lowercase()
    return when {
        id.contains("1") || id.contains("intro") ||
            id.contains("primitive") || id.contains("variable") -> DifficultyBeginner
        id.contains("error") || id.contains("generic") ||
            id.contains("trait") || id.contains("lifetime") ||
            id.contains("thread") || id.contains("macro") ||
            id.contains("smart_pointer") || id.contains("iterator") -> DifficultyAdvanced
        else -> DifficultyIntermediate
    }
}

private fun getDifficultyLabel(exerciseId: String): String {
    val id = exerciseId.lowercase()
    return when {
        id.contains("1") || id.contains("intro") ||
            id.contains("primitive") || id.contains("variable") -> "Easy"
        id.contains("error") || id.contains("generic") ||
            id.contains("trait") || id.contains("lifetime") ||
            id.contains("thread") || id.contains("macro") ||
            id.contains("smart_pointer") || id.contains("iterator") -> "Hard"
        else -> "Medium"
    }
}

// ── Router composable ───────────────────────────────────────────────

@Composable
fun ExercisesScreen(
    viewModel: ExerciseViewModel,
    onAskSensei: (String, String) -> Unit,
    onNavigateToQuiz: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.mode != ExerciseScreenMode.CATEGORIES) {
        viewModel.navigateBack()
    }

    when (uiState.mode) {
        ExerciseScreenMode.CATEGORIES -> CategoriesView(
            viewModel = viewModel,
            onNavigateToQuiz = onNavigateToQuiz
        )
        ExerciseScreenMode.DETAIL -> ExerciseDetailView(
            viewModel = viewModel,
            onAskSensei = onAskSensei
        )
    }
}

// ── Categories View ─────────────────────────────────────────────────

@Composable
private fun CategoriesView(
    viewModel: ExerciseViewModel,
    onNavigateToQuiz: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val totalExercises = uiState.categories.sumOf { it.exercises.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding)
    ) {
        // -- Heading --
        item {
            Text(
                text = "Practice",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = Spacing.XS)
            )
            Text(
                text = "$totalExercises exercises from Rustlings",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )
            Spacer(modifier = Modifier.height(Spacing.LG))
        }

        // -- Continue card --
        val lastExerciseId = uiState.lastIncompleteExerciseId
        if (lastExerciseId != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.LG)
                        .clickable { viewModel.openExercise(lastExerciseId) },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.CardPadding)
                            .semantics {
                                contentDescription = "Continue exercise: $lastExerciseId"
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(RustOrange.copy(alpha = Alpha.BORDER)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Continue exercise",
                                tint = RustOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.MD))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Continue where you left off",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = RustOrange
                            )
                            Spacer(modifier = Modifier.height(Spacing.XXS))
                            Text(
                                text = lastExerciseId,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = SecondaryText
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = RustOrange.copy(alpha = Alpha.SECONDARY),
                            modifier = Modifier.size(Dimens.IconSM)
                        )
                    }
                }
            }
        }

        // -- Mode strip: Exercises | Quizzes --
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.LG),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Exercises (active)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Dimens.CardRadius))
                        .background(RustOrange.copy(alpha = Alpha.BORDER))
                        .border(
                            Dimens.Divider,
                            RustOrange.copy(alpha = Alpha.MUTED),
                            RoundedCornerShape(Dimens.CardRadius)
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = RustOrange,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(Spacing.SM))
                        Text(
                            text = "Exercises",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = RustOrange
                        )
                        Text(
                            text = "$totalExercises Rustlings",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = RustOrange.copy(alpha = Alpha.SOFT)
                        )
                    }
                }

                // Quizzes (navigable)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Dimens.CardRadius))
                        .background(NeonCyan.copy(alpha = 0.08f))
                        .border(
                            Dimens.Divider,
                            NeonCyan.copy(alpha = Alpha.DIVIDER),
                            RoundedCornerShape(Dimens.CardRadius)
                        )
                        .clickable { onNavigateToQuiz() }
                        .padding(14.dp)
                ) {
                    Column {
                        Icon(
                            Icons.Default.Quiz,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(Spacing.SM))
                        Text(
                            text = "Quizzes",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Text(
                            text = "Test knowledge",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan.copy(alpha = Alpha.SOFT)
                        )
                    }
                }
            }
        }

        // -- Empty state --
        if (uiState.categories.isEmpty()) {
            item(key = "empty-exercises") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.Section),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(Spacing.Section),
                        tint = SecondaryText.copy(alpha = Alpha.MUTED)
                    )
                    Spacer(modifier = Modifier.height(Spacing.LG))
                    Text(
                        text = "No exercises available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.SECONDARY)
                    )
                    Text(
                        text = "Content may still be loading",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // -- Category list --
        items(uiState.categories, key = { it.id }) { category ->
            val isExpanded = uiState.expandedCategory == category.id
            val progress = uiState.categoryProgress[category.id] ?: emptyList()
            val completedCount = progress.count { it.status == "completed" }

            val chevronRotation by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = tween(durationMillis = 250),
                label = "chevron"
            )

            Column {
                // Category header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.CardRadius))
                        .clickable { viewModel.toggleCategory(category.id) }
                        .padding(horizontal = Spacing.MD, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pythonParallel(category.title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.XXS))
                        Text(
                            text = "${category.exercises.size} exercises \u00B7 $completedCount done",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = SecondaryText
                        )
                    }
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse category" else "Expand category",
                        tint = SecondaryText,
                        modifier = Modifier
                            .size(Dimens.IconSM)
                            .rotate(chevronRotation)
                    )
                }

                // Expanded exercise items
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier.padding(
                            start = Spacing.LG,
                            end = Spacing.MD,
                            bottom = Spacing.SM
                        )
                    ) {
                        category.exercises.forEach { exerciseId ->
                            val exerciseProgress =
                                progress.find { it.exerciseId == exerciseId }
                            val isCompleted =
                                exerciseProgress?.status == "completed"
                            val diffColor = getDifficultyColor(exerciseId)
                            val diffLabel = getDifficultyLabel(exerciseId)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(Spacing.SM))
                                    .clickable { viewModel.openExercise(exerciseId) }
                                    .padding(vertical = 10.dp, horizontal = Spacing.XS),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Status indicator
                                if (isCompleted) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Not completed",
                                        tint = RustOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))

                                // Exercise name
                                Text(
                                    text = exerciseId,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCompleted)
                                        SecondaryText
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                // Difficulty pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(diffColor.copy(alpha = Alpha.BORDER))
                                        .padding(horizontal = Spacing.SM, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = diffLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = diffColor
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                )
            }
        }

        // Bottom spacer
        item { Spacer(modifier = Modifier.height(Spacing.LG)) }
    }
}
