package com.sylvester.rustsensei.viewmodel

import androidx.compose.runtime.Immutable
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.content.ContentRepository
import com.sylvester.rustsensei.content.ExerciseCategory
import com.sylvester.rustsensei.content.ExerciseData
import com.sylvester.rustsensei.data.ExerciseProgress
import com.sylvester.rustsensei.data.ProgressRepository
import com.sylvester.rustsensei.domain.RunExerciseTestsUseCase
import com.sylvester.rustsensei.domain.RunTestsEvent
import com.sylvester.rustsensei.domain.TestCaseResult
import com.sylvester.rustsensei.domain.ValidateExerciseUseCase
import com.sylvester.rustsensei.domain.ValidationEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExerciseScreenMode {
    CATEGORIES,
    DETAIL
}

@Immutable
data class TestResultState(
    val passed: Int,
    val total: Int,
    val results: List<TestCaseResult>,
    val rawOutput: String
)

@Immutable
data class ExerciseUiState(
    val mode: ExerciseScreenMode = ExerciseScreenMode.CATEGORIES,
    val categories: List<ExerciseCategory> = emptyList(),
    val expandedCategory: String? = null,
    val currentExercise: ExerciseData? = null,
    val currentProgress: ExerciseProgress? = null,
    val userCode: String = "",
    val hintsRevealed: Int = 0,
    val showSolution: Boolean = false,
    val checkResult: String? = null,
    val categoryProgress: Map<String, List<ExerciseProgress>> = emptyMap(),
    val lastIncompleteExerciseId: String? = null,
    val llmValidationResult: String = "",
    val isValidating: Boolean = false,
    val isRunningTests: Boolean = false,
    val testResults: TestResultState? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val contentRepo: ContentRepository,
    private val progressRepo: ProgressRepository,
    private val validateExercise: ValidateExerciseUseCase,
    private val runExerciseTests: RunExerciseTestsUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "ExerciseViewModel"
    }

    private var validationJob: Job? = null
    private var testJob: Job? = null
    private val categoryObserverJobs = mutableMapOf<String, Job>()

    private val _uiState = MutableStateFlow(ExerciseUiState())
    val uiState: StateFlow<ExerciseUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadLastIncomplete()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val categories = contentRepo.getExerciseCategories()
                _uiState.value = _uiState.value.copy(categories = categories)
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadCategories: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load exercises: ${e.message}"
                )
            }
        }
    }

    private fun loadLastIncomplete() {
        viewModelScope.launch {
            try {
                val last = progressRepo.getLastIncompleteExercise()
                _uiState.value = _uiState.value.copy(
                    lastIncompleteExerciseId = last?.exerciseId
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadLastIncomplete: ${e.message}", e)
            }
        }
    }

    fun toggleCategory(categoryId: String) {
        val current = _uiState.value.expandedCategory
        val newExpanded = if (current == categoryId) null else categoryId
        _uiState.value = _uiState.value.copy(expandedCategory = newExpanded)

        if (newExpanded != null) {
            categoryObserverJobs[newExpanded]?.cancel()
            loadCategoryProgress(newExpanded)
        }
    }

    private fun loadCategoryProgress(categoryId: String) {
        val job = viewModelScope.launch {
            try {
                progressRepo.getExerciseProgressByCategory(categoryId).collect { progressList ->
                    val currentMap = _uiState.value.categoryProgress.toMutableMap()
                    currentMap[categoryId] = progressList
                    _uiState.value = _uiState.value.copy(categoryProgress = currentMap)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadCategoryProgress: ${e.message}", e)
            }
        }
        categoryObserverJobs[categoryId] = job
    }

    private fun refreshCurrentCategoryProgress() {
        val exercise = _uiState.value.currentExercise ?: return
        val categoryId = exercise.category
        categoryObserverJobs[categoryId]?.cancel()
        loadCategoryProgress(categoryId)
    }

    fun openExercise(exerciseId: String) {
        viewModelScope.launch {
            try {
                val exercise = contentRepo.getExercise(exerciseId)
                val progress = progressRepo.getExerciseProgress(exerciseId)
                if (exercise != null) {
                    validationJob?.cancel()
                    testJob?.cancel()
                    _uiState.value = _uiState.value.copy(
                        mode = ExerciseScreenMode.DETAIL,
                        currentExercise = exercise,
                        currentProgress = progress,
                        userCode = progress?.userCode?.ifEmpty { exercise.starterCode } ?: exercise.starterCode,
                        hintsRevealed = progress?.hintsViewed ?: 0,
                        showSolution = false,
                        checkResult = null,
                        llmValidationResult = "",
                        isValidating = false,
                        isRunningTests = false,
                        testResults = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in openExercise: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load exercise: ${e.message}"
                )
            }
        }
    }

    fun navigateBack() {
        saveCurrentCode()
        validationJob?.cancel()
        testJob?.cancel()
        _uiState.value = _uiState.value.copy(
            mode = ExerciseScreenMode.CATEGORIES,
            currentExercise = null,
            currentProgress = null,
            checkResult = null,
            showSolution = false,
            llmValidationResult = "",
            isValidating = false,
            isRunningTests = false,
            testResults = null
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
                try {
                    progressRepo.revealHint(exercise.id, exercise.category)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in revealHint: ${e.message}", e)
                }
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
            try {
                progressRepo.recordAttempt(exercise.id, exercise.category)
            } catch (e: Exception) {
                Log.e(TAG, "Error in recordAttempt: ${e.message}", e)
            }
        }

        val normalizedUser = normalizeCode(userCode)
        val normalizedSolution = normalizeCode(solutionCode)

        if (normalizedUser == normalizedSolution) {
            _uiState.value = _uiState.value.copy(checkResult = "correct")
            viewModelScope.launch {
                try {
                    progressRepo.markExerciseComplete(exercise.id, exercise.category)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in markExerciseComplete: ${e.message}", e)
                }
            }
        } else {
            val isLikelyCorrect = checkKeyPatterns(exercise, userCode)
            if (isLikelyCorrect) {
                _uiState.value = _uiState.value.copy(checkResult = "correct")
                viewModelScope.launch {
                    try {
                        progressRepo.markExerciseComplete(exercise.id, exercise.category)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in markExerciseComplete: ${e.message}", e)
                    }
                }
            } else {
                val userChangedCode = normalizedUser != normalizeCode(exercise.starterCode)
                _uiState.value = _uiState.value.copy(
                    checkResult = if (userChangedCode) "uncertain" else "incorrect"
                )
            }
        }
    }

    fun markCurrentExerciseCorrect() {
        val exercise = _uiState.value.currentExercise ?: return
        _uiState.value = _uiState.value.copy(checkResult = "correct")
        viewModelScope.launch {
            try {
                progressRepo.markExerciseComplete(exercise.id, exercise.category)
            } catch (e: Exception) {
                Log.e(TAG, "Error in markCurrentExerciseCorrect: ${e.message}", e)
            }
        }
    }

    fun validateWithLlm() {
        val exercise = _uiState.value.currentExercise ?: return
        if (_uiState.value.isValidating) return

        validationJob?.cancel()
        _uiState.value = _uiState.value.copy(isValidating = true, llmValidationResult = "")

        validationJob = viewModelScope.launch {
            validateExercise(exercise, _uiState.value.userCode.trim())
                .collect { event ->
                    when (event) {
                        is ValidationEvent.Token -> {
                            _uiState.value = _uiState.value.copy(
                                llmValidationResult = event.displayText
                            )
                        }
                        is ValidationEvent.Completed -> {
                            _uiState.value = _uiState.value.copy(
                                isValidating = false,
                                llmValidationResult = event.fullText
                            )
                            if (event.isCorrect) {
                                _uiState.value = _uiState.value.copy(checkResult = "correct")
                                progressRepo.markExerciseComplete(exercise.id, exercise.category)
                                refreshCurrentCategoryProgress()
                            }
                        }
                        is ValidationEvent.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isValidating = false,
                                llmValidationResult = event.message
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

    fun runTests() {
        val exercise = _uiState.value.currentExercise ?: return
        if (exercise.tests.isBlank()) return
        if (_uiState.value.isRunningTests) return

        testJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isRunningTests = true,
            testResults = null,
            errorMessage = null
        )

        testJob = viewModelScope.launch {
            runExerciseTests(_uiState.value.userCode.trim(), exercise.tests)
                .collect { event ->
                    when (event) {
                        is RunTestsEvent.Running -> { /* already set */ }
                        is RunTestsEvent.Completed -> {
                            _uiState.value = _uiState.value.copy(
                                isRunningTests = false,
                                testResults = TestResultState(
                                    passed = event.passed,
                                    total = event.total,
                                    results = event.testResults,
                                    rawOutput = event.rawOutput
                                )
                            )
                            if (event.passed == event.total && event.total > 0) {
                                _uiState.value = _uiState.value.copy(checkResult = "correct")
                                progressRepo.markExerciseComplete(exercise.id, exercise.category)
                                refreshCurrentCategoryProgress()
                            }
                        }
                        is RunTestsEvent.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isRunningTests = false,
                                errorMessage = event.message
                            )
                        }
                    }
                }
        }
    }

    fun stopTests() {
        testJob?.cancel()
        _uiState.value = _uiState.value.copy(isRunningTests = false)
    }

    fun resetExercise() {
        val exercise = _uiState.value.currentExercise ?: return
        validationJob?.cancel()
        testJob?.cancel()
        _uiState.value = _uiState.value.copy(
            userCode = exercise.starterCode,
            checkResult = null,
            showSolution = false,
            llmValidationResult = "",
            isValidating = false,
            isRunningTests = false,
            testResults = null
        )
    }

    fun getExerciseDescription(): String = _uiState.value.currentExercise?.description ?: ""

    fun getExerciseId(): String = _uiState.value.currentExercise?.id ?: ""

    fun getUserCode(): String = _uiState.value.userCode

    private fun saveCurrentCode() {
        val exercise = _uiState.value.currentExercise ?: return
        val code = _uiState.value.userCode
        viewModelScope.launch {
            try {
                progressRepo.updateExerciseCode(exercise.id, exercise.category, code)
            } catch (e: Exception) {
                Log.e(TAG, "Error in saveCurrentCode: ${e.message}", e)
            }
        }
    }

    private fun normalizeCode(code: String): String =
        code.replace(Regex("\\s+"), " ")
            .replace(Regex("//.*"), "")
            .trim()

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

    override fun onCleared() {
        super.onCleared()
        categoryObserverJobs.values.forEach { it.cancel() }
        categoryObserverJobs.clear()
    }
}
