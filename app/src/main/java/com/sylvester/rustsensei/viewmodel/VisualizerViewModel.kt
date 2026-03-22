package com.sylvester.rustsensei.viewmodel

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.content.AllocationStatus
import com.sylvester.rustsensei.content.ContentProvider
import com.sylvester.rustsensei.content.HeapAllocation
import com.sylvester.rustsensei.content.OwnershipScenario
import com.sylvester.rustsensei.content.StackVariable
import com.sylvester.rustsensei.content.VariableStatus
import com.sylvester.rustsensei.content.VisualizationStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@Immutable
data class VisualizerUiState(
    val scenarios: List<OwnershipScenario> = emptyList(),
    val currentScenario: OwnershipScenario? = null,
    val currentStepIndex: Int = 0,
    val isLoading: Boolean = true
) {
    val currentStep: VisualizationStep?
        get() = currentScenario?.steps?.getOrNull(currentStepIndex)
    val totalSteps: Int
        get() = currentScenario?.steps?.size ?: 0
    val hasNext: Boolean
        get() = currentStepIndex < totalSteps - 1
    val hasPrevious: Boolean
        get() = currentStepIndex > 0
}

@HiltViewModel
class VisualizerViewModel @Inject constructor(
    private val contentProvider: ContentProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisualizerUiState())
    val uiState: StateFlow<VisualizerUiState> = _uiState.asStateFlow()

    init {
        loadScenarios()
    }

    private fun loadScenarios() {
        viewModelScope.launch {
            try {
                val indexJson = contentProvider.loadVisualizationJson("index")
                val scenarios = parseIndex(indexJson)
                _uiState.value = _uiState.value.copy(
                    scenarios = scenarios,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("VisualizerVM", "Error loading scenarios: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun parseIndex(indexJson: JSONObject?): List<OwnershipScenario> {
        if (indexJson == null) return emptyList()
        val arr = indexJson.optJSONArray("scenarios") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val entry = arr.getJSONObject(i)
            val id = entry.getString("id")
            loadScenario(id)
        }
    }

    private suspend fun loadScenario(id: String): OwnershipScenario? {
        return try {
            val json = contentProvider.loadVisualizationJson(id) ?: return null
            OwnershipScenario(
                id = json.getString("id"),
                title = json.getString("title"),
                description = json.getString("description"),
                relatedChapter = json.getString("relatedChapter"),
                steps = parseSteps(json)
            )
        } catch (e: Exception) {
            Log.e("VisualizerVM", "Error loading scenario $id: ${e.message}", e)
            null
        }
    }

    private fun parseSteps(json: JSONObject): List<VisualizationStep> {
        val arr = json.optJSONArray("steps") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val step = arr.getJSONObject(i)
            VisualizationStep(
                label = step.getString("label"),
                code = step.getString("code"),
                stackVariables = parseStackVars(step),
                heapAllocations = parseHeapAllocs(step),
                annotation = step.getString("annotation")
            )
        }
    }

    private fun parseStackVars(step: JSONObject): List<StackVariable> {
        val arr = step.optJSONArray("stackVariables") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val v = arr.getJSONObject(i)
            StackVariable(
                name = v.getString("name"),
                type = v.getString("type"),
                status = VariableStatus.valueOf(v.getString("status")),
                pointsTo = v.optString("pointsTo").takeIf { it.isNotBlank() && it != "null" }
            )
        }
    }

    private fun parseHeapAllocs(step: JSONObject): List<HeapAllocation> {
        val arr = step.optJSONArray("heapAllocations") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val a = arr.getJSONObject(i)
            HeapAllocation(
                id = a.getString("id"),
                value = a.getString("value"),
                type = a.getString("type"),
                status = AllocationStatus.valueOf(a.getString("status"))
            )
        }
    }

    fun openScenario(scenario: OwnershipScenario) {
        _uiState.value = _uiState.value.copy(
            currentScenario = scenario,
            currentStepIndex = 0
        )
    }

    fun nextStep() {
        if (_uiState.value.hasNext) {
            _uiState.value = _uiState.value.copy(
                currentStepIndex = _uiState.value.currentStepIndex + 1
            )
        }
    }

    fun previousStep() {
        if (_uiState.value.hasPrevious) {
            _uiState.value = _uiState.value.copy(
                currentStepIndex = _uiState.value.currentStepIndex - 1
            )
        }
    }

    fun goBack() {
        _uiState.value = _uiState.value.copy(
            currentScenario = null,
            currentStepIndex = 0
        )
    }
}
