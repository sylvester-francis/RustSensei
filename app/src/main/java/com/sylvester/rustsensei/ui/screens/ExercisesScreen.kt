package com.sylvester.rustsensei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.ui.components.CodeBlock
import com.sylvester.rustsensei.viewmodel.ExerciseScreenMode
import com.sylvester.rustsensei.viewmodel.ExerciseViewModel

@Composable
fun ExercisesScreen(
    viewModel: ExerciseViewModel,
    onAskSensei: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.mode != ExerciseScreenMode.CATEGORIES) {
        viewModel.navigateBack()
    }

    when (uiState.mode) {
        ExerciseScreenMode.CATEGORIES -> CategoriesView(viewModel = viewModel)
        ExerciseScreenMode.DETAIL -> ExerciseDetailView(
            viewModel = viewModel,
            onAskSensei = onAskSensei
        )
    }
}

@Composable
private fun CategoriesView(viewModel: ExerciseViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Practice",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Continue card
        if (uiState.lastIncompleteExerciseId != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.openExercise(uiState.lastIncompleteExerciseId!!) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Continue where you left off",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = uiState.lastIncompleteExerciseId!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(uiState.categories, key = { it.id }) { category ->
            val isExpanded = uiState.expandedCategory == category.id
            val progress = uiState.categoryProgress[category.id] ?: emptyList()
            val completedCount = progress.count { it.status == "completed" }

            // Chevron rotation
            val chevronRotation by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                label = "chevron"
            )

            Column {
                // Category header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleCategory(category.id) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${category.exercises.size} exercises \u00B7 $completedCount done",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(chevronRotation)
                    )
                }

                // Expanded exercises list
                AnimatedVisibility(visible = isExpanded) {
                    Column(modifier = Modifier.padding(start = 32.dp, end = 16.dp, bottom = 8.dp)) {
                        category.exercises.forEach { exerciseId ->
                            val exerciseProgress = progress.find { it.exerciseId == exerciseId }
                            val isCompleted = exerciseProgress?.status == "completed"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.openExercise(exerciseId) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isCompleted) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = Color(0xFF3FB950),
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = exerciseId,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCompleted)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// Determine difficulty color based on exercise naming patterns
private fun getDifficultyColor(exerciseId: String): Color {
    return when {
        exerciseId.contains("1") || exerciseId.contains("intro") ||
            exerciseId.contains("primitive") || exerciseId.contains("variable") -> Color(0xFF3FB950) // green - beginner
        exerciseId.contains("error") || exerciseId.contains("generic") ||
            exerciseId.contains("trait") || exerciseId.contains("lifetime") ||
            exerciseId.contains("thread") || exerciseId.contains("macro") ||
            exerciseId.contains("smart_pointer") || exerciseId.contains("iterator") -> Color(0xFFF85149) // red - advanced
        else -> Color(0xFFF0883E) // amber - intermediate
    }
}

private fun getDifficultyLabel(exerciseId: String): String {
    return when {
        exerciseId.contains("1") || exerciseId.contains("intro") ||
            exerciseId.contains("primitive") || exerciseId.contains("variable") -> "Beginner"
        exerciseId.contains("error") || exerciseId.contains("generic") ||
            exerciseId.contains("trait") || exerciseId.contains("lifetime") ||
            exerciseId.contains("thread") || exerciseId.contains("macro") ||
            exerciseId.contains("smart_pointer") || exerciseId.contains("iterator") -> "Advanced"
        else -> "Intermediate"
    }
}

@Composable
private fun ExerciseDetailView(
    viewModel: ExerciseViewModel,
    onAskSensei: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val exercise = uiState.currentExercise ?: return

    // P0 Fix #3: key only on exercise ID, NOT userCode — otherwise every keystroke
    // recreates the TextFieldValue and resets the cursor to the end
    var textFieldValue by remember(uiState.currentExercise?.id) {
        mutableStateOf(TextFieldValue(
            text = uiState.userCode,
            selection = TextRange(uiState.userCode.length)
        ))
    }
    // Sync ViewModel -> TextFieldValue only when exercise changes externally (reset)
    val currentExerciseCode = uiState.userCode
    androidx.compose.runtime.LaunchedEffect(uiState.currentExercise?.id) {
        if (textFieldValue.text != currentExerciseCode) {
            textFieldValue = TextFieldValue(
                text = currentExerciseCode,
                selection = TextRange(currentExerciseCode.length)
            )
        }
    }

    // Difficulty label
    val difficultyColor = when (exercise.difficulty.lowercase()) {
        "beginner" -> Color(0xFF3FB950)
        "advanced" -> Color(0xFFF85149)
        else -> Color(0xFFF0883E)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = exercise.difficulty.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = difficultyColor
                )
            }
            IconButton(onClick = { viewModel.resetExercise() }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Instructions
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = exercise.description,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = exercise.instructions,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Code editor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D1117))
            ) {
                Column {
                    // Editor tab label
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161B22))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF21262D))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Editor",
                                color = Color(0xFF8B949E),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            viewModel.updateCode(newValue.text)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(14.dp),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFFD4D4D4)
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // Symbol toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val symbols = listOf("{", "}", "(", ")", "[", "]", "<", ">", ";", ":", "&", "*", "=", "\"", "'", "|", "->", "::", "=>")
                symbols.forEach { symbol ->
                    TextButton(
                        onClick = {
                            val cursorPos = textFieldValue.selection.start
                            val before = textFieldValue.text.substring(0, cursorPos)
                            val after = textFieldValue.text.substring(cursorPos)
                            val newText = before + symbol + after
                            val newCursorPos = cursorPos + symbol.length
                            textFieldValue = TextFieldValue(
                                text = newText,
                                selection = TextRange(newCursorPos)
                            )
                            viewModel.updateCode(newText)
                        },
                        // P3 Fix #13: minimum 48dp touch target per Material guidelines
                        modifier = Modifier.height(44.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = symbol,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Check result
            uiState.checkResult?.let { result ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (result) {
                                "correct" -> Color(0xFF3FB950).copy(alpha = 0.1f)
                                "uncertain" -> Color(0xFFF0883E).copy(alpha = 0.1f)
                                else -> Color(0xFFF85149).copy(alpha = 0.1f)
                            }
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = when (result) {
                                "correct" -> "Correct! Well done!"
                                "uncertain" -> "Your code looks different from the expected solution. It might still be correct!"
                                else -> "Not quite right. Try again or reveal a hint."
                            },
                            color = when (result) {
                                "correct" -> Color(0xFF3FB950)
                                "uncertain" -> Color(0xFFF0883E)
                                else -> Color(0xFFF85149)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        // Design Concern #1: LLM fallback — offer inline AI validation or self-mark
                        if (result == "uncertain") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.validateWithLlm() },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isValidating,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (uiState.isValidating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (uiState.isValidating) "Verifying..." else "Verify with AI",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.markCurrentExerciseCorrect() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mark Correct", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // LLM validation streaming response
            if (uiState.llmValidationResult.isNotEmpty() || uiState.isValidating) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AI Review",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (uiState.isValidating) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.llmValidationResult.ifEmpty { "Analyzing your code..." },
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.isValidating) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.stopValidation() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Stop", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.checkSolution() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Check", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = { viewModel.revealHint() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    enabled = uiState.hintsRevealed < exercise.hints.size,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hint (${uiState.hintsRevealed}/${exercise.hints.size})", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Revealed hints
            if (uiState.hintsRevealed > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Hints:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                for (i in 0 until uiState.hintsRevealed.coerceAtMost(exercise.hints.size)) {
                    Text(
                        text = "${i + 1}. ${exercise.hints[i]}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            // Show solution (collapsible)
            if (uiState.hintsRevealed > 0 && !uiState.showSolution) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.showSolution() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Show Solution", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Solution
            if (uiState.showSolution) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Solution:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                CodeBlock(code = exercise.solution, language = "rust")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = exercise.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ask Sensei button
            OutlinedButton(
                onClick = { onAskSensei(exercise.description, uiState.userCode) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ask Sensei", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
