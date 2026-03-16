package com.sylvester.rustsensei.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.content.ContentRepository
import com.sylvester.rustsensei.data.LearningPath
import com.sylvester.rustsensei.data.PathProgress
import com.sylvester.rustsensei.data.ProgressDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PathMode { LIST, DETAIL }

data class PathUiState(
    val paths: List<LearningPath> = emptyList(),
    val selectedPath: LearningPath? = null,
    val stepProgress: Map<String, Boolean> = emptyMap(), // stepId -> completed
    val mode: PathMode = PathMode.LIST
)

@HiltViewModel
class LearningPathViewModel @Inject constructor(
    private val contentRepo: ContentRepository,
    private val progressDao: ProgressDao
) : ViewModel() {

    companion object {
        private const val TAG = "LearningPathViewModel"
    }

    private val _uiState = MutableStateFlow(PathUiState())
    val uiState: StateFlow<PathUiState> = _uiState.asStateFlow()

    init {
        loadPaths()
    }

    private fun loadPaths() {
        viewModelScope.launch {
            try {
                val paths = contentRepo.getLearningPaths()
                _uiState.value = _uiState.value.copy(paths = paths)

                // Observe all path progress to keep list view percentages up to date
                launch {
                    progressDao.observeAllPathProgress().collect { allProgress ->
                        val progressMap = allProgress.associate { it.stepId to it.isCompleted }
                        _uiState.value = _uiState.value.copy(stepProgress = progressMap)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading learning paths: ${e.message}", e)
            }
        }
    }

    fun selectPath(pathId: String) {
        viewModelScope.launch {
            try {
                val path = contentRepo.getLearningPath(pathId) ?: return@launch
                _uiState.value = _uiState.value.copy(
                    selectedPath = path,
                    mode = PathMode.DETAIL
                )

                // Load progress for this specific path
                val progress = progressDao.getPathProgress(pathId)
                val progressMap = _uiState.value.stepProgress.toMutableMap()
                progress.forEach { progressMap[it.stepId] = it.isCompleted }
                _uiState.value = _uiState.value.copy(stepProgress = progressMap)
            } catch (e: Exception) {
                Log.e(TAG, "Error selecting path: ${e.message}", e)
            }
        }
    }

    fun markStepComplete(pathId: String, stepId: String) {
        viewModelScope.launch {
            try {
                val compositeId = "$pathId:$stepId"
                val progress = PathProgress(
                    stepId = compositeId,
                    pathId = pathId,
                    isCompleted = true,
                    completedAt = System.currentTimeMillis()
                )
                progressDao.upsertPathProgress(progress)

                // Update local state immediately for responsiveness
                val updatedProgress = _uiState.value.stepProgress.toMutableMap()
                updatedProgress[compositeId] = true
                _uiState.value = _uiState.value.copy(stepProgress = updatedProgress)
            } catch (e: Exception) {
                Log.e(TAG, "Error marking step complete: ${e.message}", e)
            }
        }
    }

    fun navigateBack() {
        _uiState.value = _uiState.value.copy(
            selectedPath = null,
            mode = PathMode.LIST
        )
    }

    /** Calculate completion percentage for a given path from current stepProgress map. */
    fun getPathCompletionPercent(path: LearningPath): Float {
        if (path.steps.isEmpty()) return 0f
        val completed = path.steps.count { step ->
            _uiState.value.stepProgress["${path.id}:${step.id}"] == true
        }
        return completed.toFloat() / path.steps.size
    }
}
