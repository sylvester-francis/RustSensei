package com.sylvester.rustsensei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.data.QuizQuestion
import com.sylvester.rustsensei.ui.theme.DarkSurfaceContainer
import com.sylvester.rustsensei.ui.theme.DarkSurfaceContainerHigh
import com.sylvester.rustsensei.ui.theme.ErrorNeon
import com.sylvester.rustsensei.ui.theme.NeonCyan
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

// ═══════════════════════════════════════════════════════════════════════
// QUIZ LIST
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun QuizListView(viewModel: QuizViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Quizzes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Test your understanding of each topic",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
        }

        if (uiState.quizzes.isEmpty()) {
            item(key = "empty-quizzes") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = SecondaryText.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No quizzes available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Quizzes couldn't be loaded",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText.copy(alpha = 0.4f)
                    )
                }
            }
        }

        items(uiState.quizzes, key = { it.id }) { quiz ->
            val bestResult = uiState.bestScores[quiz.id]
            val percent = bestResult?.let {
                (it.score.toFloat() / it.totalQuestions * 100).toInt()
            }
            val isPassed = percent != null && percent >= 70

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.startQuiz(quiz.id) },
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Score ring
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (percent != null) {
                            val ringColor = scoreColor(percent)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 5.dp.toPx()
                                val radius = (size.minDimension - strokeWidth) / 2
                                val topLeft = Offset(
                                    (size.width - radius * 2) / 2,
                                    (size.height - radius * 2) / 2
                                )
                                // Track
                                drawArc(
                                    color = ringColor.copy(alpha = 0.15f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(radius * 2, radius * 2),
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )
                                // Fill
                                drawArc(
                                    color = ringColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * percent / 100f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(radius * 2, radius * 2),
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "$percent%",
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor(percent)
                            )
                        } else {
                            // Untaken — show play icon
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(RustOrange.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Start quiz",
                                    tint = RustOrange,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = quiz.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isPassed) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Passed",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Question count pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkSurfaceContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${quiz.questionCount} questions",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = SecondaryText
                                )
                            }
                            if (bestResult != null) {
                                Text(
                                    text = "Best: ${bestResult.score}/${bestResult.totalQuestions}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = scoreColor(percent ?: 0).copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// IN-PROGRESS
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun QuizInProgressView(viewModel: QuizViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val quiz = uiState.currentQuiz ?: return
    val question = quiz.questions.getOrNull(uiState.currentQuestionIndex) ?: return
    val total = quiz.questions.size
    val current = uiState.currentQuestionIndex + 1
    val progress = current.toFloat() / total

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Top bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.returnToList() }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close quiz",
                    tint = SecondaryText
                )
            }

            // Progress bar in the center
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = RustOrange,
                trackColor = DarkSurfaceContainerHigh,
                strokeCap = StrokeCap.Round
            )

            // Counter
            Text(
                text = "$current/$total",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = SecondaryText,
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        // ── Question content ──
        AnimatedContent(
            targetState = uiState.currentQuestionIndex,
            transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn())
                    .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut())
            },
            label = "question",
            modifier = Modifier.weight(1f)
        ) { _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Question type badge
                val typeLabel = when (question) {
                    is QuizQuestion.MultipleChoice -> "MULTIPLE CHOICE"
                    is QuizQuestion.TrueFalse -> "TRUE / FALSE"
                    is QuizQuestion.CodeCompletion -> "CODE COMPLETION"
                }
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                    color = RustOrange.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Question text
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Answers
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

                // ── Feedback ──
                if (uiState.answeredCurrent) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val isCorrect = uiState.currentAnswerCorrect
                    val accentColor = if (isCorrect) SuccessGreen else ErrorNeon

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = accentColor.copy(alpha = 0.08f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, accentColor.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isCorrect) Icons.Default.CheckCircle
                                        else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isCorrect) "Correct!" else "Not quite",
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
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ── Bottom button ──
        if (uiState.answeredCurrent) {
            val isLast = uiState.currentQuestionIndex == quiz.questions.size - 1
            Button(
                onClick = { viewModel.nextQuestion() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = RustOrange)
            ) {
                Text(
                    text = if (isLast) "See Results" else "Continue",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// ANSWER COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════

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
                !answered -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                isCorrect -> SuccessGreen
                isSelected -> ErrorNeon
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            }
            val bgColor = when {
                !answered && isSelected -> RustOrange.copy(alpha = 0.10f)
                answered && isCorrect -> SuccessGreen.copy(alpha = 0.08f)
                answered && isSelected -> ErrorNeon.copy(alpha = 0.08f)
                else -> DarkSurfaceContainer
            }
            val borderW = if ((answered && (isCorrect || isSelected)) || (!answered && isSelected)) 2.dp else 1.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(borderW, borderColor, RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable(enabled = !answered) { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(borderColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${'A' + index}",
                        fontSize = 14.sp,
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
                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                } else if (answered && isSelected && !isCorrect) {
                    Icon(Icons.Default.Close, null, tint = ErrorNeon, modifier = Modifier.size(20.dp))
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
                !answered -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                isCorrect -> SuccessGreen
                isSelected -> ErrorNeon
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            }
            val bgColor = when {
                !answered && isSelected -> RustOrange.copy(alpha = 0.10f)
                answered && isCorrect -> SuccessGreen.copy(alpha = 0.08f)
                answered && isSelected -> ErrorNeon.copy(alpha = 0.08f)
                else -> DarkSurfaceContainer
            }
            val borderW = if ((answered && (isCorrect || isSelected)) || (!answered && isSelected)) 2.dp else 1.dp

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.8f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(borderW, borderColor, RoundedCornerShape(14.dp))
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
                            Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        } else if (isSelected) {
                            Icon(Icons.Default.Close, null, tint = ErrorNeon, modifier = Modifier.size(18.dp))
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A0E14))
                .padding(16.dp)
        ) {
            Text(
                text = question.code,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFE8ECF2),
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = codeInput,
            onValueChange = { if (!answered) onCodeChange(it) },
            label = { Text("Your answer") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !answered,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RustOrange,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                focusedLabelColor = RustOrange,
                cursorColor = RustOrange
            )
        )

        if (answered && !isCorrect) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Correct answer: ", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                Text(
                    text = question.correctAnswer,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen
                )
            }
        }

        if (!answered) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = codeInput.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RustOrange)
            ) {
                Text("Submit", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// RESULTS
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun QuizCompleteView(viewModel: QuizViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val quiz = uiState.currentQuiz ?: return
    val score = uiState.score
    val total = quiz.questions.size
    val percent = (score.toFloat() / total * 100).toInt()
    val color = scoreColor(percent)

    val message = when {
        percent == 100 -> "Perfect!"
        percent >= 80 -> "Great job!"
        percent >= 60 -> "Good effort!"
        percent >= 40 -> "Keep practicing!"
        else -> "Review and try again"
    }

    val emoji = when {
        percent == 100 -> "\uD83C\uDF1F"  // star
        percent >= 80 -> "\uD83C\uDF89"   // party
        percent >= 60 -> "\uD83D\uDCAA"   // muscle
        percent >= 40 -> "\uD83D\uDCDA"   // books
        else -> "\uD83E\uDD14"            // thinking
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.15f))

        // Emoji
        Text(text = emoji, fontSize = 56.sp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = quiz.title,
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Score ring — large
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 10.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
                drawArc(
                    color = color.copy(alpha = 0.15f),
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = Size(radius * 2, radius * 2),
                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = color,
                    startAngle = -90f, sweepAngle = 360f * percent / 100f, useCenter = false,
                    topLeft = topLeft, size = Size(radius * 2, radius * 2),
                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score/$total",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.3f))

        // Buttons
        Button(
            onClick = { viewModel.startQuiz(quiz.id) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(containerColor = RustOrange)
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Retry Quiz", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { viewModel.returnToList() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Text("Back to Quizzes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════

private fun scoreColor(percent: Int): Color = when {
    percent >= 80 -> SuccessGreen
    percent >= 50 -> WarningAmber
    else -> ErrorNeon
}
