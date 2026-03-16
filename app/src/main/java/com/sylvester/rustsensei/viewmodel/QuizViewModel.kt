package com.sylvester.rustsensei.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.content.ContentRepository
import com.sylvester.rustsensei.data.Quiz
import com.sylvester.rustsensei.data.QuizIndexEntry
import com.sylvester.rustsensei.data.QuizQuestion
import com.sylvester.rustsensei.data.QuizResult
import com.sylvester.rustsensei.data.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class QuizScreenMode {
    LIST,
    IN_PROGRESS,
    COMPLETE
}

data class QuizUiState(
    val mode: QuizScreenMode = QuizScreenMode.LIST,
    val quizzes: List<QuizIndexEntry> = emptyList(),
    val bestScores: Map<String, QuizResult> = emptyMap(),
    val currentQuiz: Quiz? = null,
    val currentQuestionIndex: Int = 0,
    val userAnswers: Map<Int, Any> = emptyMap(), // index -> answer (Int for MC, Boolean for TF, String for CC)
    val answeredCurrent: Boolean = false,
    val currentAnswerCorrect: Boolean = false,
    val score: Int = 0,
    val codeInput: String = ""
)

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val contentRepo: ContentRepository,
    private val progressRepo: ProgressRepository
) : ViewModel() {

    companion object {
        private const val TAG = "QuizViewModel"
    }

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        loadQuizzes()
    }

    fun loadQuizzes() {
        viewModelScope.launch {
            try {
                val quizzes = contentRepo.getQuizIndex()
                val scores = mutableMapOf<String, QuizResult>()
                for (quiz in quizzes) {
                    val best = progressRepo.getBestQuizResult(quiz.id)
                    if (best != null) {
                        scores[quiz.id] = best
                    }
                }
                _uiState.value = _uiState.value.copy(
                    quizzes = quizzes,
                    bestScores = scores
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading quizzes: ${e.message}", e)
            }
        }
    }

    fun startQuiz(quizId: String) {
        viewModelScope.launch {
            try {
                val quiz = contentRepo.getQuiz(quizId)
                if (quiz != null) {
                    _uiState.value = _uiState.value.copy(
                        mode = QuizScreenMode.IN_PROGRESS,
                        currentQuiz = quiz,
                        currentQuestionIndex = 0,
                        userAnswers = emptyMap(),
                        answeredCurrent = false,
                        currentAnswerCorrect = false,
                        score = 0,
                        codeInput = ""
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting quiz: ${e.message}", e)
            }
        }
    }

    fun answerMultipleChoice(selectedIndex: Int) {
        if (_uiState.value.answeredCurrent) return
        val quiz = _uiState.value.currentQuiz ?: return
        val questionIndex = _uiState.value.currentQuestionIndex
        val question = quiz.questions[questionIndex]

        if (question is QuizQuestion.MultipleChoice) {
            val correct = selectedIndex == question.correctIndex
            val newScore = if (correct) _uiState.value.score + 1 else _uiState.value.score
            val answers = _uiState.value.userAnswers.toMutableMap()
            answers[questionIndex] = selectedIndex

            _uiState.value = _uiState.value.copy(
                userAnswers = answers,
                answeredCurrent = true,
                currentAnswerCorrect = correct,
                score = newScore
            )
        }
    }

    fun answerTrueFalse(answer: Boolean) {
        if (_uiState.value.answeredCurrent) return
        val quiz = _uiState.value.currentQuiz ?: return
        val questionIndex = _uiState.value.currentQuestionIndex
        val question = quiz.questions[questionIndex]

        if (question is QuizQuestion.TrueFalse) {
            val correct = answer == question.correctAnswer
            val newScore = if (correct) _uiState.value.score + 1 else _uiState.value.score
            val answers = _uiState.value.userAnswers.toMutableMap()
            answers[questionIndex] = answer

            _uiState.value = _uiState.value.copy(
                userAnswers = answers,
                answeredCurrent = true,
                currentAnswerCorrect = correct,
                score = newScore
            )
        }
    }

    fun updateCodeInput(code: String) {
        _uiState.value = _uiState.value.copy(codeInput = code)
    }

    fun submitCodeAnswer() {
        if (_uiState.value.answeredCurrent) return
        val quiz = _uiState.value.currentQuiz ?: return
        val questionIndex = _uiState.value.currentQuestionIndex
        val question = quiz.questions[questionIndex]

        if (question is QuizQuestion.CodeCompletion) {
            val userAnswer = _uiState.value.codeInput.trim()
            val correct = question.acceptableAnswers.any { acceptable ->
                userAnswer.equals(acceptable.trim(), ignoreCase = false) ||
                    userAnswer.replace("\\s+".toRegex(), "")
                        .equals(acceptable.trim().replace("\\s+".toRegex(), ""), ignoreCase = false)
            }
            val newScore = if (correct) _uiState.value.score + 1 else _uiState.value.score
            val answers = _uiState.value.userAnswers.toMutableMap()
            answers[questionIndex] = userAnswer

            _uiState.value = _uiState.value.copy(
                userAnswers = answers,
                answeredCurrent = true,
                currentAnswerCorrect = correct,
                score = newScore
            )
        }
    }

    fun nextQuestion() {
        val quiz = _uiState.value.currentQuiz ?: return
        val nextIndex = _uiState.value.currentQuestionIndex + 1

        if (nextIndex < quiz.questions.size) {
            _uiState.value = _uiState.value.copy(
                currentQuestionIndex = nextIndex,
                answeredCurrent = false,
                currentAnswerCorrect = false,
                codeInput = ""
            )
        } else {
            finishQuiz()
        }
    }

    private fun finishQuiz() {
        val quiz = _uiState.value.currentQuiz ?: return
        val result = QuizResult(
            quizId = quiz.id,
            score = _uiState.value.score,
            totalQuestions = quiz.questions.size
        )

        viewModelScope.launch {
            try {
                progressRepo.saveQuizResult(result)
                // Refresh best scores
                val scores = _uiState.value.bestScores.toMutableMap()
                val best = progressRepo.getBestQuizResult(quiz.id)
                if (best != null) {
                    scores[quiz.id] = best
                }
                _uiState.value = _uiState.value.copy(
                    mode = QuizScreenMode.COMPLETE,
                    bestScores = scores
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error saving quiz result: ${e.message}", e)
                _uiState.value = _uiState.value.copy(mode = QuizScreenMode.COMPLETE)
            }
        }
    }

    fun returnToList() {
        _uiState.value = _uiState.value.copy(
            mode = QuizScreenMode.LIST,
            currentQuiz = null,
            currentQuestionIndex = 0,
            userAnswers = emptyMap(),
            answeredCurrent = false,
            currentAnswerCorrect = false,
            score = 0,
            codeInput = ""
        )
        loadQuizzes()
    }
}
