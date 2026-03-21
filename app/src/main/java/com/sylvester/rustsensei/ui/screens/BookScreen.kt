package com.sylvester.rustsensei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.sylvester.rustsensei.viewmodel.BookScreenMode
import com.sylvester.rustsensei.viewmodel.BookViewModel
import com.sylvester.rustsensei.viewmodel.LearningPathViewModel
import com.sylvester.rustsensei.viewmodel.ReferenceScreenMode
import com.sylvester.rustsensei.viewmodel.ReferenceViewModel
import com.sylvester.rustsensei.viewmodel.ReviewViewModel

/**
 * Top-level router for the Learn tab.
 *
 * Delegates to focused view composables based on the current [BookScreenMode]:
 *  - [BookIndexView] — chapter list + reference grid
 *  - [BookChapterView] — section list for a single chapter
 *  - [BookSectionView] — full reading experience for a single section
 *
 * Each view lives in its own file for maintainability.
 */
@Composable
fun BookScreen(
    viewModel: BookViewModel,
    referenceViewModel: ReferenceViewModel,
    reviewViewModel: ReviewViewModel? = null,
    learningPathViewModel: LearningPathViewModel? = null,
    onOpenReference: (sectionId: String) -> Unit,
    onAskSensei: (String, String) -> Unit,
    onNavigateToReview: () -> Unit = {},
    onNavigateToLearningPaths: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val refState by referenceViewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.mode != BookScreenMode.INDEX || refState.mode != ReferenceScreenMode.INDEX) {
        if (refState.mode != ReferenceScreenMode.INDEX) referenceViewModel.navigateBack()
        else viewModel.navigateBack()
    }

    uiState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("OK") } }
        )
    }

    refState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { referenceViewModel.clearError() },
            title = { Text("Unavailable") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = { referenceViewModel.clearError() }) { Text("OK") } }
        )
    }

    if (refState.mode != ReferenceScreenMode.INDEX) {
        ReferenceScreen(viewModel = referenceViewModel)
        return
    }

    when (uiState.mode) {
        BookScreenMode.INDEX -> BookIndexView(
            viewModel = viewModel,
            referenceViewModel = referenceViewModel,
            reviewViewModel = reviewViewModel,
            learningPathViewModel = learningPathViewModel,
            onOpenReference = onOpenReference,
            onNavigateToReview = onNavigateToReview,
            onNavigateToLearningPaths = onNavigateToLearningPaths
        )
        BookScreenMode.CHAPTER -> BookChapterView(viewModel = viewModel)
        BookScreenMode.SECTION -> BookSectionView(
            viewModel = viewModel,
            onAskSensei = onAskSensei
        )
    }
}

/** Maps reference section IDs to representative icons. */
internal fun referenceSectionIcon(id: String): ImageVector = when (id) {
    "cheatsheets" -> Icons.Default.Speed
    "compiler-errors" -> Icons.Default.BugReport
    "comparisons" -> Icons.Default.CompareArrows
    "challenges" -> Icons.Default.Code
    "crates" -> Icons.Default.Extension
    "async-rust" -> Icons.Default.Speed
    "cli-guide" -> Icons.Default.Terminal
    "design-patterns" -> Icons.Default.Psychology
    "glossary" -> Icons.Default.MenuBook
    "testing" -> Icons.Default.Lightbulb
    "unsafe-guide" -> Icons.Default.Security
    "interview-prep" -> Icons.Default.Quiz
    else -> Icons.Default.Book
}
