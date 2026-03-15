package com.sylvester.rustsensei.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.RustSenseiApplication
import com.sylvester.rustsensei.content.ReferenceSectionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class ReferenceScreenMode {
    INDEX,
    SECTION_LIST,
    ITEM_DETAIL
}

data class ReferenceUiState(
    val mode: ReferenceScreenMode = ReferenceScreenMode.INDEX,
    val sections: List<ReferenceSectionInfo> = emptyList(),
    val currentSection: ReferenceSectionInfo? = null,
    val currentItemId: String? = null,
    val currentItemJson: JSONObject? = null,
    val errorMessage: String? = null
)

class ReferenceViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RustSenseiApplication
    private val contentRepo = app.contentRepository

    private val _uiState = MutableStateFlow(ReferenceUiState())
    val uiState: StateFlow<ReferenceUiState> = _uiState.asStateFlow()

    init {
        loadIndex()
    }

    private fun loadIndex() {
        viewModelScope.launch {
            val index = withContext(Dispatchers.IO) {
                contentRepo.getReferenceIndex()
            }
            _uiState.value = _uiState.value.copy(sections = index.sections)
        }
    }

    fun openSection(sectionId: String) {
        val section = _uiState.value.sections.find { it.id == sectionId } ?: return
        _uiState.value = _uiState.value.copy(
            mode = ReferenceScreenMode.SECTION_LIST,
            currentSection = section,
            errorMessage = null
        )
    }

    fun openItem(sectionId: String, itemId: String) {
        viewModelScope.launch {
            val json = withContext(Dispatchers.IO) {
                contentRepo.getReferenceItem(sectionId, itemId)
            }
            if (json != null) {
                _uiState.value = _uiState.value.copy(
                    mode = ReferenceScreenMode.ITEM_DETAIL,
                    currentItemId = itemId,
                    currentItemJson = json,
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Content not available yet"
                )
            }
        }
    }

    fun navigateBack() {
        when (_uiState.value.mode) {
            ReferenceScreenMode.ITEM_DETAIL -> {
                _uiState.value = _uiState.value.copy(
                    mode = ReferenceScreenMode.SECTION_LIST,
                    currentItemId = null,
                    currentItemJson = null
                )
            }
            ReferenceScreenMode.SECTION_LIST -> {
                _uiState.value = _uiState.value.copy(
                    mode = ReferenceScreenMode.INDEX,
                    currentSection = null
                )
            }
            ReferenceScreenMode.INDEX -> {}
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
