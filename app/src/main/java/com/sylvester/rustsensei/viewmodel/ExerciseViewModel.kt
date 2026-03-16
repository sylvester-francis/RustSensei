package com.sylvester.rustsensei.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.content.ContentRepository
import com.sylvester.rustsensei.content.ExerciseCategory
import com.sylvester.rustsensei.content.ExerciseData
import com.sylvester.rustsensei.data.ExerciseProgress
import com.sylvester.rustsensei.data.PreferencesManager
import com.sylvester.rustsensei.data.ProgressRepository
import com.sylvester.rustsensei.llm.ChatTemplateFormatter
import com.sylvester.rustsensei.llm.InferenceConfig
import com.sylvester.rustsensei.llm.InferenceEngine
import com.sylvester.rustsensei.llm.LiteRtEngine
import com.sylvester.rustsensei.llm.ModelManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

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
    val lastIncompleteExerciseId: String? = null,
    val llmValidationResult: String = "",
    val isValidating: Boolean = false,
    val errorMessage: String? = null
)

class ExerciseViewModel(
    private val contentRepo: ContentRepository,
    private val progressRepo: ProgressRepository,
    private val liteRtEngine: LiteRtEngine,
    private val prefsManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "ExerciseViewModel"
    }

    private fun getActiveEngine(): InferenceEngine {
        val modelId = prefsManager.getSelectedModelId()
        val model = ModelManager.getModelById(modelId)
        return liteRtEngine
    }

    private var validationJob: Job? = null

    // Bug 4: track category observer jobs so we can cancel previous collectors
    // when a category is re-expanded, preventing coroutine leaks
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

    // Design Concern #3: load last incomplete exercise for "Continue" flow
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

    // Fix #5: load progress on-demand when a category is expanded, not all at init
    fun toggleCategory(categoryId: String) {
        val current = _uiState.value.expandedCategory
        val newExpanded = if (current == categoryId) null else categoryId
        _uiState.value = _uiState.value.copy(expandedCategory = newExpanded)

        if (newExpanded != null) {
            // Bug 4: cancel any existing observer for this category before starting a new one
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
        // Bug 4: store the job so it can be cancelled on re-expand or cleanup
        categoryObserverJobs[categoryId] = job
    }

    // Refresh progress for the current category after completing an exercise
    private fun refreshCurrentCategoryProgress() {
        val exercise = _uiState.value.currentExercise ?: return
        val categoryId = exercise.category
        // Bug 4: cancel the existing observer before re-fetching
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
                    _uiState.value = _uiState.value.copy(
                        mode = ExerciseScreenMode.DETAIL,
                        currentExercise = exercise,
                        currentProgress = progress,
                        userCode = progress?.userCode?.ifEmpty { exercise.starterCode } ?: exercise.starterCode,
                        hintsRevealed = progress?.hintsViewed ?: 0,
                        showSolution = false,
                        checkResult = null,
                        llmValidationResult = "",
                        isValidating = false
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
        _uiState.value = _uiState.value.copy(
            mode = ExerciseScreenMode.CATEGORIES,
            currentExercise = null,
            currentProgress = null,
            checkResult = null,
            showSolution = false,
            llmValidationResult = "",
            isValidating = false
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
            try {
                progressRepo.markExerciseComplete(exercise.id, exercise.category)
            } catch (e: Exception) {
                Log.e(TAG, "Error in markCurrentExerciseCorrect: ${e.message}", e)
            }
        }
    }

    fun validateWithLlm() {
        val exercise = _uiState.value.currentExercise ?: return
        val userCode = _uiState.value.userCode.trim()

        if (_uiState.value.isValidating) return
        val activeEngine = getActiveEngine()
        if (!activeEngine.isModelLoaded()) return

        validationJob?.cancel()

        val prompt = ChatTemplateFormatter.formatExerciseValidation(
            exerciseDescription = exercise.description,
            exerciseInstructions = exercise.instructions,
            expectedSolution = exercise.solution,
            studentCode = userCode
        )

        _uiState.value = _uiState.value.copy(
            isValidating = true,
            llmValidationResult = ""
        )

        val config = InferenceConfig(
            temperature = 0.3f,
            topP = 0.9f,
            maxTokens = 256,
            contextLength = 2048
        )

        val tokenBuffer = StringBuilder()

        validationJob = viewModelScope.launch {
            try {
                activeEngine.generate(prompt, config)
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            isValidating = false,
                            llmValidationResult = "Error: ${e.message}"
                        )
                    }
                    .onCompletion {
                        val finalText = ChatTemplateFormatter.stripThinkTags(
                            tokenBuffer.toString()
                        ).trim()
                        _uiState.value = _uiState.value.copy(
                            isValidating = false,
                            llmValidationResult = finalText
                        )
                        if (finalText.uppercase().startsWith("CORRECT")) {
                            _uiState.value = _uiState.value.copy(checkResult = "correct")
                            progressRepo.markExerciseComplete(exercise.id, exercise.category)
                            refreshCurrentCategoryProgress()
                        }
                    }
                    .collect { token ->
                        tokenBuffer.append(token)
                        val displayText = ChatTemplateFormatter.stripThinkTags(
                            tokenBuffer.toString()
                        )
                        _uiState.value = _uiState.value.copy(
                            llmValidationResult = displayText
                        )
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in validateWithLlm: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isValidating = false,
                    llmValidationResult = "Error: ${e.message}"
                )
            }
        }
    }

    fun stopValidation() {
        validationJob?.cancel()
        _uiState.value = _uiState.value.copy(isValidating = false)
    }

    fun resetExercise() {
        val exercise = _uiState.value.currentExercise ?: return
        validationJob?.cancel()
        _uiState.value = _uiState.value.copy(
            userCode = exercise.starterCode,
            checkResult = null,
            showSolution = false,
            llmValidationResult = "",
            isValidating = false
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
            try {
                progressRepo.updateExerciseCode(exercise.id, exercise.category, code)
            } catch (e: Exception) {
                Log.e(TAG, "Error in saveCurrentCode: ${e.message}", e)
            }
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

    override fun onCleared() {
        super.onCleared()
        // Bug 4: cancel all category observer jobs on cleanup
        categoryObserverJobs.values.forEach { it.cancel() }
        categoryObserverJobs.clear()
    }
}
