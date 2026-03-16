package com.sylvester.rustsensei.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.content.ContentRepository
import com.sylvester.rustsensei.data.LearningStats
import com.sylvester.rustsensei.data.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContinueTarget(
    val type: String, // "section" or "exercise"
    val id: String,
    val parentId: String? = null // chapterId for sections
)

data class ProgressUiState(
    val completedSections: Int = 0,
    val totalSections: Int = 0,
    val completedExercises: Int = 0,
    val totalExercises: Int = 0,
    val totalStudyTimeSeconds: Long = 0,
    val weeklyStats: List<LearningStats> = emptyList(),
    val studyStreak: Int = 0,
    val continueTarget: ContinueTarget? = null
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val progressRepo: ProgressRepository,
    private val contentRepo: ContentRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ProgressViewModel"
    }

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadProgress()
    }

    private fun loadProgress() {
        viewModelScope.launch {
            try {
                val totalSections = contentRepo.getTotalSectionsCount()
                val totalExercises = contentRepo.getTotalExercisesCount()

                _uiState.value = _uiState.value.copy(
                    totalSections = totalSections,
                    totalExercises = totalExercises
                )

                launch {
                    try {
                        progressRepo.getCompletedSectionsCount().collect { count ->
                            _uiState.value = _uiState.value.copy(completedSections = count)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error collecting completedSections: ${e.message}", e)
                    }
                }
                launch {
                    try {
                        progressRepo.getCompletedExercisesCount().collect { count ->
                            _uiState.value = _uiState.value.copy(completedExercises = count)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error collecting completedExercises: ${e.message}", e)
                    }
                }
                launch {
                    try {
                        progressRepo.getTotalStudyTime().collect { time ->
                            _uiState.value = _uiState.value.copy(totalStudyTimeSeconds = time ?: 0)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error collecting totalStudyTime: ${e.message}", e)
                    }
                }
                launch {
                    try {
                        progressRepo.getRecentStats(7).collect { stats ->
                            _uiState.value = _uiState.value.copy(
                                weeklyStats = stats,
                                studyStreak = calculateStreak(stats)
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error collecting recentStats: ${e.message}", e)
                    }
                }

                // Design Concern #3: find the best "Continue Learning" target
                loadContinueTarget()
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadProgress: ${e.message}", e)
            }
        }
    }

    private fun loadContinueTarget() {
        viewModelScope.launch {
            try {
                // Prefer the last read section, fall back to last incomplete exercise
                val lastSection = progressRepo.getLastReadSection()
                if (lastSection != null && !lastSection.isCompleted) {
                    _uiState.value = _uiState.value.copy(
                        continueTarget = ContinueTarget(
                            type = "section",
                            id = lastSection.sectionId,
                            parentId = lastSection.chapterId
                        )
                    )
                    return@launch
                }
                val lastExercise = progressRepo.getLastIncompleteExercise()
                if (lastExercise != null) {
                    _uiState.value = _uiState.value.copy(
                        continueTarget = ContinueTarget(
                            type = "exercise",
                            id = lastExercise.exerciseId
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadContinueTarget: ${e.message}", e)
            }
        }
    }

    private fun calculateStreak(stats: List<LearningStats>): Int {
        if (stats.isEmpty()) return 0
        var streak = 0
        for (stat in stats.sortedByDescending { it.date }) {
            if (stat.sectionsRead > 0 || stat.exercisesCompleted > 0 || stat.studyTimeSeconds > 60) {
                streak++
            } else {
                break
            }
        }
        return streak
    }
}
