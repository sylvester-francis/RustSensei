package com.sylvester.rustsensei.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.ui.components.CodeBlock
import com.sylvester.rustsensei.ui.components.CodeEditor
import com.sylvester.rustsensei.ui.components.ConfettiOverlay
import com.sylvester.rustsensei.ui.components.UndoRedoManager
import com.sylvester.rustsensei.ui.theme.Alpha
import com.sylvester.rustsensei.ui.theme.DarkSurfaceContainerHigh
import com.sylvester.rustsensei.ui.theme.Dimens
import com.sylvester.rustsensei.ui.theme.DifficultyAdvanced
import com.sylvester.rustsensei.ui.theme.DifficultyBeginner
import com.sylvester.rustsensei.ui.theme.DifficultyIntermediate
import com.sylvester.rustsensei.ui.theme.SecondaryText
import com.sylvester.rustsensei.ui.theme.Spacing
import com.sylvester.rustsensei.viewmodel.ExerciseViewModel
import com.sylvester.rustsensei.ui.theme.AppColors

// ---- EXERCISE DETAIL VIEW ---------------------------------------------------

@Composable
internal fun ExerciseDetailView(
    viewModel: ExerciseViewModel,
    onAskSensei: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val exercise = uiState.currentExercise ?: return

    var showConfetti by remember { mutableStateOf(false) }

    // Trigger confetti when check result becomes "correct"
    LaunchedEffect(uiState.checkResult) {
        if (uiState.checkResult == "correct") {
            showConfetti = true
        }
    }

    // Reset confetti when the exercise changes
    LaunchedEffect(uiState.currentExercise) {
        showConfetti = false
    }

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

    // Difficulty label and color from the exercise data's difficulty field
    val difficultyColor = when (exercise.difficulty.lowercase()) {
        "beginner", "easy" -> DifficultyBeginner
        "advanced", "hard" -> DifficultyAdvanced
        else -> DifficultyIntermediate
    }
    val difficultyLabel = exercise.difficulty.replaceFirstChar { it.uppercase() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
        // -- Header: back + title + difficulty + reset --
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.XS, vertical = Spacing.XS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.size(Dimens.CompactTopBarHeight)
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
                            .background(difficultyColor.copy(alpha = Alpha.BORDER))
                            .padding(horizontal = Spacing.SM, vertical = Spacing.XXS)
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
                modifier = Modifier.size(Dimens.CompactTopBarHeight)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset exercise",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // -- Scrollable content --
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding)
        ) {
            // Instructions
            Spacer(modifier = Modifier.height(Spacing.SM))
            Text(
                text = exercise.description,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.SM))
            Text(
                text = exercise.instructions,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.LG))

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
                    .height(Dimens.CompactTopBarHeight)
                    .clip(RoundedCornerShape(0.dp, 0.dp, Dimens.CardRadius, Dimens.CardRadius))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.XXS),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(Spacing.XS))

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
                    modifier = Modifier.height(Dimens.CompactTopBarHeight),
                    contentPadding = PaddingValues(horizontal = Spacing.SM, vertical = 0.dp)
                ) {
                    Text(
                        text = "Undo",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (undoRedoManager.canUndo())
                            AppColors.current.accent
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
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
                    modifier = Modifier.height(Dimens.CompactTopBarHeight),
                    contentPadding = PaddingValues(horizontal = Spacing.SM, vertical = 0.dp)
                ) {
                    Text(
                        text = "Redo",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (undoRedoManager.canRedo())
                            AppColors.current.accent
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                // Separator
                Box(
                    modifier = Modifier
                        .height(Spacing.XXL)
                        .width(Dimens.Divider)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.DIVIDER))
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
                        modifier = Modifier.height(Dimens.CompactTopBarHeight),
                        contentPadding = PaddingValues(horizontal = Spacing.SM, vertical = 0.dp)
                    ) {
                        Text(
                            text = symbol,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Spacing.XS))
            }

            Spacer(modifier = Modifier.height(Spacing.LG))

            // -- Check result --
            uiState.checkResult?.let { result ->
                val resultBg = when (result) {
                    "correct" -> AppColors.current.success.copy(alpha = 0.10f)
                    "uncertain" -> AppColors.current.amber.copy(alpha = 0.10f)
                    else -> AppColors.current.error.copy(alpha = 0.10f)
                }
                val resultBorder = when (result) {
                    "correct" -> AppColors.current.success.copy(alpha = Alpha.MUTED)
                    "uncertain" -> AppColors.current.amber.copy(alpha = Alpha.MUTED)
                    else -> AppColors.current.error.copy(alpha = Alpha.MUTED)
                }
                val resultColor = when (result) {
                    "correct" -> AppColors.current.success
                    "uncertain" -> AppColors.current.amber
                    else -> AppColors.current.error
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.CardRadius))
                        .border(Dimens.Divider, resultBorder, RoundedCornerShape(Dimens.CardRadius))
                        .background(resultBg)
                        .padding(Dimens.CardPadding)
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
                                modifier = Modifier.size(Dimens.IconSM)
                            )
                            Spacer(modifier = Modifier.width(Spacing.SM))
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
                            Spacer(modifier = Modifier.height(Spacing.MD))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.SM)
                            ) {
                                Button(
                                    onClick = { viewModel.validateWithLlm() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(Dimens.CompactTopBarHeight),
                                    enabled = !uiState.isValidating,
                                    shape = RoundedCornerShape(Dimens.CardRadius),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.current.accent
                                    )
                                ) {
                                    if (uiState.isValidating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(Spacing.LG),
                                            strokeWidth = Spacing.XXS,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Verify with AI",
                                            modifier = Modifier.size(Spacing.LG)
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
                                        .height(Dimens.CompactTopBarHeight),
                                    shape = RoundedCornerShape(Dimens.CardRadius)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Mark correct",
                                        modifier = Modifier.size(Spacing.LG)
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
                Spacer(modifier = Modifier.height(Spacing.MD))
            }

            // -- AI Validation section with primary left border --
            if (uiState.llmValidationResult.isNotEmpty() || uiState.isValidating) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .clip(RoundedCornerShape(Dimens.CardRadius))
                ) {
                    // Primary-colored left border (3dp)
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(AppColors.current.accent)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(Dimens.CardPadding)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AI Review",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.current.accent
                                )
                                if (uiState.isValidating) {
                                    Spacer(modifier = Modifier.width(Spacing.SM))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = Spacing.XXS,
                                        color = AppColors.current.accent
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(Spacing.SM))
                            Text(
                                text = uiState.llmValidationResult.ifEmpty { "Analyzing your code..." },
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.isValidating) {
                                Spacer(modifier = Modifier.height(Spacing.SM))
                                OutlinedButton(
                                    onClick = { viewModel.stopValidation() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(Dimens.CompactTopBarHeight),
                                    shape = RoundedCornerShape(Dimens.CardRadius)
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
                Spacer(modifier = Modifier.height(Spacing.MD))
            }

            // -- Action buttons: Check + Hint --
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.MD)
            ) {
                Button(
                    onClick = { viewModel.checkSolution() },
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.CompactTopBarHeight),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.current.accent
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
                        .height(Dimens.CompactTopBarHeight),
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
                Spacer(modifier = Modifier.height(Spacing.LG))
                Text(
                    text = "Hints:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.current.amber
                )
                Spacer(modifier = Modifier.height(Spacing.XS))
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
                            color = AppColors.current.amber,
                            modifier = Modifier.width(Spacing.XXL)
                        )
                        Text(
                            text = exercise.hints[i],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // -- Solution reveal (collapsible) --
            if (uiState.hintsRevealed > 0 && !uiState.showSolution) {
                Spacer(modifier = Modifier.height(Spacing.MD))
                OutlinedButton(
                    onClick = { viewModel.showSolution() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.CompactTopBarHeight),
                    shape = RoundedCornerShape(Dimens.CardRadius),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.current.error
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
                    Spacer(modifier = Modifier.height(Spacing.LG))
                    Text(
                        text = "Solution:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.current.accent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CodeBlock(code = exercise.solution, language = "rust")
                    Spacer(modifier = Modifier.height(Spacing.SM))
                    Text(
                        text = exercise.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.LG))

            // -- Ask Sensei button --
            OutlinedButton(
                onClick = { onAskSensei(exercise.description, uiState.userCode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.CompactTopBarHeight),
                shape = RoundedCornerShape(Dimens.CardRadius)
            ) {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = "Ask Sensei for help",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.SM))
                Text(
                    "Ask Sensei",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.XXL))
        }
    }

    ConfettiOverlay(
        isVisible = showConfetti,
        onComplete = { showConfetti = false }
    )
    }
}
