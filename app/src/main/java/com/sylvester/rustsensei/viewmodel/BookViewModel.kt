package com.sylvester.rustsensei.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylvester.rustsensei.content.BookChapter
import com.sylvester.rustsensei.content.BookIndexEntry
import com.sylvester.rustsensei.content.BookSection
import com.sylvester.rustsensei.content.ContentRepository
import com.sylvester.rustsensei.data.BookProgress
import com.sylvester.rustsensei.data.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class BookScreenMode {
    INDEX,
    CHAPTER,
    SECTION
}

data class BookUiState(
    val mode: BookScreenMode = BookScreenMode.INDEX,
    val chapters: List<BookIndexEntry> = emptyList(),
    val currentChapter: BookChapter? = null,
    val currentSection: BookSection? = null,
    val currentChapterId: String? = null,
    val currentSectionId: String? = null,
    val sectionProgress: Map<String, BookProgress> = emptyMap(),
    val readProgress: Float = 0f,
    val sectionMarkedComplete: Boolean = false,
    val lastReadChapterId: String? = null,
    val lastReadSectionId: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class BookViewModel @Inject constructor(
    private val contentRepo: ContentRepository,
    private val progressRepo: ProgressRepository
) : ViewModel() {

    companion object {
        private const val TAG = "BookViewModel"
    }

    private val _uiState = MutableStateFlow(BookUiState())
    val uiState: StateFlow<BookUiState> = _uiState.asStateFlow()

    // Fix #6: track the chapter progress observer job so we can cancel it
    private var chapterProgressJob: Job? = null

    // Fix #8: track section read time
    private var sectionEnteredAt: Long = 0L

    init {
        loadBookIndex()
        loadLastRead()
    }

    private fun loadBookIndex() {
        viewModelScope.launch {
            try {
                val index = withContext(Dispatchers.IO) {
                    contentRepo.getBookIndex()
                }
                _uiState.value = _uiState.value.copy(chapters = index.chapters)
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadBookIndex: ${e.message}", e)
            }
        }
    }

    // Design Concern #3: load last read section for "Continue Reading" flow
    private fun loadLastRead() {
        viewModelScope.launch {
            try {
                val last = progressRepo.getLastReadSection()
                if (last != null) {
                    _uiState.value = _uiState.value.copy(
                        lastReadChapterId = last.chapterId,
                        lastReadSectionId = last.sectionId
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadLastRead: ${e.message}", e)
            }
        }
    }

    fun openChapter(chapterId: String) {
        viewModelScope.launch {
            try {
                val chapter = withContext(Dispatchers.IO) {
                    contentRepo.getChapter(chapterId)
                }
                if (chapter == null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to load chapter. The content may be corrupted."
                    )
                    return@launch
                }
                _uiState.value = _uiState.value.copy(
                    mode = BookScreenMode.CHAPTER,
                    currentChapter = chapter,
                    currentChapterId = chapterId,
                    errorMessage = null
                )
                observeChapterProgress(chapterId)
            } catch (e: Exception) {
                Log.e(TAG, "Error in openChapter: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load chapter: ${e.message}"
                )
            }
        }
    }

    fun openSection(chapterId: String, sectionId: String) {
        saveReadTimeForCurrentSection()

        viewModelScope.launch {
            try {
                val section = withContext(Dispatchers.IO) {
                    contentRepo.getSection(chapterId, sectionId)
                }
                if (section == null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to load section content."
                    )
                    return@launch
                }
                _uiState.value = _uiState.value.copy(
                    mode = BookScreenMode.SECTION,
                    currentSection = section,
                    currentSectionId = sectionId,
                    currentChapterId = chapterId,
                    readProgress = 0f,
                    sectionMarkedComplete = false,
                    errorMessage = null
                )
                sectionEnteredAt = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e(TAG, "Error in openSection: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load section: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun navigateBack() {
        val state = _uiState.value
        when (state.mode) {
            BookScreenMode.SECTION -> {
                // Fix #8: save read time when leaving section
                saveReadTimeForCurrentSection()
                _uiState.value = state.copy(
                    mode = BookScreenMode.CHAPTER,
                    currentSection = null,
                    currentSectionId = null,
                    sectionMarkedComplete = false
                )
                // Bug 9: refresh the "Continue Reading" card so it reflects
                // the section the user just left, not the stale init-time value.
                loadLastRead()
            }
            BookScreenMode.CHAPTER -> {
                _uiState.value = state.copy(
                    mode = BookScreenMode.INDEX,
                    currentChapter = null,
                    currentChapterId = null
                )
                // Bug 9: also refresh when returning to the index from chapter view
                loadLastRead()
            }
            BookScreenMode.INDEX -> { /* already at root */ }
        }
    }

    fun updateReadProgress(percent: Float) {
        val sectionId = _uiState.value.currentSectionId ?: return
        val chapterId = _uiState.value.currentChapterId ?: return
        _uiState.value = _uiState.value.copy(readProgress = percent)
        viewModelScope.launch {
            try {
                progressRepo.updateReadProgress(sectionId, chapterId, percent)
            } catch (e: Exception) {
                Log.e(TAG, "Error in updateReadProgress: ${e.message}", e)
            }
        }
    }

    // Fix #1: only mark complete once per section visit
    fun markSectionComplete() {
        if (_uiState.value.sectionMarkedComplete) return
        val sectionId = _uiState.value.currentSectionId ?: return
        val chapterId = _uiState.value.currentChapterId ?: return
        _uiState.value = _uiState.value.copy(sectionMarkedComplete = true)
        viewModelScope.launch {
            try {
                progressRepo.markSectionComplete(sectionId, chapterId)
            } catch (e: Exception) {
                Log.e(TAG, "Error in markSectionComplete: ${e.message}", e)
            }
        }
    }

    fun toggleBookmark() {
        val sectionId = _uiState.value.currentSectionId ?: return
        val chapterId = _uiState.value.currentChapterId ?: return
        viewModelScope.launch {
            try {
                progressRepo.toggleBookmark(sectionId, chapterId)
            } catch (e: Exception) {
                Log.e(TAG, "Error in toggleBookmark: ${e.message}", e)
            }
        }
    }

    fun navigateToNextSection() {
        val state = _uiState.value
        val chapter = state.currentChapter ?: return
        val chapterId = state.currentChapterId ?: return
        val currentIdx = chapter.sections.indexOfFirst { it.id == state.currentSectionId }
        if (currentIdx < chapter.sections.lastIndex) {
            val next = chapter.sections[currentIdx + 1]
            openSection(chapterId, next.id)
        }
    }

    fun navigateToPreviousSection() {
        val state = _uiState.value
        val chapter = state.currentChapter ?: return
        val chapterId = state.currentChapterId ?: return
        val currentIdx = chapter.sections.indexOfFirst { it.id == state.currentSectionId }
        if (currentIdx > 0) {
            val prev = chapter.sections[currentIdx - 1]
            openSection(chapterId, prev.id)
        }
    }

    fun getSectionContent(): String {
        return _uiState.value.currentSection?.content ?: ""
    }

    fun getCurrentSectionId(): String {
        return _uiState.value.currentSectionId ?: ""
    }

    // Fix #8: save accumulated read time
    private fun saveReadTimeForCurrentSection() {
        if (sectionEnteredAt <= 0L) return
        val sectionId = _uiState.value.currentSectionId ?: return
        val chapterId = _uiState.value.currentChapterId ?: return
        val elapsed = (System.currentTimeMillis() - sectionEnteredAt) / 1000L
        if (elapsed > 2) { // only save if spent more than 2 seconds
            viewModelScope.launch {
                try {
                    progressRepo.addReadTime(sectionId, chapterId, elapsed)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in saveReadTime: ${e.message}", e)
                }
            }
        }
        sectionEnteredAt = 0L
    }

    // Fix #6: cancel previous observer before starting a new one
    private fun observeChapterProgress(chapterId: String) {
        chapterProgressJob?.cancel()
        chapterProgressJob = viewModelScope.launch {
            try {
                progressRepo.getChapterProgress(chapterId).collect { progressList ->
                    val progressMap = progressList.associateBy { it.sectionId }
                    _uiState.value = _uiState.value.copy(sectionProgress = progressMap)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in observeChapterProgress: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveReadTimeForCurrentSection()
    }
}
