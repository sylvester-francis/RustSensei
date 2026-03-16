package com.sylvester.rustsensei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.ui.components.CodeBlock
import com.sylvester.rustsensei.ui.components.CodeEditor
import com.sylvester.rustsensei.ui.components.UndoRedoManager
import com.sylvester.rustsensei.ui.theme.DangerRed
import com.sylvester.rustsensei.ui.theme.DarkSurfaceContainerHigh
import com.sylvester.rustsensei.ui.theme.ErrorNeon
import com.sylvester.rustsensei.ui.theme.NeonCyan
import com.sylvester.rustsensei.ui.theme.RustOrange
import com.sylvester.rustsensei.ui.theme.SecondaryText
import com.sylvester.rustsensei.ui.theme.SuccessGreen
import com.sylvester.rustsensei.ui.theme.WarningAmber
import com.sylvester.rustsensei.viewmodel.ExerciseScreenMode
import com.sylvester.rustsensei.viewmodel.ExerciseViewModel

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

// Determine difficulty color based on exercise naming patterns
private fun getDifficultyColor(exerciseId: String): Color {
    return when {
        exerciseId.contains("1") || exerciseId.contains("intro") ||
            exerciseId.contains("primitive") || exerciseId.contains("variable") -> SuccessGreen
        exerciseId.contains("error") || exerciseId.contains("generic") ||
            exerciseId.contains("trait") || exerciseId.contains("lifetime") ||
            exerciseId.contains("thread") || exerciseId.contains("macro") ||
            exerciseId.contains("smart_pointer") || exerciseId.contains("iterator") -> DangerRed
        else -> WarningAmber
    }
}

private fun getDifficultyLabel(exerciseId: String): String {
    return when {
        exerciseId.contains("1") || exerciseId.contains("intro") ||
            exerciseId.contains("primitive") || exerciseId.contains("variable") -> "Easy"
        exerciseId.contains("error") || exerciseId.contains("generic") ||
            exerciseId.contains("trait") || exerciseId.contains("lifetime") ||
            exerciseId.contains("thread") || exerciseId.contains("macro") ||
            exerciseId.contains("smart_pointer") || exerciseId.contains("iterator") -> "Hard"
        else -> "Medium"
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

// ---- CATEGORIES VIEW --------------------------------------------------------

@Composable
private fun CategoriesView(
    viewModel: ExerciseViewModel,
    onNavigateToQuiz: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val totalExercises = uiState.categories.sumOf { it.exercises.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // -- Heading --
        item {
            Text(
                text = "Practice",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "$totalExercises exercises from Rustlings",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // -- Continue card --
        val lastExerciseId = uiState.lastIncompleteExerciseId
        if (lastExerciseId != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { viewModel.openExercise(lastExerciseId) },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .semantics {
                                contentDescription = "Continue exercise: $lastExerciseId"
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(RustOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Continue exercise",
                                tint = RustOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Continue where you left off",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = RustOrange
                            )
                            Spacer(modifier = Modifier.height(2.dp))
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
                            tint = RustOrange.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
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
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Exercises (active)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RustOrange.copy(alpha = 0.12f))
                        .border(
                            1.dp,
                            RustOrange.copy(alpha = 0.30f),
                            RoundedCornerShape(12.dp)
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
                        Spacer(modifier = Modifier.height(8.dp))
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
                            color = RustOrange.copy(alpha = 0.7f)
                        )
                    }
                }

                // Quizzes (navigable)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonCyan.copy(alpha = 0.08f))
                        .border(
                            1.dp,
                            NeonCyan.copy(alpha = 0.20f),
                            RoundedCornerShape(12.dp)
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
                        Spacer(modifier = Modifier.height(8.dp))
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
                            color = NeonCyan.copy(alpha = 0.7f)
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
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = SecondaryText.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No exercises available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.toggleCategory(category.id) }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pythonParallel(category.title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
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
                            .size(20.dp)
                            .rotate(chevronRotation)
                    )
                }

                // Expanded exercise items
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 12.dp,
                            bottom = 8.dp
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.openExercise(exerciseId) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
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
                                        .background(diffColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
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
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ---- EXERCISE DETAIL VIEW ---------------------------------------------------

@Composable
private fun ExerciseDetailView(
    viewModel: ExerciseViewModel,
    onAskSensei: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val exercise = uiState.currentExercise ?: return

    // TextFieldValue keyed only on exercise ID to avoid cursor-reset on every keystroke
    var textFieldValue by remember(uiState.currentExercise?.id) {
        mutableStateOf(
            TextFieldValue(
                text = uiState.userCode,
                selection = TextRange(uiState.userCode.length)
            )
        )
    }

    // Sync ViewModel -> TextFieldValue only when exercise changes externally (reset)
    val currentExerciseCode = uiState.userCode
    LaunchedEffect(uiState.currentExercise?.id) {
        if (textFieldValue.text != currentExerciseCode) {
            textFieldValue = TextFieldValue(
                text = currentExerciseCode,
                selection = TextRange(currentExerciseCode.length)
            )
        }
    }

    // Undo/redo manager keyed to exercise so it resets on navigation
    val undoRedoManager = remember(uiState.currentExercise?.id) {
        UndoRedoManager().also { it.push(uiState.userCode) }
    }

    // Difficulty label and color
    val difficultyColor = when (exercise.difficulty.lowercase()) {
        "beginner", "easy" -> SuccessGreen
        "advanced", "hard" -> ErrorNeon
        else -> WarningAmber
    }
    val difficultyLabel = exercise.difficulty.replaceFirstChar { it.uppercase() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // -- Header: back + title + difficulty + reset --
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(difficultyColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = difficultyLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = difficultyColor
                        )
                    }
                }
            }
            IconButton(
                onClick = { viewModel.resetExercise() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset exercise",
                    tint = SecondaryText
                )
            }
        }

        // -- Scrollable content --
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
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Code editor
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

            // Quick-insert symbol toolbar with Undo/Redo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp))
                    .background(DarkSurfaceContainerHigh)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(4.dp))

                // Undo
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
                    modifier = Modifier.height(48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Undo",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (undoRedoManager.canUndo())
                            RustOrange
                        else
                            SecondaryText.copy(alpha = 0.4f)
                    )
                }

                // Redo
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
                    modifier = Modifier.height(48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Redo",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (undoRedoManager.canRedo())
                            RustOrange
                        else
                            SecondaryText.copy(alpha = 0.4f)
                    )
                }

                // Separator
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(SecondaryText.copy(alpha = 0.2f))
                )

                // Symbol buttons
                val symbols = listOf(
                    "{", "}", "(", ")", "[", "]", "<", ">",
                    ";", ":", "&", "*", "=", "\"", "'", "|",
                    "->", "::", "=>"
                )
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
                        modifier = Modifier.height(48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = symbol,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = SecondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -- Check result --
            uiState.checkResult?.let { result ->
                val resultBg = when (result) {
                    "correct" -> SuccessGreen.copy(alpha = 0.10f)
                    "uncertain" -> WarningAmber.copy(alpha = 0.10f)
                    else -> ErrorNeon.copy(alpha = 0.10f)
                }
                val resultBorder = when (result) {
                    "correct" -> SuccessGreen.copy(alpha = 0.30f)
                    "uncertain" -> WarningAmber.copy(alpha = 0.30f)
                    else -> ErrorNeon.copy(alpha = 0.30f)
                }
                val resultColor = when (result) {
                    "correct" -> SuccessGreen
                    "uncertain" -> WarningAmber
                    else -> ErrorNeon
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, resultBorder, RoundedCornerShape(12.dp))
                        .background(resultBg)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                when (result) {
                                    "correct" -> Icons.Default.CheckCircle
                                    else -> Icons.Default.Close
                                },
                                contentDescription = null,
                                tint = resultColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (result) {
                                    "correct" -> "Correct! Well done!"
                                    "uncertain" -> "Solution looks different -- it might still be correct!"
                                    else -> "Not quite right. Try again or reveal a hint."
                                },
                                color = resultColor,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // LLM fallback for uncertain results
                        if (result == "uncertain") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.validateWithLlm() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    enabled = !uiState.isValidating,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RustOrange
                                    )
                                ) {
                                    if (uiState.isValidating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Verify with AI",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (uiState.isValidating) "Verifying..." else "Verify with AI",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.markCurrentExerciseCorrect() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Mark correct",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Mark Correct",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // -- AI Validation section with primary left border --
            if (uiState.llmValidationResult.isNotEmpty() || uiState.isValidating) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    // Primary-colored left border (3dp)
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(RustOrange)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceContainerHigh)
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AI Review",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = RustOrange
                                )
                                if (uiState.isValidating) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = RustOrange
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.llmValidationResult.ifEmpty { "Analyzing your code..." },
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp,
                                color = SecondaryText
                            )
                            if (uiState.isValidating) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.stopValidation() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Stop",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // -- Action buttons: Check + Hint --
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.checkSolution() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RustOrange
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Check solution",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Check",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = { viewModel.revealHint() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = uiState.hintsRevealed < exercise.hints.size,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = "Reveal hint",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Hint (${uiState.hintsRevealed}/${exercise.hints.size})",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // -- Revealed hints --
            if (uiState.hintsRevealed > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Hints:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = WarningAmber
                )
                Spacer(modifier = Modifier.height(4.dp))
                for (i in 0 until uiState.hintsRevealed.coerceAtMost(exercise.hints.size)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Text(
                            text = "${i + 1}.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = WarningAmber,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            text = exercise.hints[i],
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // -- Solution reveal (collapsible) --
            if (uiState.hintsRevealed > 0 && !uiState.showSolution) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.showSolution() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ErrorNeon
                    )
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "Show solution",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Reveal Solution",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // -- Solution display --
            AnimatedVisibility(visible = uiState.showSolution) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Solution:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = RustOrange
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CodeBlock(code = exercise.solution, language = "rust")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = exercise.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        color = SecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -- Ask Sensei button --
            OutlinedButton(
                onClick = { onAskSensei(exercise.description, uiState.userCode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = "Ask Sensei for help",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Ask Sensei",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
