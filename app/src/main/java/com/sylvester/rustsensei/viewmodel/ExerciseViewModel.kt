package com.sylvester.rustsensei.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.RustSenseiApplication
import com.sylvester.rustsensei.content.ExerciseCategory
import com.sylvester.rustsensei.content.ExerciseData
import com.sylvester.rustsensei.data.ExerciseProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ExerciseScreenMode {
    CATEGORIES,
    DETAIL
}

data class ExerciseUiState(
    val mode: ExerciseScreenMode = ExerciseScreenMode.CATEGORIES,
    val categories: List<ExerciseCategory> = emptyList(),
    val expandedCategory: String? = null,
    val currentExercise: ExerciseData? = null,
    val currentProgress: ExerciseProgress? = null,
    val userCode: String = "",
    val hintsRevealed: Int = 0,
    val showSolution: Boolean = false,
    val checkResult: String? = null, // "correct", "incorrect", "uncertain"
    val categoryProgress: Map<String, List<ExerciseProgress>> = emptyMap(),
    val lastIncompleteExerciseId: String? = null
)

class ExerciseViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RustSenseiApplication
    private val contentRepo = app.contentRepository
    private val progressRepo = app.progressRepository

    private val _uiState = MutableStateFlow(ExerciseUiState())
    val uiState: StateFlow<ExerciseUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadLastIncomplete()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val categories = withContext(Dispatchers.IO) {
                contentRepo.getExerciseCategories()
            }
            _uiState.value = _uiState.value.copy(categories = categories)
        }
    }

    // Design Concern #3: load last incomplete exercise for "Continue" flow
    private fun loadLastIncomplete() {
        viewModelScope.launch {
            val last = progressRepo.getLastIncompleteExercise()
            _uiState.value = _uiState.value.copy(
                lastIncompleteExerciseId = last?.exerciseId
            )
        }
    }

    // Fix #5: load progress on-demand when a category is expanded, not all at init
    fun toggleCategory(categoryId: String) {
        val current = _uiState.value.expandedCategory
        val newExpanded = if (current == categoryId) null else categoryId
        _uiState.value = _uiState.value.copy(expandedCategory = newExpanded)

        // Load progress for the newly expanded category if not already loaded
        if (newExpanded != null && !_uiState.value.categoryProgress.containsKey(newExpanded)) {
            loadCategoryProgress(newExpanded)
        }
    }

    private fun loadCategoryProgress(categoryId: String) {
        viewModelScope.launch {
            progressRepo.getExerciseProgressByCategory(categoryId).collect { progressList ->
                val currentMap = _uiState.value.categoryProgress.toMutableMap()
                currentMap[categoryId] = progressList
                _uiState.value = _uiState.value.copy(categoryProgress = currentMap)
            }
        }
    }

    // Refresh progress for the current category after completing an exercise
    private fun refreshCurrentCategoryProgress() {
        val exercise = _uiState.value.currentExercise ?: return
        val categoryId = exercise.category
        // Re-fetch from DB to update the map
        viewModelScope.launch {
            val progressList = withContext(Dispatchers.IO) {
                // Fetch once, not as a flow
                progressRepo.getExerciseProgressByCategory(categoryId)
            }
            // Collect just the first emission to update
            progressList.collect { list ->
                val currentMap = _uiState.value.categoryProgress.toMutableMap()
                currentMap[categoryId] = list
                _uiState.value = _uiState.value.copy(categoryProgress = currentMap)
            }
        }
    }

    fun openExercise(exerciseId: String) {
        viewModelScope.launch {
            val exercise = withContext(Dispatchers.IO) {
                contentRepo.getExercise(exerciseId)
            }
            val progress = progressRepo.getExerciseProgress(exerciseId)
            if (exercise != null) {
                _uiState.value = _uiState.value.copy(
                    mode = ExerciseScreenMode.DETAIL,
                    currentExercise = exercise,
                    currentProgress = progress,
                    userCode = progress?.userCode?.ifEmpty { exercise.starterCode } ?: exercise.starterCode,
                    hintsRevealed = progress?.hintsViewed ?: 0,
                    showSolution = false,
                    checkResult = null
                )
            }
        }
    }

    fun navigateBack() {
        saveCurrentCode()
        _uiState.value = _uiState.value.copy(
            mode = ExerciseScreenMode.CATEGORIES,
            currentExercise = null,
            currentProgress = null,
            checkResult = null,
            showSolution = false
        )
    }

    fun updateCode(code: String) {
        _uiState.value = _uiState.value.copy(userCode = code)
    }

    fun revealHint() {
        val exercise = _uiState.value.currentExercise ?: return
        val currentHints = _uiState.value.hintsRevealed
        if (currentHints < exercise.hints.size) {
            _uiState.value = _uiState.value.copy(hintsRevealed = currentHints + 1)
            viewModelScope.launch {
                progressRepo.revealHint(exercise.id, exercise.category)
            }
        }
    }

    fun showSolution() {
        _uiState.value = _uiState.value.copy(showSolution = true)
    }

    fun checkSolution() {
        val exercise = _uiState.value.currentExercise ?: return
        val userCode = _uiState.value.userCode.trim()
        val solutionCode = exercise.solution.trim()

        viewModelScope.launch {
            progressRepo.recordAttempt(exercise.id, exercise.category)
        }

        val normalizedUser = normalizeCode(userCode)
        val normalizedSolution = normalizeCode(solutionCode)

        if (normalizedUser == normalizedSolution) {
            _uiState.value = _uiState.value.copy(checkResult = "correct")
            viewModelScope.launch {
                progressRepo.markExerciseComplete(exercise.id, exercise.category)
            }
        } else {
            val isLikelyCorrect = checkKeyPatterns(exercise, userCode)
            if (isLikelyCorrect) {
                _uiState.value = _uiState.value.copy(checkResult = "correct")
                viewModelScope.launch {
                    progressRepo.markExerciseComplete(exercise.id, exercise.category)
                }
            } else {
                // Design Concern #1: if user code differs significantly from starter,
                // mark as "uncertain" to suggest LLM check, not flat "incorrect"
                val userChangedCode = normalizedUser != normalizeCode(exercise.starterCode)
                _uiState.value = _uiState.value.copy(
                    checkResult = if (userChangedCode) "uncertain" else "incorrect"
                )
            }
        }
    }

    // Design Concern #1: allow user to self-mark as correct after LLM review
    fun markCurrentExerciseCorrect() {
        val exercise = _uiState.value.currentExercise ?: return
        _uiState.value = _uiState.value.copy(checkResult = "correct")
        viewModelScope.launch {
            progressRepo.markExerciseComplete(exercise.id, exercise.category)
        }
    }

    fun resetExercise() {
        val exercise = _uiState.value.currentExercise ?: return
        _uiState.value = _uiState.value.copy(
            userCode = exercise.starterCode,
            checkResult = null,
            showSolution = false
        )
    }

    fun getExerciseDescription(): String {
        return _uiState.value.currentExercise?.description ?: ""
    }

    fun getExerciseId(): String {
        return _uiState.value.currentExercise?.id ?: ""
    }

    fun getUserCode(): String {
        return _uiState.value.userCode
    }

    private fun saveCurrentCode() {
        val exercise = _uiState.value.currentExercise ?: return
        val code = _uiState.value.userCode
        viewModelScope.launch {
            progressRepo.updateExerciseCode(exercise.id, exercise.category, code)
        }
    }

    private fun normalizeCode(code: String): String {
        return code.replace(Regex("\\s+"), " ")
            .replace(Regex("//.*"), "")
            .trim()
    }

    private fun checkKeyPatterns(exercise: ExerciseData, userCode: String): Boolean {
        val starterNorm = normalizeCode(exercise.starterCode)
        val solutionNorm = normalizeCode(exercise.solution)
        val userNorm = normalizeCode(userCode)

        if (starterNorm == solutionNorm) return userNorm == solutionNorm

        val solutionWords = solutionNorm.split(" ").toSet()
        val starterWords = starterNorm.split(" ").toSet()
        val keyDifferences = solutionWords - starterWords

        val userWords = userNorm.split(" ").toSet()
        val matchedDiffs = keyDifferences.intersect(userWords)

        return matchedDiffs.size >= keyDifferences.size * 0.7
    }
}
