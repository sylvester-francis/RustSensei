package com.sylvester.rustsensei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.data.QuizQuestion
import com.sylvester.rustsensei.ui.theme.DarkSurfaceContainer
import com.sylvester.rustsensei.ui.theme.DarkSurfaceContainerHigh
import com.sylvester.rustsensei.ui.theme.DangerRed
import com.sylvester.rustsensei.ui.theme.ErrorNeon
import com.sylvester.rustsensei.ui.theme.RustOrange
import com.sylvester.rustsensei.ui.theme.SecondaryText
import com.sylvester.rustsensei.ui.theme.SuccessGreen
import com.sylvester.rustsensei.ui.theme.WarningAmber
import com.sylvester.rustsensei.viewmodel.QuizScreenMode
import com.sylvester.rustsensei.viewmodel.QuizViewModel

@Composable
fun QuizScreen(viewModel: QuizViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.mode != QuizScreenMode.LIST) {
        viewModel.returnToList()
    }

    when (uiState.mode) {
        QuizScreenMode.LIST -> QuizListView(viewModel)
        QuizScreenMode.IN_PROGRESS -> QuizInProgressView(viewModel)
        QuizScreenMode.COMPLETE -> QuizCompleteView(viewModel)
    }
}

// ---- QUIZ LIST VIEW ---------------------------------------------------------

@Composable
private fun QuizListView(viewModel: QuizViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Topic Quizzes",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Test your knowledge after each chapter",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        items(uiState.quizzes, key = { it.id }) { quiz ->
            val bestResult = uiState.bestScores[quiz.id]
            val percent = bestResult?.let {
                (it.score.toFloat() / it.totalQuestions * 100).toInt()
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceContainer)
                    .clickable { viewModel.startQuiz(quiz.id) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (percent != null) scoreColor(percent).copy(alpha = 0.12f)
                            else RustOrange.copy(alpha = 0.10f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (percent != null) {
                        Text(
                            text = "$percent%",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor(percent)
                        )
                    } else {
                        Text(
                            text = "?",
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = RustOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quiz.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${quiz.questionCount}q",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = SecondaryText
                        )
                        if (bestResult != null) {
                            Text(
                                text = "  \u00B7  best ${bestResult.score}/${bestResult.totalQuestions}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = scoreColor(percent!!)
                            )
                        }
                    }
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = SecondaryText.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ---- IN-PROGRESS VIEW -------------------------------------------------------

@Composable
private fun QuizInProgressView(viewModel: QuizViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val quiz = uiState.currentQuiz ?: return
    val question = quiz.questions.getOrNull(uiState.currentQuestionIndex) ?: return
    val total = quiz.questions.size
    val current = uiState.currentQuestionIndex + 1
    val progress = current.toFloat() / total

    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // -- Header: [X close] Quiz title  Q 3 of 10 --
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.returnToList() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close quiz",
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = quiz.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            // Question counter pill
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RustOrange.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Q $current of $total",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = RustOrange
                )
            }
        }

        // -- Progress bar (4dp, animated) --
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = RustOrange,
            trackColor = DarkSurfaceContainerHigh,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(20.dp))

        // -- Question content with animated transitions (slide + fade) --
        AnimatedContent(
            targetState = uiState.currentQuestionIndex,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn())
                    .togetherWith(slideOutHorizontally { -it } + fadeOut())
            },
            label = "question-transition",
            modifier = Modifier.weight(1f)
        ) { _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // Question type badge
                val typeLabel = when (question) {
                    is QuizQuestion.MultipleChoice -> "MULTIPLE CHOICE"
                    is QuizQuestion.TrueFalse -> "TRUE / FALSE"
                    is QuizQuestion.CodeCompletion -> "CODE"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceContainerHigh)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        color = SecondaryText
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Question text
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Answer options
                when (question) {
                    is QuizQuestion.MultipleChoice -> MultipleChoiceAnswers(
                        question = question,
                        selectedIndex = uiState.userAnswers[uiState.currentQuestionIndex] as? Int,
                        answered = uiState.answeredCurrent,
                        onSelect = { viewModel.answerMultipleChoice(it) }
                    )
                    is QuizQuestion.TrueFalse -> TrueFalseAnswers(
                        question = question,
                        selectedAnswer = uiState.userAnswers[uiState.currentQuestionIndex] as? Boolean,
                        answered = uiState.answeredCurrent,
                        onSelect = { viewModel.answerTrueFalse(it) }
                    )
                    is QuizQuestion.CodeCompletion -> CodeCompletionAnswer(
                        question = question,
                        codeInput = uiState.codeInput,
                        answered = uiState.answeredCurrent,
                        isCorrect = uiState.currentAnswerCorrect,
                        onCodeChange = { viewModel.updateCodeInput(it) },
                        onSubmit = { viewModel.submitCodeAnswer() }
                    )
                }

                // -- Feedback panel (shown after answering) --
                if (uiState.answeredCurrent) {
                    Spacer(modifier = Modifier.height(20.dp))

                    val isCorrect = uiState.currentAnswerCorrect
                    val accentColor = if (isCorrect) SuccessGreen else ErrorNeon

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        // Colored top border (3dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(accentColor)
                        )
                        // Panel content
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(accentColor.copy(alpha = 0.08f))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isCorrect) Icons.Default.CheckCircle
                                        else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isCorrect) "Correct!" else "Incorrect",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = question.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // -- Bottom action bar --
        if (uiState.answeredCurrent) {
            val isLast = uiState.currentQuestionIndex == quiz.questions.size - 1
            Button(
                onClick = { viewModel.nextQuestion() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RustOrange
                )
            ) {
                Text(
                    text = if (isLast) "See Results" else "Next",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ---- ANSWER COMPOSABLES -----------------------------------------------------

@Composable
private fun MultipleChoiceAnswers(
    question: QuizQuestion.MultipleChoice,
    selectedIndex: Int?,
    answered: Boolean,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        question.options.forEachIndexed { index, option ->
            val isSelected = selectedIndex == index
            val isCorrect = index == question.correctIndex

            val borderColor = when {
                !answered && isSelected -> RustOrange
                !answered -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                isCorrect -> SuccessGreen
                isSelected -> ErrorNeon
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            }
            val borderWidth = when {
                !answered && isSelected -> 2.dp
                answered && (isCorrect || isSelected) -> 2.dp
                else -> 1.dp
            }
            val bgColor = when {
                !answered && isSelected -> RustOrange.copy(alpha = 0.08f)
                !answered -> DarkSurfaceContainer
                answered && isCorrect -> SuccessGreen.copy(alpha = 0.08f)
                answered && isSelected -> ErrorNeon.copy(alpha = 0.08f)
                else -> DarkSurfaceContainer
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable(enabled = !answered) { onSelect(index) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Letter badge in circle
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(borderColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${'A' + index}",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = borderColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (answered && isCorrect) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (answered && isSelected && !isCorrect) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = ErrorNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrueFalseAnswers(
    question: QuizQuestion.TrueFalse,
    selectedAnswer: Boolean?,
    answered: Boolean,
    onSelect: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(true, false).forEach { value ->
            val isSelected = selectedAnswer == value
            val isCorrect = value == question.correctAnswer

            val borderColor = when {
                !answered && isSelected -> RustOrange
                !answered -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                isCorrect -> SuccessGreen
                isSelected -> ErrorNeon
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            }
            val borderWidth = when {
                !answered && isSelected -> 2.dp
                answered && (isCorrect || isSelected) -> 2.dp
                else -> 1.dp
            }
            val bgColor = when {
                !answered && isSelected -> RustOrange.copy(alpha = 0.08f)
                !answered -> DarkSurfaceContainer
                answered && isCorrect -> SuccessGreen.copy(alpha = 0.08f)
                answered && isSelected -> ErrorNeon.copy(alpha = 0.08f)
                else -> DarkSurfaceContainer
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable(enabled = !answered) { onSelect(value) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (value) "True" else "False",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = borderColor
                    )
                    if (answered) {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isCorrect) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (isSelected) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = ErrorNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeCompletionAnswer(
    question: QuizQuestion.CodeCompletion,
    codeInput: String,
    answered: Boolean,
    isCorrect: Boolean,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column {
        // Code block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF06080C))
                .padding(14.dp)
        ) {
            Text(
                text = question.code,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFE8ECF2),
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = codeInput,
            onValueChange = { if (!answered) onCodeChange(it) },
            label = { Text("Fill in the blank") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !answered,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RustOrange,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedLabelColor = RustOrange,
                cursorColor = RustOrange
            )
        )

        if (answered && !isCorrect) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Answer: ${question.correctAnswer}",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = SuccessGreen
            )
        }

        if (!answered) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = codeInput.isNotBlank(),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RustOrange
                )
            ) {
                Text(
                    "Check",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ---- COMPLETE VIEW ----------------------------------------------------------

@Composable
private fun QuizCompleteView(viewModel: QuizViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val quiz = uiState.currentQuiz ?: return
    val score = uiState.score
    val total = quiz.questions.size
    val percent = (score.toFloat() / total * 100).toInt()
    val color = scoreColor(percent)

    val message = when {
        percent == 100 -> "Perfect score!"
        percent >= 80 -> "Great job!"
        percent >= 60 -> "Good effort!"
        percent >= 40 -> "Keep practicing!"
        else -> "Review the material and try again"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Trophy icon in circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = "Trophy",
                tint = color,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = quiz.title,
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Score display -- large monospace
        Text(
            text = "$score / $total",
            style = MaterialTheme.typography.headlineLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = color
        )

        Text(
            text = "$percent%",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            color = color.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { score.toFloat() / total },
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = DarkSurfaceContainerHigh,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.weight(1f))

        // Retry button
        Button(
            onClick = { viewModel.startQuiz(quiz.id) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RustOrange
            )
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Retry",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Done button
        OutlinedButton(
            onClick = { viewModel.returnToList() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(
                "Done",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ---- HELPERS ----------------------------------------------------------------

private fun scoreColor(percent: Int): Color = when {
    percent >= 80 -> SuccessGreen
    percent >= 50 -> WarningAmber
    else -> DangerRed
}
