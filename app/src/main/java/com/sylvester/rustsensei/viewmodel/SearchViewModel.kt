package com.sylvester.rustsensei.viewmodel

import androidx.compose.runtime.Immutable
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.content.ContentRepository
import com.sylvester.rustsensei.data.PreferencesManager
import com.sylvester.rustsensei.data.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchResult(
    val title: String,
    val subtitle: String,
    val type: String, // "section", "exercise", "reference", "glossary"
    val id: String,
    val matchSnippet: String = ""
)

@Immutable
data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val recentSearches: List<String> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val contentRepo: ContentRepository,
    private val progressRepo: ProgressRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "SearchViewModel"
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadRecentSearches()
    }

    private fun loadRecentSearches() {
        _uiState.value = _uiState.value.copy(
            recentSearches = preferencesManager.getRecentSearches()
        )
    }

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isSearching = false)
            return
        }
        // Debounce search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            search(query)
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(query = "", results = emptyList(), isSearching = false)
    }

    fun selectRecentSearch(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            search(query)
        }
    }

    private suspend fun search(query: String) {
        _uiState.value = _uiState.value.copy(isSearching = true)
        try {
            val results = mutableListOf<SearchResult>()
            val lowerQuery = query.lowercase()

            // Search book sections
            try {
                val bookIndex = contentRepo.getBookIndex()
                for (chapter in bookIndex.chapters) {
                    for (i in chapter.sectionIds.indices) {
                        val sectionId = chapter.sectionIds[i]
                        val sectionTitle = chapter.sectionTitles.getOrElse(i) { sectionId }
                        if (sectionTitle.lowercase().contains(lowerQuery) ||
                            sectionId.lowercase().contains(lowerQuery)
                        ) {
                            results.add(
                                SearchResult(
                                    title = sectionTitle,
                                    subtitle = chapter.title,
                                    type = "section",
                                    id = "${chapter.id}/$sectionId",
                                    matchSnippet = "Chapter: ${chapter.title}"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching book index: ${e.message}", e)
            }

            // Search exercises
            try {
                val categories = contentRepo.getExerciseCategories()
                for (category in categories) {
                    if (category.title.lowercase().contains(lowerQuery)) {
                        results.add(
                            SearchResult(
                                title = category.title,
                                subtitle = category.description,
                                type = "exercise",
                                id = category.id,
                                matchSnippet = "${category.exercises.size} exercises"
                            )
                        )
                    }
                    for (exerciseId in category.exercises) {
                        if (exerciseId.lowercase().contains(lowerQuery)) {
                            results.add(
                                SearchResult(
                                    title = exerciseId.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    subtitle = category.title,
                                    type = "exercise",
                                    id = exerciseId,
                                    matchSnippet = "Difficulty: ${category.description}"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching exercises: ${e.message}", e)
            }

            // Search reference sections
            try {
                val refIndex = contentRepo.getReferenceIndex()
                for (section in refIndex.sections) {
                    if (section.title.lowercase().contains(lowerQuery) ||
                        section.description.lowercase().contains(lowerQuery)
                    ) {
                        results.add(
                            SearchResult(
                                title = section.title,
                                subtitle = section.description,
                                type = "reference",
                                id = section.id,
                                matchSnippet = "${section.items.size} items"
                            )
                        )
                    }
                    for (item in section.items) {
                        if (item.lowercase().contains(lowerQuery)) {
                            results.add(
                                SearchResult(
                                    title = item.replace("-", " ").replaceFirstChar { it.uppercase() },
                                    subtitle = section.title,
                                    type = "reference",
                                    id = "${section.id}/$item",
                                    matchSnippet = "In: ${section.title}"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching reference: ${e.message}", e)
            }

            // Search glossary terms
            try {
                val glossaryJson = contentRepo.getReferenceItem("glossary", "glossary")
                if (glossaryJson != null && glossaryJson.has("terms")) {
                    val terms = glossaryJson.getJSONArray("terms")
                    for (i in 0 until terms.length()) {
                        val term = terms.getJSONObject(i)
                        val termName = term.getString("term")
                        val definition = term.getString("definition")
                        if (termName.lowercase().contains(lowerQuery) ||
                            definition.lowercase().contains(lowerQuery)
                        ) {
                            val snippet = if (definition.length > 80) {
                                definition.substring(0, 80) + "..."
                            } else definition
                            results.add(
                                SearchResult(
                                    title = termName,
                                    subtitle = "Glossary",
                                    type = "glossary",
                                    id = "glossary/$termName",
                                    matchSnippet = snippet
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching glossary: ${e.message}", e)
            }

            // Search user notes
            try {
                val notes = progressRepo.searchNotes(query)
                for (note in notes.take(5)) {
                    val snippet = if (note.content.length > 80) {
                        note.content.substring(0, 80) + "..."
                    } else note.content
                    results.add(
                        SearchResult(
                            title = "Note: ${note.sectionId.replace("-", " ")}",
                            subtitle = "Your note",
                            type = "section",
                            id = note.sectionId,
                            matchSnippet = snippet
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching notes: ${e.message}", e)
            }

            // Save to recent searches
            if (query.isNotBlank()) {
                val recent = _uiState.value.recentSearches.toMutableList()
                recent.remove(query)
                recent.add(0, query)
                val trimmed = recent.take(10)
                preferencesManager.saveRecentSearches(trimmed)
                _uiState.value = _uiState.value.copy(recentSearches = trimmed)
            }

            _uiState.value = _uiState.value.copy(
                results = results.take(20),
                isSearching = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in search: ${e.message}", e)
            _uiState.value = _uiState.value.copy(isSearching = false)
        }
    }

    fun clearRecentSearches() {
        preferencesManager.saveRecentSearches(emptyList())
        _uiState.value = _uiState.value.copy(recentSearches = emptyList())
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}
