package com.sylvester.rustsensei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Rustlings Exercises",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Design Concern #3: "Continue where you left off" card
        if (uiState.lastIncompleteExerciseId != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openExercise(uiState.lastIncompleteExerciseId!!) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Continue where you left off",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = uiState.lastIncompleteExerciseId!!,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        items(uiState.categories, key = { it.id }) { category ->
            val isExpanded = uiState.expandedCategory == category.id
            val progress = uiState.categoryProgress[category.id] ?: emptyList()
            val completedCount = progress.count { it.status == "completed" }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    // Category header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleCategory(category.id) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Progress circle
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = {
                                    if (category.exercises.isNotEmpty())
                                        completedCount.toFloat() / category.exercises.size
                                    else 0f
                                },
                                modifier = Modifier.size(40.dp),
                                strokeWidth = 3.dp,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = "$completedCount/${category.exercises.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = category.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    // Expanded exercises list
                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                            category.exercises.forEach { exerciseId ->
                                val exerciseProgress = progress.find { it.exerciseId == exerciseId }
                                val isCompleted = exerciseProgress?.status == "completed"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.openExercise(exerciseId) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isCompleted) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Completed",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = exerciseId,
                                        fontSize = 14.sp,
                                        color = if (isCompleted)
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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
    // Sync ViewModel → TextFieldValue only when exercise changes externally (reset)
    val currentExerciseCode = uiState.userCode
    androidx.compose.runtime.LaunchedEffect(uiState.currentExercise?.id) {
        if (textFieldValue.text != currentExerciseCode) {
            textFieldValue = TextFieldValue(
                text = currentExerciseCode,
                selection = TextRange(currentExerciseCode.length)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = exercise.difficulty.replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.resetExercise() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Instructions
                Text(
                    text = exercise.description,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = exercise.instructions,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Code editor
                Text(
                    text = "Your Code:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            viewModel.updateCode(newValue.text)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFFD4D4D4)
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }

                // Fix #9: Coding toolbar inserts at cursor position
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                            modifier = Modifier.height(48.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = symbol,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Check result
                uiState.checkResult?.let { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when (result) {
                                "correct" -> Color(0xFF1B5E20).copy(alpha = 0.2f)
                                "uncertain" -> Color(0xFFE65100).copy(alpha = 0.15f)
                                else -> Color(0xFFB71C1C).copy(alpha = 0.2f)
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = when (result) {
                                    "correct" -> "Correct! Well done!"
                                    "uncertain" -> "Your code looks different from the expected solution. It might still be correct!"
                                    else -> "Not quite right. Try again or reveal a hint."
                                },
                                color = when (result) {
                                    "correct" -> Color(0xFF4CAF50)
                                    "uncertain" -> Color(0xFFFF9800)
                                    else -> Color(0xFFEF5350)
                                },
                                fontWeight = FontWeight.Medium
                            )
                            // Design Concern #1: LLM fallback — offer inline AI validation or self-mark
                            if (result == "uncertain") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.validateWithLlm() },
                                        modifier = Modifier.weight(1f),
                                        enabled = !uiState.isValidating
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
                                        Text(if (uiState.isValidating) "Verifying..." else "Verify with AI", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.markCurrentExerciseCorrect() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mark Correct", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // LLM validation streaming response
                if (uiState.llmValidationResult.isNotEmpty() || uiState.isValidating) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
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
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.isValidating) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.stopValidation() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Stop", fontSize = 12.sp)
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
                    Button(
                        onClick = { viewModel.checkSolution() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Check")
                    }
                    OutlinedButton(
                        onClick = { viewModel.revealHint() },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.hintsRevealed < exercise.hints.size
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hint (${uiState.hintsRevealed}/${exercise.hints.size})")
                    }
                }

                // Revealed hints
                if (uiState.hintsRevealed > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Hints:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    for (i in 0 until uiState.hintsRevealed.coerceAtMost(exercise.hints.size)) {
                        Text(
                            text = "${i + 1}. ${exercise.hints[i]}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                // Show solution button (available after viewing at least 1 hint)
                if (uiState.hintsRevealed > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.showSolution() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Show Solution")
                    }
                }

                // Solution
                if (uiState.showSolution) {
                    Spacer(modifier = Modifier.height(12.dp))
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
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Ask Sensei FAB
        ExtendedFloatingActionButton(
            onClick = { onAskSensei(exercise.description, uiState.userCode) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            icon = { Icon(Icons.Default.Chat, contentDescription = null) },
            text = { Text("Ask Sensei") }
        )
    }
}
