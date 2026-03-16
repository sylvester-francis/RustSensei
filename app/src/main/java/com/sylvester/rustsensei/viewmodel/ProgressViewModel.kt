package com.sylvester.rustsensei.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.content.ContentRepository
import com.sylvester.rustsensei.data.LearningStats
import com.sylvester.rustsensei.data.ProgressRepository
import com.sylvester.rustsensei.data.UserNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean
)

data class ContinueTarget(
    val type: String, // "section" or "exercise"
    val id: String,
    val parentId: String? = null // chapterId for sections
)

data class DayActivity(
    val label: String, // e.g. "Mon"
    val level: Int // 0=none, 1=low, 2=high
)

data class ProgressUiState(
    val completedSections: Int = 0,
    val totalSections: Int = 0,
    val completedExercises: Int = 0,
    val totalExercises: Int = 0,
    val totalStudyTimeSeconds: Long = 0,
    val weeklyStats: List<LearningStats> = emptyList(),
    val studyStreak: Int = 0,
    val continueTarget: ContinueTarget? = null,
    val achievements: List<Achievement> = emptyList(),
    val weekActivity: List<DayActivity> = emptyList(),
    val recentNotes: List<UserNote> = emptyList()
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
        loadRecentNotes()
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
                            refreshAchievements()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error collecting completedSections: ${e.message}", e)
                    }
                }
                launch {
                    try {
                        progressRepo.getCompletedExercisesCount().collect { count ->
                            _uiState.value = _uiState.value.copy(completedExercises = count)
                            refreshAchievements()
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
                            val streak = calculateStreak(stats)
                            _uiState.value = _uiState.value.copy(
                                weeklyStats = stats,
                                studyStreak = streak,
                                weekActivity = buildWeekActivity(stats)
                            )
                            refreshAchievements()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error collecting recentStats: ${e.message}", e)
                    }
                }

                // Design Concern #3: find the best "Continue Learning" target
                loadContinueTarget()

                // Load achievements on startup
                refreshAchievements()
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadProgress: ${e.message}", e)
            }
        }
    }

    private fun loadRecentNotes() {
        viewModelScope.launch {
            try {
                progressRepo.getAllNotes().collect { notes ->
                    _uiState.value = _uiState.value.copy(
                        recentNotes = notes.take(3)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting recent notes: ${e.message}", e)
            }
        }
    }

    private fun refreshAchievements() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val completedSections = progressRepo.getCompletedSectionsCountSync()
                val completedExercises = progressRepo.getCompletedExercisesCountSync()
                val moveSemanticsDone = progressRepo.getCompletedExerciseCountByCategory("move_semantics")
                // Check for perfect quiz score
                val quizResults = progressRepo.getAllQuizResults().first()
                val hasPerfectQuiz = quizResults.any { it.score == it.totalQuestions && it.totalQuestions > 0 }

                val achievements = listOf(
                    Achievement(
                        id = "first_steps",
                        title = "First Steps",
                        description = "Complete your first section",
                        icon = "\uD83D\uDE80",
                        isUnlocked = completedSections >= 1
                    ),
                    Achievement(
                        id = "borrow_checker_survivor",
                        title = "Borrow Checker Survivor",
                        description = "Complete all ownership exercises",
                        icon = "\uD83D\uDEE1\uFE0F",
                        isUnlocked = moveSemanticsDone >= 5
                    ),
                    Achievement(
                        id = "streak_master",
                        title = "Streak Master",
                        description = "Maintain a 7-day streak",
                        icon = "\uD83D\uDD25",
                        isUnlocked = state.studyStreak >= 7
                    ),
                    Achievement(
                        id = "century",
                        title = "Century",
                        description = "Complete 100 exercises",
                        icon = "\uD83D\uDCAF",
                        isUnlocked = completedExercises >= 100
                    ),
                    Achievement(
                        id = "bookworm",
                        title = "Bookworm",
                        description = "Read 10 sections",
                        icon = "\uD83D\uDCDA",
                        isUnlocked = completedSections >= 10
                    ),
                    Achievement(
                        id = "quiz_whiz",
                        title = "Quiz Whiz",
                        description = "Score 100% on any quiz",
                        icon = "\uD83E\uDDE0",
                        isUnlocked = hasPerfectQuiz
                    )
                )

                _uiState.value = _uiState.value.copy(achievements = achievements)
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing achievements: ${e.message}", e)
            }
        }
    }

    private fun buildWeekActivity(stats: List<LearningStats>): List<DayActivity> {
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        // Fill 7 slots from stats (most recent first in stats)
        val reversed = stats.sortedBy { it.date }.takeLast(7)
        return (0 until 7).map { i ->
            val stat = reversed.getOrNull(i)
            val activity = (stat?.sectionsRead ?: 0) + (stat?.exercisesCompleted ?: 0)
            val level = when {
                activity >= 3 -> 2
                activity >= 1 -> 1
                else -> 0
            }
            DayActivity(
                label = dayLabels.getOrElse(i) { "" },
                level = level
            )
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
