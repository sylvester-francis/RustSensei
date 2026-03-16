package com.sylvester.rustsensei.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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

@Composable
private fun QuizListView(viewModel: QuizViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Quizzes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Test your knowledge after each topic",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(uiState.quizzes, key = { it.id }) { quiz ->
            val bestResult = uiState.bestScores[quiz.id]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { viewModel.startQuiz(quiz.id) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = quiz.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${quiz.questionCount} questions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (bestResult != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val percent = (bestResult.score.toFloat() / bestResult.totalQuestions * 100).toInt()
                            val scoreColor = when {
                                percent >= 80 -> Color(0xFF3FB950)
                                percent >= 50 -> Color(0xFFF0883E)
                                else -> Color(0xFFF85149)
                            }
                            Text(
                                text = "Best: ${bestResult.score}/${bestResult.totalQuestions} ($percent%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = scoreColor
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuizInProgressView(viewModel: QuizViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val quiz = uiState.currentQuiz ?: return
    val question = quiz.questions[uiState.currentQuestionIndex]
    val progress = (uiState.currentQuestionIndex + 1).toFloat() / quiz.questions.size

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Header with progress
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.returnToList() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quiz.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Question ${uiState.currentQuestionIndex + 1} of ${quiz.questions.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${uiState.score}",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )

        // Question content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Question type badge
            val typeLabel = when (question) {
                is QuizQuestion.MultipleChoice -> "Multiple Choice"
                is QuizQuestion.TrueFalse -> "True / False"
                is QuizQuestion.CodeCompletion -> "Code Completion"
            }
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Question text
            Text(
                text = question.question,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Answer area based on question type
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

            // Explanation (shown after answering)
            if (uiState.answeredCurrent) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (uiState.currentAnswerCorrect)
                                Color(0xFF3FB950).copy(alpha = 0.08f)
                            else
                                Color(0xFFF85149).copy(alpha = 0.08f)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (uiState.currentAnswerCorrect) Icons.Default.CheckCircle
                                else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (uiState.currentAnswerCorrect) Color(0xFF3FB950)
                                else Color(0xFFF85149),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.currentAnswerCorrect) "Correct!" else "Incorrect",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.currentAnswerCorrect) Color(0xFF3FB950)
                                else Color(0xFFF85149)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Next / Finish button
                val isLast = uiState.currentQuestionIndex == quiz.questions.size - 1
                Button(
                    onClick = { viewModel.nextQuestion() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isLast) "Finish Quiz" else "Next Question",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MultipleChoiceAnswers(
    question: QuizQuestion.MultipleChoice,
    selectedIndex: Int?,
    answered: Boolean,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        question.options.forEachIndexed { index, option ->
            val isSelected = selectedIndex == index
            val isCorrect = index == question.correctIndex

            val borderColor = when {
                !answered -> if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
                isCorrect -> Color(0xFF3FB950)
                isSelected && !isCorrect -> Color(0xFFF85149)
                else -> MaterialTheme.colorScheme.outline
            }

            val bgColor = when {
                !answered -> if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else Color.Transparent
                isCorrect -> Color(0xFF3FB950).copy(alpha = 0.08f)
                isSelected && !isCorrect -> Color(0xFFF85149).copy(alpha = 0.08f)
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(bgColor)
                    .clickable(enabled = !answered) { onSelect(index) }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Option letter
                    Text(
                        text = "${'A' + index}",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = borderColor,
                        modifier = Modifier.width(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (answered && isCorrect) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Correct",
                            tint = Color(0xFF3FB950),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (answered && isSelected && !isCorrect) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Incorrect",
                            tint = Color(0xFFF85149),
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
                !answered -> if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
                isCorrect -> Color(0xFF3FB950)
                isSelected && !isCorrect -> Color(0xFFF85149)
                else -> MaterialTheme.colorScheme.outline
            }

            val bgColor = when {
                !answered -> if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else Color.Transparent
                isCorrect -> Color(0xFF3FB950).copy(alpha = 0.08f)
                isSelected && !isCorrect -> Color(0xFFF85149).copy(alpha = 0.08f)
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(bgColor)
                    .clickable(enabled = !answered) { onSelect(value) }
                    .padding(vertical = 20.dp),
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
                                contentDescription = "Correct",
                                tint = Color(0xFF3FB950),
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (isSelected) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Incorrect",
                                tint = Color(0xFFF85149),
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
        // Code block with blank
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF06080C))
                .padding(16.dp)
        ) {
            Text(
                text = question.code,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFE8ECF2),
                lineHeight = 22.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input field
        OutlinedTextField(
            value = codeInput,
            onValueChange = { if (!answered) onCodeChange(it) },
            label = { Text("Your answer") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !answered,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        if (answered && !isCorrect) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Expected: ${question.correctAnswer}",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF3FB950)
            )
        }

        if (!answered) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = codeInput.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Submit",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuizCompleteView(viewModel: QuizViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val quiz = uiState.currentQuiz ?: return
    val score = uiState.score
    val total = quiz.questions.size
    val percent = (score.toFloat() / total * 100).toInt()

    val resultColor = when {
        percent >= 80 -> Color(0xFF3FB950)
        percent >= 50 -> Color(0xFFF0883E)
        else -> Color(0xFFF85149)
    }

    val resultMessage = when {
        percent == 100 -> "Perfect score!"
        percent >= 80 -> "Great job!"
        percent >= 60 -> "Good effort!"
        percent >= 40 -> "Keep practicing!"
        else -> "Review the material and try again."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Trophy icon
        Icon(
            Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = resultColor,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Quiz Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = quiz.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Score display
        Text(
            text = "$score/$total",
            style = MaterialTheme.typography.displayMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = resultColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "$percent%",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Monospace,
            color = resultColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = resultMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { score.toFloat() / total },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = resultColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Actions
        Button(
            onClick = { viewModel.startQuiz(quiz.id) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Retry Quiz",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { viewModel.returnToList() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Done",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
