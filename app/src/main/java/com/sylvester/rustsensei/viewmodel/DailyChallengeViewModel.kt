package com.sylvester.rustsensei.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.content.ExerciseData
import com.sylvester.rustsensei.domain.CompleteDailyChallengeUseCase
import com.sylvester.rustsensei.domain.GetDailyChallengeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class DailyChallengeUiState(
    val exercise: ExerciseData? = null,
    val userCode: String = "",
    val isCompleted: Boolean = false,
    val isLoading: Boolean = true,
    val completionTimeSeconds: Long? = null,
    val showConfetti: Boolean = false,
    val elapsedSeconds: Long = 0
)

@HiltViewModel
class DailyChallengeViewModel @Inject constructor(
    private val getDailyChallenge: GetDailyChallengeUseCase,
    private val completeDailyChallenge: CompleteDailyChallengeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyChallengeUiState())
    val uiState: StateFlow<DailyChallengeUiState> = _uiState.asStateFlow()

    private var startTimeMs: Long = 0

    init {
        loadChallenge()
    }

    fun loadChallenge() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val data = getDailyChallenge()
            _uiState.value = _uiState.value.copy(
                exercise = data.exercise,
                isCompleted = data.isCompleted,
                completionTimeSeconds = data.completionTime,
                userCode = data.exercise?.starterCode ?: "",
                isLoading = false
            )
            if (!data.isCompleted) {
                startTimeMs = System.currentTimeMillis()
            }
        }
    }

    fun updateCode(code: String) {
        _uiState.value = _uiState.value.copy(userCode = code)
    }

    fun submitChallenge() {
        val exercise = _uiState.value.exercise ?: return
        val timeTaken = (System.currentTimeMillis() - startTimeMs) / 1000

        viewModelScope.launch {
            completeDailyChallenge(exercise.id, timeTaken)
            _uiState.value = _uiState.value.copy(
                isCompleted = true,
                completionTimeSeconds = timeTaken,
                showConfetti = true
            )
        }
    }

    fun dismissConfetti() {
        _uiState.value = _uiState.value.copy(showConfetti = false)
    }
}
