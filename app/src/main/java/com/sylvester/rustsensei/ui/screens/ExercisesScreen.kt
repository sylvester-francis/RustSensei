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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.ui.components.CodeBlock
import com.sylvester.rustsensei.ui.components.CodeEditor
import com.sylvester.rustsensei.ui.components.UndoRedoManager
import com.sylvester.rustsensei.viewmodel.ExerciseScreenMode
import com.sylvester.rustsensei.viewmodel.ExerciseViewModel
import androidx.compose.material.icons.filled.Quiz

// Python parallel descriptions for exercise categories
private fun pythonParallel(categoryTitle: String): String {
    val title = categoryTitle.lowercase()
    return when {
        title.contains("variable") -> "Variables (like Python's variables, but immutable by default)"
        title.contains("function") -> "Functions (like Python's def, but with type annotations)"
        title.contains("if") || title.contains("control") -> "Control Flow (similar to Python's if/else)"
        title.contains("primitive") || title.contains("type") -> "Primitive Types (like Python's int/float/str, but fixed-size)"
        title.contains("string") -> "Strings (like Python's str, but with ownership)"
        title.contains("vector") || title.contains("vec") -> "Vectors (like Python's list)"
        title.contains("struct") -> "Structs (like Python's dataclass)"
        title.contains("enum") -> "Enums (like Python's Enum, but much more powerful)"
        title.contains("module") -> "Modules (like Python's import system)"
        title.contains("hash") || title.contains("map") -> "HashMaps (like Python's dict)"
        title.contains("option") -> "Options (like Python's Optional type hint)"
        title.contains("error") -> "Error Handling (like Python's try/except, but compile-time)"
        title.contains("generic") -> "Generics (like Python's TypeVar)"
        title.contains("trait") -> "Traits (like Python's abstract base classes)"
        title.contains("lifetime") -> "Lifetimes (no Python equivalent -- this is new!)"
        title.contains("thread") -> "Threads (like Python's threading, but safe)"
        title.contains("iterator") -> "Iterators (like Python's iterators and generators)"
        title.contains("smart_pointer") || title.contains("pointer") -> "Smart Pointers (no direct Python equivalent)"
        title.contains("macro") -> "Macros (like Python's decorators, but at compile-time)"
        title.contains("closure") -> "Closures (like Python's lambda, but more powerful)"
        title.contains("move") || title.contains("ownership") -> "Ownership (no Python equivalent -- core Rust concept)"
        title.contains("borrow") || title.contains("reference") -> "References (like Python's object references, but checked)"
        else -> categoryTitle
    }
}

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

@Composable
private fun CategoriesView(
    viewModel: ExerciseViewModel,
    onNavigateToQuiz: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Calculate total exercise count
    val totalExercises = uiState.categories.sumOf { it.exercises.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            // headlineMedium (monospace via theme)
            Text(
                text = "Practice",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "$totalExercises exercises from Rustlings",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Continue card — more visual weight
        val lastExerciseId = uiState.lastIncompleteExerciseId
        if (lastExerciseId != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { viewModel.openExercise(lastExerciseId) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .semantics { contentDescription = "Continue exercise: $lastExerciseId" },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Continue exercise",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Continue where you left off",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = lastExerciseId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Quiz entry card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onNavigateToQuiz() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Quiz,
                        contentDescription = "Topic Quizzes",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Topic Quizzes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Test your knowledge after each chapter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
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
                        // Category name with Python parallel
                        Text(
                            text = pythonParallel(category.title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        // Stat in labelSmall
                        Text(
                            text = "${category.exercises.size} exercises \u00B7 $completedCount done",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse category" else "Expand category",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(chevronRotation)
                    )
                }

                // Expanded exercises list — prefix with ">" in primary (monospace)
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
                                    // ">" prefix in primary, monospace
                                    Text(
                                        text = ">",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary,
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

                // Orange-tinted neon divider
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
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

    // Undo/redo manager — keyed to exercise so it resets on navigation
    val undoRedoManager = remember(uiState.currentExercise?.id) {
        UndoRedoManager().also { it.push(uiState.userCode) }
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

            // Code editor with line numbers, syntax highlighting, auto-indent/brackets
            CodeEditor(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    undoRedoManager.push(newValue.text)
                    viewModel.updateCode(newValue.text)
                },
                modifier = Modifier.fillMaxWidth(),
                minHeight = 240.dp
            )

            // Symbol toolbar with undo/redo — surface background, sharp corners
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo button
                TextButton(
                    onClick = {
                        undoRedoManager.undo()?.let { undoneText ->
                            textFieldValue = TextFieldValue(
                                text = undoneText,
                                selection = TextRange(undoneText.length)
                            )
                            viewModel.updateCode(undoneText)
                        }
                    },
                    enabled = undoRedoManager.canUndo(),
                    modifier = Modifier.height(44.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Undo",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (undoRedoManager.canUndo())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                // Redo button
                TextButton(
                    onClick = {
                        undoRedoManager.redo()?.let { redoneText ->
                            textFieldValue = TextFieldValue(
                                text = redoneText,
                                selection = TextRange(redoneText.length)
                            )
                            viewModel.updateCode(redoneText)
                        }
                    },
                    enabled = undoRedoManager.canRedo(),
                    modifier = Modifier.height(44.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Redo",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (undoRedoManager.canRedo())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                // Separator
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                )

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
                            undoRedoManager.push(newText)
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
                        .clip(RoundedCornerShape(8.dp))
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
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (uiState.isValidating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Verify with AI", modifier = Modifier.size(16.dp))
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
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Mark correct", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mark Correct", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // AI Review section — visually separated with a section divider
            if (uiState.llmValidationResult.isNotEmpty() || uiState.isValidating) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Row {
                        // Primary-colored left border (3dp)
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(120.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Stop", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Action buttons — sharp 8dp corners, outlined, 12dp spacing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.checkSolution() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Check solution", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Check", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = { viewModel.revealHint() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = uiState.hintsRevealed < exercise.hints.size,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = "Reveal hint", modifier = Modifier.size(18.dp))
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
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = "Show solution", modifier = Modifier.size(18.dp))
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

            // Ask Sensei button — sharp 8dp corners, 48dp touch target
            OutlinedButton(
                onClick = { onAskSensei(exercise.description, uiState.userCode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Ask Sensei for help", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ask Sensei", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
