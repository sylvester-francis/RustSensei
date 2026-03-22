package com.sylvester.rustsensei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.R
import com.sylvester.rustsensei.ui.components.CodeBlock
import com.sylvester.rustsensei.ui.components.CodeEditor
import com.sylvester.rustsensei.ui.components.UndoRedoManager
import com.sylvester.rustsensei.ui.theme.Alpha
import com.sylvester.rustsensei.ui.theme.DarkSurfaceContainerHigh
import com.sylvester.rustsensei.ui.theme.Dimens
import com.sylvester.rustsensei.ui.theme.DifficultyBeginner
import com.sylvester.rustsensei.ui.theme.DifficultyIntermediate
import com.sylvester.rustsensei.ui.theme.ErrorNeon
import com.sylvester.rustsensei.ui.theme.RustOrange
import com.sylvester.rustsensei.ui.theme.SecondaryText
import com.sylvester.rustsensei.ui.theme.Spacing
import com.sylvester.rustsensei.ui.theme.SuccessGreen
import com.sylvester.rustsensei.ui.theme.WarningAmber
import com.sylvester.rustsensei.viewmodel.RefactoringViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefactoringScreen(
    viewModel: RefactoringViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.currentChallenge != null) {
        viewModel.navigateBack()
    }

    if (uiState.currentChallenge != null) {
        RefactoringDetailView(
            viewModel = viewModel
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.refactoring_title),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            RefactoringListView(
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun RefactoringListView(
    viewModel: RefactoringViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = RustOrange,
                modifier = Modifier.size(Dimens.IconLG)
            )
        }
        return
    }

    if (uiState.challenges.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(Spacing.Section),
                    tint = SecondaryText.copy(alpha = Alpha.MUTED)
                )
                Spacer(modifier = Modifier.height(Spacing.LG))
                Text(
                    text = stringResource(R.string.refactoring_no_challenges),
                    style = MaterialTheme.typography.bodyLarge,
                    color = SecondaryText
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.ScreenPadding)
    ) {
        item {
            Text(
                text = stringResource(R.string.refactoring_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                modifier = Modifier.padding(bottom = Spacing.LG)
            )
        }

        items(uiState.challenges, key = { it.id }) { challenge ->
            val bestScore = uiState.bestScores[challenge.id]
            val difficultyColor = when (challenge.difficulty.lowercase()) {
                "beginner" -> DifficultyBeginner
                else -> DifficultyIntermediate
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.MD)
                    .clickable { viewModel.openChallenge(challenge.id) },
                shape = RoundedCornerShape(Dimens.CardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.CardPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = challenge.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.XS))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.SM)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(Spacing.XS))
                                    .background(difficultyColor.copy(alpha = Alpha.BORDER))
                                    .padding(
                                        horizontal = Spacing.SM,
                                        vertical = Spacing.XXS
                                    )
                            ) {
                                Text(
                                    text = challenge.difficulty.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = difficultyColor
                                )
                            }
                            if (bestScore != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = scoreColor(bestScore),
                                        modifier = Modifier.size(Dimens.IconSM)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.XXS))
                                    Text(
                                        text = "$bestScore/100",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = scoreColor(bestScore)
                                    )
                                }
                            }
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = SecondaryText.copy(alpha = Alpha.SECONDARY),
                        modifier = Modifier.size(Dimens.IconSM)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(Spacing.LG)) }
    }
}

@Composable
private fun RefactoringDetailView(
    viewModel: RefactoringViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val challenge = uiState.currentChallenge ?: return

    val difficultyColor = when (challenge.difficulty.lowercase()) {
        "beginner" -> DifficultyBeginner
        else -> DifficultyIntermediate
    }

    var textFieldValue by remember(challenge.id) {
        mutableStateOf(
            TextFieldValue(
                text = uiState.userCode,
                selection = TextRange(uiState.userCode.length)
            )
        )
    }

    val undoRedoManager = remember(challenge.id) {
        UndoRedoManager().also { it.push(uiState.userCode) }
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
                    .padding(horizontal = Spacing.XS, vertical = Spacing.XS),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateBack() },
                    modifier = Modifier.size(Dimens.CompactTopBarHeight)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Spacing.XS))
                            .background(difficultyColor.copy(alpha = Alpha.BORDER))
                            .padding(horizontal = Spacing.SM, vertical = Spacing.XXS)
                    ) {
                        Text(
                            text = challenge.difficulty.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = difficultyColor
                        )
                    }
                }
                IconButton(
                    onClick = { viewModel.resetChallenge() },
                    modifier = Modifier.size(Dimens.CompactTopBarHeight)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = SecondaryText
                    )
                }
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenPadding)
            ) {
                // Description
                Spacer(modifier = Modifier.height(Spacing.SM))
                Text(
                    text = challenge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Original code
                Spacer(modifier = Modifier.height(Spacing.LG))
                Text(
                    text = stringResource(R.string.refactoring_original_code),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = RustOrange,
                    modifier = Modifier.padding(bottom = Spacing.SM)
                )
                CodeBlock(code = challenge.uglyCode, language = "rust")

                // User code editor
                Spacer(modifier = Modifier.height(Spacing.LG))
                Text(
                    text = stringResource(R.string.refactoring_your_version),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = RustOrange,
                    modifier = Modifier.padding(bottom = Spacing.SM)
                )
                CodeEditor(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        undoRedoManager.push(newValue.text)
                        viewModel.updateCode(newValue.text)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minHeight = Dimens.CompactTopBarHeight * 5
                )

                // Symbol toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.CompactTopBarHeight)
                        .clip(
                            RoundedCornerShape(
                                bottomStart = Dimens.CardRadius,
                                bottomEnd = Dimens.CardRadius
                            )
                        )
                        .background(DarkSurfaceContainerHigh)
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
                        contentPadding = PaddingValues(
                            horizontal = Spacing.SM,
                            vertical = Spacing.XXS
                        )
                    ) {
                        Text(
                            text = "Undo",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (undoRedoManager.canUndo())
                                RustOrange
                            else
                                SecondaryText.copy(alpha = Alpha.HINT)
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
                        contentPadding = PaddingValues(
                            horizontal = Spacing.SM,
                            vertical = Spacing.XXS
                        )
                    ) {
                        Text(
                            text = "Redo",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (undoRedoManager.canRedo())
                                RustOrange
                            else
                                SecondaryText.copy(alpha = Alpha.HINT)
                        )
                    }

                    // Separator
                    Box(
                        modifier = Modifier
                            .height(Spacing.XXL)
                            .width(Dimens.Divider)
                            .background(SecondaryText.copy(alpha = Alpha.DIVIDER))
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
                            contentPadding = PaddingValues(
                                horizontal = Spacing.SM,
                                vertical = Spacing.XXS
                            )
                        ) {
                            Text(
                                text = symbol,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = SecondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(Spacing.XS))
                }

                Spacer(modifier = Modifier.height(Spacing.LG))

                // Score display
                uiState.score?.let { score ->
                    val scoreBg = when {
                        score >= 80 -> SuccessGreen.copy(alpha = Alpha.BORDER)
                        score >= 50 -> WarningAmber.copy(alpha = Alpha.BORDER)
                        else -> ErrorNeon.copy(alpha = Alpha.BORDER)
                    }
                    val scoreBorder = when {
                        score >= 80 -> SuccessGreen.copy(alpha = Alpha.MUTED)
                        score >= 50 -> WarningAmber.copy(alpha = Alpha.MUTED)
                        else -> ErrorNeon.copy(alpha = Alpha.MUTED)
                    }
                    val scoreTextColor = when {
                        score >= 80 -> SuccessGreen
                        score >= 50 -> WarningAmber
                        else -> ErrorNeon
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.CardRadius))
                            .border(
                                Dimens.Divider,
                                scoreBorder,
                                RoundedCornerShape(Dimens.CardRadius)
                            )
                            .background(scoreBg)
                            .padding(Dimens.CardPadding)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = scoreTextColor,
                                modifier = Modifier.size(Dimens.IconMD)
                            )
                            Spacer(modifier = Modifier.width(Spacing.SM))
                            Column {
                                Text(
                                    text = "${stringResource(R.string.refactoring_score_label)}: $score/100",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = scoreTextColor
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.MD))
                }

                // AI feedback section
                if (uiState.feedback.isNotEmpty() || uiState.isValidating) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .clip(RoundedCornerShape(Dimens.CardRadius))
                    ) {
                        Box(
                            modifier = Modifier
                                .width(Spacing.XXS + Dimens.Divider)
                                .fillMaxHeight()
                                .background(RustOrange)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceContainerHigh)
                                .padding(Dimens.CardPadding)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.refactoring_score_label),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = RustOrange
                                    )
                                    if (uiState.isValidating) {
                                        Spacer(modifier = Modifier.width(Spacing.SM))
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(Spacing.LG),
                                            strokeWidth = Spacing.XXS,
                                            color = RustOrange
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(Spacing.SM))
                                Text(
                                    text = uiState.feedback.ifEmpty {
                                        stringResource(R.string.refactoring_scoring)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 20.sp,
                                    color = SecondaryText
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
                                            stringResource(R.string.stop_generation),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.MD))
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.MD)
                ) {
                    Button(
                        onClick = { viewModel.submitForScoring() },
                        modifier = Modifier
                            .weight(1f)
                            .height(Dimens.CompactTopBarHeight),
                        enabled = !uiState.isValidating,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RustOrange
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
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.IconSM)
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.SM))
                        Text(
                            if (uiState.isValidating)
                                stringResource(R.string.refactoring_scoring)
                            else
                                stringResource(R.string.refactoring_submit),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.revealHint() },
                        modifier = Modifier
                            .weight(1f)
                            .height(Dimens.CompactTopBarHeight),
                        enabled = uiState.hintsRevealed < challenge.hints.size,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.IconSM)
                        )
                        Spacer(modifier = Modifier.width(Spacing.SM))
                        Text(
                            "Hint (${uiState.hintsRevealed}/${challenge.hints.size})",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Revealed hints
                if (uiState.hintsRevealed > 0) {
                    Spacer(modifier = Modifier.height(Spacing.LG))
                    Text(
                        text = "Hints:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber
                    )
                    Spacer(modifier = Modifier.height(Spacing.XS))
                    for (i in 0 until uiState.hintsRevealed.coerceAtMost(challenge.hints.size)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.XXS)
                        ) {
                            Text(
                                text = "${i + 1}.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = WarningAmber,
                                modifier = Modifier.width(Spacing.XXL)
                            )
                            Text(
                                text = challenge.hints[i],
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryText,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // Show solution button
                if (uiState.hintsRevealed > 0 && !uiState.showSolution) {
                    Spacer(modifier = Modifier.height(Spacing.MD))
                    OutlinedButton(
                        onClick = { viewModel.showSolution() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.CompactTopBarHeight),
                        shape = RoundedCornerShape(Dimens.CardRadius),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ErrorNeon
                        )
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.IconSM)
                        )
                        Spacer(modifier = Modifier.width(Spacing.SM))
                        Text(
                            stringResource(R.string.refactoring_show_solution),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Solution display
                AnimatedVisibility(visible = uiState.showSolution) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        Spacer(modifier = Modifier.height(Spacing.LG))
                        Text(
                            text = "Idiomatic Solution:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = RustOrange
                        )
                        Spacer(modifier = Modifier.height(Spacing.SM))
                        CodeBlock(
                            code = challenge.idiomaticSolution,
                            language = "rust"
                        )
                    }
                }

                // Error message
                uiState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(Spacing.MD))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.CardRadius))
                            .background(ErrorNeon.copy(alpha = Alpha.BORDER))
                            .padding(Dimens.CardPadding)
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErrorNeon
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.XXL))
            }
        }
    }
}

@Composable
private fun scoreColor(score: Int) = when {
    score >= 80 -> SuccessGreen
    score >= 50 -> WarningAmber
    else -> ErrorNeon
}
