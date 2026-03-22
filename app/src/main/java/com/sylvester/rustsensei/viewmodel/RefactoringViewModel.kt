package com.sylvester.rustsensei.viewmodel

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.content.ContentProvider
import com.sylvester.rustsensei.content.RefactoringChallenge
import com.sylvester.rustsensei.data.ProgressDao
import com.sylvester.rustsensei.data.RefactoringResult
import com.sylvester.rustsensei.domain.RefactoringEvent
import com.sylvester.rustsensei.domain.ValidateRefactoringUseCase
import com.sylvester.rustsensei.llm.InferenceConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class RefactoringUiState(
    val challenges: List<RefactoringChallenge> = emptyList(),
    val currentChallenge: RefactoringChallenge? = null,
    val userCode: String = "",
    val score: Int? = null,
    val feedback: String = "",
    val isValidating: Boolean = false,
    val isLoading: Boolean = true,
    val hintsRevealed: Int = 0,
    val showSolution: Boolean = false,
    val errorMessage: String? = null,
    val bestScores: Map<String, Int> = emptyMap()
)

@HiltViewModel
class RefactoringViewModel @Inject constructor(
    private val contentRepo: ContentProvider,
    private val progressDao: ProgressDao,
    private val validateRefactoring: ValidateRefactoringUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "RefactoringViewModel"
    }

    private var validationJob: Job? = null

    private val _uiState = MutableStateFlow(RefactoringUiState())
    val uiState: StateFlow<RefactoringUiState> = _uiState.asStateFlow()

    init {
        loadChallenges()
    }

    private fun loadChallenges() {
        viewModelScope.launch {
            try {
                val challenges = contentRepo.getRefactoringChallenges()
                val scores = mutableMapOf<String, Int>()
                for (challenge in challenges) {
                    val best = progressDao.getBestRefactoringResult(challenge.id)
                    if (best != null) {
                        scores[challenge.id] = best.score
                    }
                }
                _uiState.value = _uiState.value.copy(
                    challenges = challenges,
                    isLoading = false,
                    bestScores = scores
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading challenges: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load challenges: ${e.message}"
                )
            }
        }
    }

    fun openChallenge(challengeId: String) {
        viewModelScope.launch {
            try {
                val challenge = contentRepo.getRefactoringChallenge(challengeId)
                if (challenge != null) {
                    validationJob?.cancel()
                    _uiState.value = _uiState.value.copy(
                        currentChallenge = challenge,
                        userCode = challenge.uglyCode,
                        score = null,
                        feedback = "",
                        isValidating = false,
                        hintsRevealed = 0,
                        showSolution = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error opening challenge: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load challenge: ${e.message}"
                )
            }
        }
    }

    fun navigateBack() {
        validationJob?.cancel()
        _uiState.value = _uiState.value.copy(
            currentChallenge = null,
            score = null,
            feedback = "",
            isValidating = false,
            showSolution = false,
            errorMessage = null
        )
    }

    fun updateCode(code: String) {
        _uiState.value = _uiState.value.copy(userCode = code)
    }

    fun submitForScoring() {
        val challenge = _uiState.value.currentChallenge ?: return
        if (_uiState.value.isValidating) return

        validationJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isValidating = true,
            feedback = "",
            score = null
        )

        val config = InferenceConfig(
            temperature = 0.3f,
            topP = 0.9f,
            maxTokens = 384,
            contextLength = 2048
        )

        validationJob = viewModelScope.launch {
            validateRefactoring(
                originalCode = challenge.uglyCode,
                userCode = _uiState.value.userCode.trim(),
                idiomaticSolution = challenge.idiomaticSolution,
                scoringCriteria = challenge.scoringCriteria,
                config = config
            ).collect { event ->
                when (event) {
                    is RefactoringEvent.Token -> {
                        _uiState.value = _uiState.value.copy(
                            feedback = event.displayText
                        )
                    }
                    is RefactoringEvent.Completed -> {
                        _uiState.value = _uiState.value.copy(
                            isValidating = false,
                            feedback = event.fullText,
                            score = event.score
                        )
                        saveResult(challenge.id, event.score, event.fullText)
                    }
                    is RefactoringEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isValidating = false,
                            errorMessage = event.message
                        )
                    }
                }
            }
        }
    }

    fun stopValidation() {
        validationJob?.cancel()
        _uiState.value = _uiState.value.copy(isValidating = false)
    }

    fun revealHint() {
        val challenge = _uiState.value.currentChallenge ?: return
        val currentHints = _uiState.value.hintsRevealed
        if (currentHints < challenge.hints.size) {
            _uiState.value = _uiState.value.copy(hintsRevealed = currentHints + 1)
        }
    }

    fun showSolution() {
        _uiState.value = _uiState.value.copy(showSolution = true)
    }

    fun resetChallenge() {
        val challenge = _uiState.value.currentChallenge ?: return
        validationJob?.cancel()
        _uiState.value = _uiState.value.copy(
            userCode = challenge.uglyCode,
            score = null,
            feedback = "",
            isValidating = false,
            hintsRevealed = 0,
            showSolution = false,
            errorMessage = null
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun saveResult(challengeId: String, score: Int, feedback: String) {
        viewModelScope.launch {
            try {
                progressDao.insertRefactoringResult(
                    RefactoringResult(
                        challengeId = challengeId,
                        userCode = _uiState.value.userCode,
                        score = score,
                        feedback = feedback,
                        completedAt = System.currentTimeMillis()
                    )
                )
                // Update best scores map
                val currentBest = _uiState.value.bestScores[challengeId] ?: 0
                if (score > currentBest) {
                    val updated = _uiState.value.bestScores.toMutableMap()
                    updated[challengeId] = score
                    _uiState.value = _uiState.value.copy(bestScores = updated)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving result: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        validationJob?.cancel()
    }
}
