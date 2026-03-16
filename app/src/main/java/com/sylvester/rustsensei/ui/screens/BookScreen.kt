package com.sylvester.rustsensei.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.ui.components.BookContentRenderer
import com.sylvester.rustsensei.ui.components.CodeBlock
import com.sylvester.rustsensei.ui.theme.DarkSurfaceContainerHigh
import com.sylvester.rustsensei.ui.theme.SuccessGreen
import com.sylvester.rustsensei.ui.theme.NeonCyan
import com.sylvester.rustsensei.viewmodel.BookScreenMode
import com.sylvester.rustsensei.viewmodel.BookViewModel
import com.sylvester.rustsensei.viewmodel.LearningPathViewModel
import com.sylvester.rustsensei.viewmodel.ReferenceScreenMode
import com.sylvester.rustsensei.viewmodel.ReferenceViewModel
import com.sylvester.rustsensei.viewmodel.ReviewViewModel
import kotlinx.coroutines.launch

private val CardShape = RoundedCornerShape(12.dp)
private val CardColor = DarkSurfaceContainerHigh

private fun referenceSectionIcon(id: String): ImageVector = when (id) {
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
        if (refState.mode != ReferenceScreenMode.INDEX) {
            referenceViewModel.navigateBack()
        } else {
            viewModel.navigateBack()
        }
    }

    // Book error dialog
    uiState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    // Reference error dialog
    refState.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { referenceViewModel.clearError() },
            title = { Text("Unavailable") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { referenceViewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    // If reference is navigated into (SECTION_LIST or ITEM_DETAIL), show ReferenceScreen content inline
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
        BookScreenMode.CHAPTER -> ChapterView(viewModel = viewModel)
        BookScreenMode.SECTION -> SectionView(
            viewModel = viewModel,
            onAskSensei = onAskSensei
        )
    }
}

// ── BookIndexView ────────────────────────────────────────────────────────────

@Composable
private fun BookIndexView(
    viewModel: BookViewModel,
    referenceViewModel: ReferenceViewModel,
    reviewViewModel: ReviewViewModel? = null,
    learningPathViewModel: LearningPathViewModel? = null,
    onOpenReference: (sectionId: String) -> Unit,
    onNavigateToReview: () -> Unit = {},
    onNavigateToLearningPaths: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val refState by referenceViewModel.uiState.collectAsState()
    val reviewUiState = reviewViewModel?.uiState?.collectAsState()?.value
    val pathUiState = learningPathViewModel?.uiState?.collectAsState()?.value
    var showingReference by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Mode toggle strip ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Rust Book tab
            val bookSelected = !showingReference
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(CardShape)
                    .background(
                        if (bookSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        else CardColor
                    )
                    .then(
                        if (bookSelected) Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                            CardShape
                        ) else Modifier
                    )
                    .clickable { showingReference = false }
                    .semantics { contentDescription = "Rust Book tab" },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = if (bookSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rust Book",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (bookSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (bookSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Reference tab
            val refSelected = showingReference
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(CardShape)
                    .background(
                        if (refSelected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                        else CardColor
                    )
                    .then(
                        if (refSelected) Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.30f),
                            CardShape
                        ) else Modifier
                    )
                    .clickable { showingReference = true }
                    .semantics { contentDescription = "Reference tab" },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = if (refSelected) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reference",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (refSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (refSelected) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Neon divider below toggle strip
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )

        if (!showingReference) {
            // ── BOOK MODE ──
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Continue Reading card
                val lastChapterId = uiState.lastReadChapterId
                val lastSectionId = uiState.lastReadSectionId
                if (lastChapterId != null && lastSectionId != null) {
                    item(key = "continue-reading") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clickable {
                                    viewModel.openChapter(lastChapterId)
                                    viewModel.openSection(lastChapterId, lastSectionId)
                                },
                            shape = CardShape,
                            colors = CardDefaults.cardColors(containerColor = CardColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                            )
                                        ),
                                        shape = CardShape
                                    )
                                    .padding(16.dp)
                                    .semantics {
                                        contentDescription =
                                            "Continue reading: ${lastSectionId.replace("-", " ")}"
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Continue Reading",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = lastSectionId.replace("-", " "),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // ── Review Card (from Learn section) ──
                if (reviewUiState != null && reviewUiState.dueCardCount > 0) {
                    item(key = "review-card") {
                        Card(
                            onClick = onNavigateToReview,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(containerColor = CardColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${reviewUiState.dueCardCount} card${if (reviewUiState.dueCardCount != 1) "s" else ""} due",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tap to start review",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // ── Learning Paths Card (from Learn section) ──
                if (pathUiState != null && pathUiState.paths.isNotEmpty()) {
                    item(key = "learning-paths-card") {
                        val activePath = pathUiState.paths.firstOrNull { path ->
                            val completed = path.steps.count { step ->
                                pathUiState.stepProgress["${path.id}:${step.id}"] == true
                            }
                            completed > 0 && completed < path.steps.size
                        }
                        Card(
                            onClick = onNavigateToLearningPaths,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(containerColor = CardColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AutoStories,
                                    contentDescription = "Learning paths",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    if (activePath != null) {
                                        val completedSteps = activePath.steps.count { step ->
                                            pathUiState.stepProgress["${activePath.id}:${step.id}"] == true
                                        }
                                        Text(
                                            text = activePath.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$completedSteps of ${activePath.steps.size} steps",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    } else {
                                        Text(
                                            text = "Learning Paths",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Choose a guided path",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Empty state if no chapters loaded
                if (uiState.chapters.isEmpty()) {
                    item(key = "empty-chapters") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No chapters available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Content may still be loading",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // Chapter list
                itemsIndexed(uiState.chapters, key = { _, ch -> ch.id }) { index, chapter ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { viewModel.openChapter(chapter.id) }
                            .semantics {
                                contentDescription =
                                    "Chapter ${index + 1}: ${chapter.title}, ${chapter.sectionIds.size} sections"
                            },
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = CardColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Chapter number badge
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${chapter.sectionIds.size} sections",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                item(key = "bottom-spacer-book") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            // ── REFERENCE MODE (grid) ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Quick Reference",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${refState.sections.size} guides",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 14.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(refState.sections, key = { "ref-${it.id}" }) { section ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenReference(section.id) }
                                .semantics {
                                    contentDescription =
                                        "${section.title}, ${section.items.size} items"
                                },
                            shape = CardShape,
                            colors = CardDefaults.cardColors(containerColor = CardColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Icon(
                                    referenceSectionIcon(section.id),
                                    contentDescription = section.title,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${section.items.size} items",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.6f
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── ChapterView (sections list) ─────────────────────────────────────────────

@Composable
private fun ChapterView(viewModel: BookViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val chapter = uiState.currentChapter ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        // Header: back + chapter title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Navigate back" }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Neon divider
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )

        // Sections list
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(chapter.sections, key = { it.id }) { section ->
                val progress = uiState.sectionProgress[section.id]
                val isCompleted = progress?.isCompleted == true
                val hasProgress = progress != null && !isCompleted && progress.readPercent > 0f
                val chapterId = uiState.currentChapterId

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            chapterId?.let { viewModel.openSection(it, section.id) }
                        }
                        .semantics {
                            contentDescription = if (isCompleted) {
                                "${section.title}, completed"
                            } else if (hasProgress) {
                                "${section.title}, in progress"
                            } else {
                                section.title
                            }
                        },
                    shape = CardShape,
                    colors = CardDefaults.cardColors(containerColor = CardColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Status icon
                            if (isCompleted) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.4f
                                    ),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Progress indicator for in-progress sections
                        if (hasProgress && progress != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { progress.readPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                strokeCap = StrokeCap.Round,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "chapter-bottom-spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ── SectionView (reading content) ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionView(
    viewModel: BookViewModel,
    onAskSensei: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val section = uiState.currentSection ?: return
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Notes bottom sheet state
    var showNotesSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var noteText by remember(uiState.currentSectionId) { mutableStateOf("") }
    var currentNoteId by remember(uiState.currentSectionId) { mutableStateOf<Long?>(null) }

    // Load existing note for this section
    LaunchedEffect(uiState.currentSectionId) {
        val sectionId = uiState.currentSectionId ?: return@LaunchedEffect
        val notes = viewModel.getNotesForSection(sectionId)
        if (notes.isNotEmpty()) {
            noteText = notes.first().content
            currentNoteId = notes.first().id
        } else {
            noteText = ""
            currentNoteId = null
        }
    }

    // Scroll progress percent (for the sub-bar indicator)
    val scrollPercent = remember(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            scrollState.value.toFloat() / scrollState.maxValue.toFloat()
        } else 0f
    }

    // Track scroll progress, mark complete at 95%
    LaunchedEffect(scrollState) {
        var lastSaveTime = 0L
        snapshotFlow { scrollState.value to scrollState.maxValue }
            .collect { (current, max) ->
                if (max > 0) {
                    val percent = current.toFloat() / max.toFloat()
                    val now = System.currentTimeMillis()
                    if (now - lastSaveTime > 5000) {
                        viewModel.updateReadProgress(percent)
                        lastSaveTime = now
                    }
                    if (percent >= 0.95f) {
                        viewModel.markSectionComplete()
                    }
                }
            }
    }

    // Notes bottom sheet
    if (showNotesSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                val sectionId = uiState.currentSectionId
                if (sectionId != null && noteText.isNotBlank()) {
                    viewModel.saveNote(sectionId, noteText, currentNoteId)
                } else if (sectionId != null && noteText.isBlank() && currentNoteId != null) {
                    currentNoteId?.let { viewModel.deleteNote(it) }
                    currentNoteId = null
                }
                showNotesSheet = false
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                TextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = {
                        Text(
                            "Write your notes here...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.3f
                        ),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.2f
                        ),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (currentNoteId != null) {
                        TextButton(
                            onClick = {
                                currentNoteId?.let { viewModel.deleteNote(it) }
                                noteText = ""
                                currentNoteId = null
                                scope.launch {
                                    sheetState.hide()
                                    showNotesSheet = false
                                }
                            }
                        ) {
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Button(
                        onClick = {
                            val sectionId = uiState.currentSectionId
                            if (sectionId != null && noteText.isNotBlank()) {
                                viewModel.saveNote(sectionId, noteText, currentNoteId)
                            }
                            scope.launch {
                                sheetState.hide()
                                showNotesSheet = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Save", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Top bar: [back] Chapter title [bookmark-toggle] ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Navigate back" }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = uiState.currentChapter?.title ?: section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val isBookmarked = uiState.sectionProgress[section.id]?.bookmarked == true
            IconButton(
                onClick = { viewModel.toggleBookmark() },
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription =
                            if (isBookmarked) "Remove bookmark" else "Add bookmark"
                    }
            ) {
                Icon(
                    if (isBookmarked) Icons.Default.Bookmark
                    else Icons.Default.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Bookmarked" else "Not bookmarked",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Sub-bar: 3dp reading progress indicator (primary gradient) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = scrollPercent.coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
        }

        // ── Section title ──
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // ── Scrollable content ──
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            BookContentRenderer(content = section.content)

            // Code examples below content
            if (section.codeExamples.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                section.codeExamples.forEach { example ->
                    Text(
                        text = example.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 26.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CodeBlock(
                        code = example.code,
                        language = "rust"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Footer: [<< Previous] Notes Sensei [Next >>] ──
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous
            TextButton(
                onClick = { viewModel.navigateToPreviousSection() },
                modifier = Modifier
                    .height(48.dp)
                    .semantics { contentDescription = "Previous section" }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.NavigateBefore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("Prev", style = MaterialTheme.typography.labelMedium)
            }

            // Notes
            TextButton(
                onClick = { showNotesSheet = true },
                modifier = Modifier
                    .height(48.dp)
                    .semantics { contentDescription = "Open notes for this section" }
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Notes", style = MaterialTheme.typography.labelMedium)
            }

            // Sensei
            TextButton(
                onClick = { onAskSensei(section.content, "") },
                modifier = Modifier
                    .height(48.dp)
                    .semantics { contentDescription = "Ask Sensei about this section" }
            ) {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sensei", style = MaterialTheme.typography.labelMedium)
            }

            // Next
            TextButton(
                onClick = { viewModel.navigateToNextSection() },
                modifier = Modifier
                    .height(48.dp)
                    .semantics { contentDescription = "Next section" }
            ) {
                Text("Next", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
