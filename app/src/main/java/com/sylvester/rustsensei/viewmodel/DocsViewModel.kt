package com.sylvester.rustsensei.viewmodel

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.content.ContentProvider
import com.sylvester.rustsensei.content.DocEntry
import com.sylvester.rustsensei.content.DocIndexEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class DocsUiState(
    val docIndex: List<DocIndexEntry> = emptyList(),
    val currentDoc: DocEntry? = null,
    val searchQuery: String = "",
    val filteredIndex: List<DocIndexEntry> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DocsViewModel @Inject constructor(
    private val contentRepo: ContentProvider
) : ViewModel() {

    companion object {
        private const val TAG = "DocsViewModel"
    }

    private val _uiState = MutableStateFlow(DocsUiState())
    val uiState: StateFlow<DocsUiState> = _uiState.asStateFlow()

    init {
        loadIndex()
    }

    private fun loadIndex() {
        viewModelScope.launch {
            try {
                val index = contentRepo.getDocIndex()
                _uiState.value = _uiState.value.copy(
                    docIndex = index,
                    filteredIndex = index,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading doc index: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun openDoc(typeId: String) {
        viewModelScope.launch {
            try {
                val doc = contentRepo.getDoc(typeId)
                if (doc != null) {
                    _uiState.value = _uiState.value.copy(currentDoc = doc)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading doc $typeId: ${e.message}", e)
            }
        }
    }

    fun goBack() {
        _uiState.value = _uiState.value.copy(currentDoc = null)
    }

    fun search(query: String) {
        val trimmed = query.trim()
        val filtered = if (trimmed.isEmpty()) {
            _uiState.value.docIndex
        } else {
            _uiState.value.docIndex.filter { entry ->
                entry.name.contains(trimmed, ignoreCase = true) ||
                    entry.module.contains(trimmed, ignoreCase = true)
            }
        }
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredIndex = filtered
        )
    }
}
