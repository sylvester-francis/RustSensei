package com.sylvester.rustsensei.viewmodel

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.content.ContentProvider
import com.sylvester.rustsensei.content.Project
import com.sylvester.rustsensei.content.ProjectStep
import com.sylvester.rustsensei.data.ProjectProgress
import com.sylvester.rustsensei.data.ProgressDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@Immutable
data class ProjectUiState(
    val projects: List<Project> = emptyList(),
    val currentProject: Project? = null,
    val currentStepIndex: Int = 0,
    val userCode: String = "",
    val completedStepIds: Set<String> = emptySet(),
    val isLoading: Boolean = true
) {
    val currentStep: ProjectStep?
        get() = currentProject?.steps?.getOrNull(currentStepIndex)
    val totalSteps: Int
        get() = currentProject?.steps?.size ?: 0
}

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val contentProvider: ContentProvider,
    private val progressDao: ProgressDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    init {
        loadProjects()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            try {
                val projects = loadProjectsFromAssets()
                _uiState.value = _uiState.value.copy(
                    projects = projects,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("ProjectVM", "Error loading projects: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun loadProjectsFromAssets(): List<Project> {
        val indexJson = contentProvider.loadProjectJson( "index") ?: return emptyList()
        val arr = indexJson.optJSONArray("projects") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val entry = arr.getJSONObject(i)
            loadProject(entry.getString("id"))
        }
    }

    private suspend fun loadProject(id: String): Project? {
        return try {
            val json = contentProvider.loadProjectJson( id) ?: return null
            Project(
                id = json.getString("id"),
                title = json.getString("title"),
                description = json.getString("description"),
                difficulty = json.getString("difficulty"),
                estimatedHours = json.getInt("estimatedHours"),
                concepts = (0 until json.getJSONArray("concepts").length()).map {
                    json.getJSONArray("concepts").getString(it)
                },
                steps = parseSteps(json)
            )
        } catch (e: Exception) {
            Log.e("ProjectVM", "Error loading project $id: ${e.message}", e)
            null
        }
    }

    private fun parseSteps(json: JSONObject): List<ProjectStep> {
        val arr = json.optJSONArray("steps") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val step = arr.getJSONObject(i)
            ProjectStep(
                id = step.getString("id"),
                title = step.getString("title"),
                instructions = step.getString("instructions"),
                starterCode = step.getString("starterCode"),
                relatedChapter = step.optString("relatedChapter").takeIf { it.isNotBlank() }
            )
        }
    }

    fun openProject(project: Project) {
        viewModelScope.launch {
            val progress = progressDao.getProjectProgress(project.id)
            val completedIds = progress.filter { it.isCompleted }.map { it.stepId }.toSet()
            _uiState.value = _uiState.value.copy(
                currentProject = project,
                currentStepIndex = 0,
                userCode = project.steps.firstOrNull()?.starterCode ?: "",
                completedStepIds = completedIds
            )
        }
    }

    fun selectStep(index: Int) {
        val step = _uiState.value.currentProject?.steps?.getOrNull(index) ?: return
        _uiState.value = _uiState.value.copy(
            currentStepIndex = index,
            userCode = step.starterCode
        )
    }

    fun updateCode(code: String) {
        _uiState.value = _uiState.value.copy(userCode = code)
    }

    fun completeStep() {
        val project = _uiState.value.currentProject ?: return
        val step = _uiState.value.currentStep ?: return

        viewModelScope.launch {
            progressDao.upsertProjectProgress(
                ProjectProgress(
                    id = "${project.id}_${step.id}",
                    projectId = project.id,
                    stepId = step.id,
                    isCompleted = true,
                    userCode = _uiState.value.userCode,
                    completedAt = System.currentTimeMillis()
                )
            )
            _uiState.value = _uiState.value.copy(
                completedStepIds = _uiState.value.completedStepIds + step.id
            )
        }
    }

    fun goBack() {
        _uiState.value = _uiState.value.copy(
            currentProject = null,
            currentStepIndex = 0,
            userCode = ""
        )
    }
}
